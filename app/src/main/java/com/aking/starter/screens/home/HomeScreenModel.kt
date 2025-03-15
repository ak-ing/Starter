package com.aking.starter.screens.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel

/**
 * @author Ak
 * 2025/3/13  17:16
 */

@Stable
interface HomeUiState {
    val floatingPermission: Permission
}

private class MutableDiceUiState : HomeUiState {
    override var floatingPermission by mutableStateOf(FloatingPermission)
}

sealed interface HomeIntent {
    /* 处理权限请求结果 */
    data class HandlePermission(val granted: Boolean) : HomeIntent
}

class HomeScreenModel : ScreenModel {

    private val _uiState = MutableDiceUiState()
    val uiState: HomeUiState = _uiState

    fun reducer(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.HandlePermission -> handlePermissionResult(intent)
        }
    }

    private fun handlePermissionResult(intent: HomeIntent.HandlePermission) {
        if (intent.granted != uiState.floatingPermission.isGranted) {
            _uiState.floatingPermission = uiState.floatingPermission.copy(isGranted = intent.granted)
        }
    }
}