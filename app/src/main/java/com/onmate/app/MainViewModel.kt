package com.onmate.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onmate.app.data.JsonFileTripRepository
import com.onmate.app.data.SeedData
import com.onmate.app.data.TripRepository
import com.onmate.app.domain.AlertCandidate
import com.onmate.app.domain.AlertDecider
import com.onmate.app.domain.DayType
import com.onmate.app.domain.Feedback
import com.onmate.app.domain.HabitLearner
import com.onmate.app.domain.HabitStat
import com.onmate.app.domain.ItemSuggester
import com.onmate.app.domain.SituationKey
import com.onmate.app.domain.TimeSlot
import com.onmate.app.domain.Trip
import com.onmate.app.domain.TripDetector
import com.onmate.app.domain.TripPhase
import com.onmate.app.domain.TripSignal
import com.onmate.app.domain.VehicleEvent
import com.onmate.app.domain.Zone
import com.onmate.app.domain.ZoneRecord
import com.onmate.app.domain.ZoneTag
import com.onmate.app.ui.components.NavTab
import com.onmate.app.vehicle.FakeDestinationSource
import com.onmate.app.vehicle.FakeVehicleSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/** '출발 전 물건 입력' 화면 내용 */
data class ItemPrompt(val zone: Zone, val suggestion: String?, val others: List<String>)

