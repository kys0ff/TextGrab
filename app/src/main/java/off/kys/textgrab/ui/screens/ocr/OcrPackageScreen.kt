package off.kys.textgrab.ui.screens.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
import off.kys.textgrab.ui.screens.ocr.components.DeleteConfirmationDialog
import off.kys.textgrab.ui.screens.ocr.components.OcrIntroBanner
import off.kys.textgrab.ui.screens.ocr.components.OcrPackageCard
import off.kys.textgrab.ui.theme.TextGrabTheme
import org.koin.androidx.compose.koinViewModel

class OcrPackageScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<OcrPackageViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        OcrPackageContent(
            state = state,
            onEvent = viewModel::onEvent,
            onPop = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrPackageContent(
    state: OcrPackageState,
    onEvent: (OcrPackageEvent) -> Unit,
    onPop: () -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ocr_package_label_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(OcrPackageEvent.Refresh) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.ocr_package_action_button_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OcrIntroBanner()
                }

                items(
                    items = state.packages,
                    key = { it.displayName }
                ) { pkg ->
                    OcrPackageCard(
                        pkg = pkg,
                        onEvent = onEvent
                    )
                }
            }

            if (state.deleteConfirmation != null) {
                DeleteConfirmationDialog(
                    conf = state.deleteConfirmation,
                    onConfirm = { pkg, version ->
                        onEvent(OcrPackageEvent.ConfirmDelete(pkg, version))
                    },
                    onDismiss = { onEvent(OcrPackageEvent.DismissDeleteDialog) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OcrPackageScreenPreview() {
    TextGrabTheme {
        OcrPackageContent(
            state = OcrPackageState(
                packages = listOf(
                    OcrPackage(
                        language = OcrLanguage.LATIN,
                        displayName = "English",
                        tessCode = "eng",
                        versions = listOf(
                            OcrVersion(
                                TesseractVersion.FAST,
                                "url",
                                10_000_000L,
                                true,
                                DownloadState.Downloaded,
                                true
                            ),
                            OcrVersion(
                                TesseractVersion.STANDARD,
                                "url",
                                20_000_000L,
                                false,
                                DownloadState.Downloading(0.45f),
                                false
                            )
                        )
                    )
                )
            ),
            onEvent = {},
            onPop = {}
        )
    }
}