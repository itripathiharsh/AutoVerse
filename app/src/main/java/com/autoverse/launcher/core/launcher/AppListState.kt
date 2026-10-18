package com.autoverse.launcher.core.launcher

import com.autoverse.launcher.core.model.InstalledApp

/** UI state for the app list; keeps loading/error/empty handling explicit. */
sealed interface AppListState {
    data object Loading : AppListState
    data class Ready(val apps: List<InstalledApp>) : AppListState
    data class Error(val message: String) : AppListState
}
