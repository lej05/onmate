package com.onmate.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.onmate.app.ui.screens.SplashScreen
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onmate.app.domain.TripPhase
import com.onmate.app.ui.components.NavRail
import com.onmate.app.ui.components.NavTab
import com.onmate.app.ui.screens.ArrivalAlert
import com.onmate.app.ui.screens.DemoPanel
import com.onmate.app.ui.screens.DrivingScreen
import com.onmate.app.ui.screens.HabitScreen
import com.onmate.app.ui.screens.HomeScreen
import com.onmate.app.ui.screens.LogScreen
import com.onmate.app.ui.screens.TagScreen
import com.onmate.app.ui.theme.OnColors

@Composable
fun OnMateApp(vm: MainViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1800)
        splash = false
    }

    Box(Modifier.fillMaxSize().background(OnColors.Background)) {
        if (splash) {
            SplashScreen()
            return@Box
        }
        if (st.phase == TripPhase.DRIVING) {
            DrivingScreen(st)
        } else {
            Row(Modifier.fillMaxSize()) {
                NavRail(st.tab, vm::selectTab)
                Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 40.dp, vertical = 32.dp)) {
                    when (st.tab) {
                        NavTab.HOME -> HomeScreen(st, vm)
                        NavTab.HABIT -> HabitScreen(st, vm::selectHabitSituation)
                        NavTab.LOG -> LogScreen(st)
                        NavTab.TAG -> TagScreen(st, vm)
                    }
                }
            }
        }
        if (st.alertVisible && st.phase == TripPhase.ARRIVED) {
            ArrivalAlert(st, vm::answerAlert)
        }
        DemoPanel(st, vm, Modifier.align(Alignment.BottomEnd))
    }
}