data class UiState(
    val tab: NavTab = NavTab.HOME,
    val phase: TripPhase = TripPhase.PARKED,
    val openedBefore: Set<Zone> = emptySet(),
    val openedAfter: Set<Zone> = emptySet(),
    val openedAtMs: Map<Zone, Long> = emptyMap(),
    val tripItems: Map<Zone, String> = emptyMap(),
    val itemPrompt: ItemPrompt? = null,
    val candidates: List<AlertCandidate> = emptyList(),
    val alertVisible: Boolean = false,
    val departedAtMs: Long? = null,
    val origin: String? = null,
    val destination: String? = null,
    val remainingMin: Int? = null,
    val trips: List<Trip> = emptyList(),
    val tags: Map<Zone, ZoneTag> = emptyMap(),
    val habitSituations: List<SituationKey> = emptyList(),
    val habitSituation: SituationKey? = null,
    val habitStats: Map<Zone, HabitStat> = emptyMap(),
    val tagZone: Zone = Zone.REAR_LEFT,
    val learnedDays: Int = 0,
    val habitsFound: Int = 0,
    val tookItThisMonth: Int = 0,
    val showDemo: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // 실제 차량 연동 시 PleosVehicleSource(app)로 교체
    val vehicle = FakeVehicleSource()
    private val destinations = FakeDestinationSource()
    private val repo: TripRepository =
        JsonFileTripRepository.open(File(app.filesDir, "onmate.json")) { SeedData.build() }

    private val detector = TripDetector()
    private val learner = HabitLearner()
    private val decider = AlertDecider()
    private val suggester = ItemSuggester()

    private val pendingFeedback = mutableMapOf<Zone, Feedback>()
    private val nothingZones = mutableSetOf<Zone>()
    private var tripSituation: SituationKey? = null
    private var autoFinishJob: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        refreshHistory()
        viewModelScope.launch {
            vehicle.events.collect { e -> onVehicleEvent(e) }
        }
    }

    // ---------- 차량 이벤트 처리 ----------

    private fun onVehicleEvent(e: VehicleEvent) {
        val wasParked = detector.phase == TripPhase.PARKED
        val signals = detector.process(e)
        if (e is VehicleEvent.DoorChanged && e.open && wasParked) {
            _state.update { it.copy(openedAtMs = it.openedAtMs + (e.zone to (it.openedAtMs[e.zone] ?: e.timestampMs))) }
        }
        _state.update { it.copy(phase = detector.phase, openedBefore = detector.currentOpenedBefore, openedAfter = detector.currentOpenedAfter) }
        signals.forEach(::onSignal)
    }

    private fun onSignal(s: TripSignal) {
        when (s) {
            is TripSignal.ZoneClosedWhileParked -> showItemPrompt(s.zone)
            is TripSignal.Departed -> {
                tripSituation = currentSituation()
                _state.update {
                    it.copy(
                        itemPrompt = null, departedAtMs = s.atMs, tab = NavTab.HOME,
                        origin = destinations.currentOrigin(), destination = destinations.currentDestination(),
                        remainingMin = destinations.remainingMinutes(),
                    )
                }
            }
            is TripSignal.Arrived -> {
                val situation = tripSituation ?: currentSituation()
                val stats = learner.stats(repo.trips(), situation)
                val candidates = decider.decide(
                    openedBefore = s.openedBefore - nothingZones,
                    openedAfter = detector.currentOpenedAfter,
                    stats = stats,
                    tags = repo.tags(),
                    tripItems = _state.value.tripItems,
                )
                _state.update { it.copy(candidates = candidates, alertVisible = candidates.isNotEmpty(), tab = NavTab.HOME) }
            }
            is TripSignal.ZoneOpenedAfterArrival -> {
                _state.update { st ->
                    val left = st.candidates.filterNot { c -> c.zone == s.zone }
                    st.copy(candidates = left, alertVisible = st.alertVisible && left.isNotEmpty())
                }
                if (_state.value.candidates.isEmpty() && detector.phase == TripPhase.EXITING) scheduleAutoFinish()
            }
            TripSignal.DriverExiting -> {
                _state.update { it.copy(alertVisible = false) }
                if (_state.value.candidates.isEmpty()) scheduleAutoFinish()
            }
            is TripSignal.TripCompleted -> saveTrip(s)
        }
    }

    private fun showItemPrompt(zone: Zone) {
        val trips = repo.trips()
        val suggestion = suggester.suggest(zone, trips)
        val others = suggester.frequentItems(trips, exclude = suggestion, limit = 4)
        _state.update { it.copy(itemPrompt = ItemPrompt(zone, suggestion, others), tab = NavTab.HOME) }
    }

    private fun saveTrip(done: TripSignal.TripCompleted) {
        val st = _state.value
        val tags = repo.tags()
        val zones = (done.openedBefore + done.openedAfter).filter { it.learnable }
        val records = zones.map { z ->
            val before = z in done.openedBefore
            ZoneRecord(
                zone = z,
                openedBefore = before,
                openedAfter = z in done.openedAfter,
                item = st.tripItems[z] ?: if (before && z !in nothingZones) tags[z]?.item else null,
                feedback = pendingFeedback[z],
            )
        }
        repo.saveTrip(
            Trip(
                id = repo.nextTripId(),
                departedAtMs = done.departedAtMs,
                arrivedAtMs = done.arrivedAtMs,
                situation = tripSituation ?: currentSituation(),
                origin = st.origin,
                records = records,
            )
        )
        pendingFeedback.clear()
        nothingZones.clear()
        tripSituation = null
        _state.update {
            it.copy(
                phase = detector.phase, openedBefore = emptySet(), openedAfter = emptySet(), openedAtMs = emptyMap(),
                tripItems = emptyMap(), candidates = emptyList(), alertVisible = false, departedAtMs = null,
            )
        }
        refreshHistory()
    }

    private fun scheduleAutoFinish() {
        autoFinishJob?.cancel()
        autoFinishJob = viewModelScope.launch {
            delay(8_000)
            if (detector.phase == TripPhase.EXITING && _state.value.candidates.isEmpty()) finishTrip()
        }
    }

    // ---------- 사용자 동작 ----------

    fun selectTab(tab: NavTab) = _state.update { it.copy(tab = tab) }

    /** 출발 전 물건 입력: item=null 이면 '아무것도 안 뒀어요' */
    fun answerItemPrompt(item: String?) {
        val prompt = _state.value.itemPrompt ?: return
        if (item == null) {
            nothingZones += prompt.zone
            _state.update { it.copy(itemPrompt = null, tripItems = it.tripItems - prompt.zone) }
        } else {
            nothingZones -= prompt.zone
            _state.update { it.copy(itemPrompt = null, tripItems = it.tripItems + (prompt.zone to item)) }
        }
    }

    fun skipItemPrompt() = _state.update { it.copy(itemPrompt = null) }

    /** 하차 전 알림 답변 */
    fun answerAlert(feedback: Feedback) {
        _state.value.candidates.forEach { pendingFeedback[it.zone] = feedback }
        _state.update { it.copy(alertVisible = false, candidates = emptyList()) }
        if (detector.phase == TripPhase.EXITING) scheduleAutoFinish()
    }

    /** 하차 중 화면 답변: 답하고 트립을 마감 */
    fun answerExit(feedback: Feedback) {
        _state.value.candidates.forEach { pendingFeedback[it.zone] = feedback }
        finishTrip()
    }

    fun finishTrip() {
        autoFinishJob?.cancel()
        detector.finishTrip()?.let { saveTrip(it) }
    }

    fun selectHabitSituation(s: SituationKey) =
        _state.update { it.copy(habitSituation = s, habitStats = learner.stats(repo.trips(), s)) }

    fun selectTagZone(z: Zone) { if (z.learnable) _state.update { it.copy(tagZone = z) } }

    fun saveTag(item: String, notify: Boolean) {
        val zone = _state.value.tagZone
        if (item.isBlank()) return
        repo.saveTag(ZoneTag(zone, item.trim(), notify))
        refreshHistory()
    }

    fun removeTag() {
        repo.removeTag(_state.value.tagZone)
        refreshHistory()
    }

    // ---------- 시연용 조작 (DemoPanel) ----------

    fun toggleDemo() = _state.update { it.copy(showDemo = !it.showDemo) }
    fun demoOpenClose(z: Zone) = vehicle.openAndClose(z)
    fun demoOpen(z: Zone) = vehicle.setDoor(z, true)
    fun demoClose(z: Zone) = vehicle.setDoor(z, false)
    fun demoDepart() = vehicle.depart()
    fun demoArrive() = vehicle.arrive()
    fun demoScenario() {
        viewModelScope.launch { vehicle.playDemoScenario() }
    }

    // ---------- 계산 ----------

    private fun refreshHistory() {
        val trips = repo.trips()
        val situations = learner.situations(trips).take(4)
        val selected = _state.value.habitSituation?.takeIf { it in situations } ?: situations.firstOrNull()
        val now = System.currentTimeMillis()
        val firstTrip = trips.minOfOrNull { it.departedAtMs }
        val learnedDays = if (firstTrip == null) 0 else ((now - firstTrip) / DAY_MS + 1).toInt()
        val habits = situations.sumOf { s ->
            learner.stats(trips, s).values.count { it.isLearned && (it.rate ?: 0f) >= 0.7f }
        }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        }.timeInMillis
        val tookIt = trips.filter { it.departedAtMs >= monthStart }
            .sumOf { t -> t.records.count { it.feedback == Feedback.TOOK_IT } }

        _state.update {
            it.copy(
                trips = trips,
                tags = repo.tags(),
                habitSituations = situations,
                habitSituation = selected,
                habitStats = if (selected != null) learner.stats(trips, selected) else emptyMap(),
                learnedDays = learnedDays,
                habitsFound = habits,
                tookItThisMonth = tookIt,
            )
        }
    }

    /**
     * 시연용 고정 상황. 언제 녹화해도 '평일 아침 · 회사' 습관(92%)으로 알림이 뜨게 한다.
     * 실제 차량 연동 시 null 로 바꾸면 현재 시각·목적지로 계산한다.
     */
    private val situationOverride: SituationKey? = SituationKey(DayType.WEEKDAY, TimeSlot.MORNING, "회사")

    private fun currentSituation(nowMs: Long = System.currentTimeMillis()): SituationKey {
        situationOverride?.let { return it }
        val c = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val day = if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) DayType.WEEKEND else DayType.WEEKDAY
        return SituationKey(day, TimeSlot.fromHour(c.get(Calendar.HOUR_OF_DAY)), destinations.currentDestination())
    }

    override fun onCleared() {
        vehicle.stop()
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
