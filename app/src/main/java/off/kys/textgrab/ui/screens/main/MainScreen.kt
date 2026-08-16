package off.kys.textgrab.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.ui.screens.main.components.ClearHistoryDialog
import off.kys.textgrab.ui.screens.main.components.DonationDialog
import off.kys.textgrab.ui.screens.main.components.HistorySection
import off.kys.textgrab.ui.screens.main.components.PermissionGrid
import off.kys.textgrab.ui.screens.main.components.ReadinessHero
import off.kys.textgrab.ui.screens.main.components.SectionHeader
import off.kys.textgrab.ui.screens.main.components.TileSetupCompact
import off.kys.textgrab.ui.screens.ocr.OcrPackageScreen
import off.kys.textgrab.ui.theme.TextGrabTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<MainViewModel>()
        val state by viewModel.state.collectAsState()

        val permissionManager = koinInject<PermissionManager>()
        val navigator = LocalNavigator.currentOrThrow

        MainScreenContent(
            state = state,
            onEvent = viewModel::onEvent,
            onOpenAccessibility = { permissionManager.openAccessibilitySettings() },
            onOpenOverlay = { permissionManager.openOverlaySettings() },
            onOpenNotifications = { permissionManager.openNotificationsSettings() },
            onNavigateToOcr = { navigator.push(OcrPackageScreen()) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigateToOcr: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
    }

    LifecycleResumeEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
        onPauseOrDispose {}
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ready = state.permissions.accessibility && state.permissions.overlay
    val grantedPermissions = listOf(
        state.permissions.accessibility,
        state.permissions.overlay,
        state.permissions.notifications
    ).count { it }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.common_app_label_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    AnimatedVisibility(
                        visible = state.showDonationIcon,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        IconButton(onClick = { onEvent(MainEvent.OpenDonationDialog) }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.ic_liberapay),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = ready,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        OverlayBus.send(OverlayCommand.Trigger(ExtractionMode.ACCESSIBILITY))
                    },
                    shape = CircleShape,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_document_scanner),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.main_action_button_scan),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ReadinessHero(
                    granted = grantedPermissions,
                    ready = ready
                )
            }

            item {
                SectionHeader(title = stringResource(R.string.main_label_permissions_section))
            }

            item {
                PermissionGrid(
                    permissions = state.permissions,
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenOverlay = onOpenOverlay,
                    onOpenNotifications = onOpenNotifications,
                    onOcr = onNavigateToOcr
                )
            }

            item {
                TileSetupCompact()
            }

            item {
                HistorySection(
                    history = state.history,
                    onClear = { onEvent(MainEvent.ClearHistory) },
                    onCopy = { onEvent(MainEvent.OnHistoryCopy(it)) }
                )
            }
        }

        if (state.showClearHistoryConfirmation) {
            ClearHistoryDialog(
                onConfirm = { onEvent(MainEvent.ConfirmClearHistory) },
                onDismiss = { onEvent(MainEvent.DismissClearHistoryDialog) }
            )
        }

        if (state.showDonationDialog) {
            DonationDialog(
                onDismiss = { onEvent(MainEvent.DismissDonationDialog) },
                onRemoveIcon = { onEvent(MainEvent.RemoveDonationIcon) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    TextGrabTheme {
        MainScreenContent(
            state = MainState(
                history = listOf(
                    HistoryEntry(
                        1L,
                        "Sample extracted text 1",
                        System.currentTimeMillis(),
                        ExtractionMode.ACCESSIBILITY
                    ),
                    HistoryEntry(
                        2L,
                        "Sample extracted text 2",
                        System.currentTimeMillis() - 1000000,
                        ExtractionMode.OCR
                    )
                ),
                permissions = PermissionUiState(
                    accessibility = true,
                    overlay = false,
                    notifications = true
                )
            ),
            onEvent = {},
            onOpenAccessibility = {},
            onOpenOverlay = {},
            onOpenNotifications = {},
            onNavigateToOcr = {}
        )
    }
}
