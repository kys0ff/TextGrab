package off.kys.textgrab.ui.screens.main

import off.kys.textgrab.core.model.HistoryEntry

sealed interface MainEvent {
    data object RefreshPermissions : MainEvent
    data object ClearHistory : MainEvent
    data object ConfirmClearHistory : MainEvent
    data object DismissClearHistoryDialog : MainEvent
    data class OnHistoryCopy(val entry: HistoryEntry) : MainEvent
    data object OpenDonationDialog : MainEvent
    data object DismissDonationDialog : MainEvent
    data object RemoveDonationIcon : MainEvent
}