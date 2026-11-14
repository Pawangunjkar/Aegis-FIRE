package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.models.AppTab
import com.example.providers.FireViewModel
import com.example.services.AdMobService
import com.example.services.PdfExportService
import com.example.ui.components.AdMobBanner
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.screens.CalculatorsTab
import com.example.ui.screens.FirePlannerTab
import com.example.ui.screens.PortfolioTab
import com.example.ui.screens.RoadmapTab
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: FireViewModel = viewModel()) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isIndian by viewModel.isIndianCurrency.collectAsState()
    val parameters by viewModel.parameters.collectAsState()
    val result by viewModel.result.collectAsState()
    val mcResult by viewModel.monteCarloResult.collectAsState()
    val portfolioSummary by viewModel.portfolioSummary.collectAsState()
    val proState by viewModel.proState.collectAsState()
    val recentUnlock by viewModel.recentAchievementUnlock.collectAsState()

    var isAdLoading by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!proState.isProUnlocked) {
            AdMobService.preloadAds(context)
        }
        viewModel.initPortfolioAndProfiles(context)
    }

    LaunchedEffect(proState.statusMessage) {
        val msg = proState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBillingStatusMessage()
        }
    }

    LaunchedEffect(recentUnlock) {
        val unlocked = recentUnlock
        if (unlocked != null) {
            snackbarHostState.showSnackbar("Achievement Unlocked: ${unlocked.title} (+${unlocked.points} pts)")
            viewModel.clearRecentAchievementUnlock()
        }
    }

    // Back gesture returns to primary FIRE tab before closing the app
    BackHandler(enabled = selectedTab != AppTab.FIRE) {
        viewModel.selectTab(AppTab.FIRE)
    }

    val runMonteCarloWithAd: () -> Unit = {
        if (proState.isProUnlocked) {
            viewModel.runMonteCarlo()
            viewModel.selectTab(AppTab.ROADMAP)
        } else {
            AdMobService.showRewardedWithFallback(
                context = context,
                onLoadingChanged = { isAdLoading = it },
                onReward = {
                    viewModel.runMonteCarlo()
                    viewModel.selectTab(AppTab.ROADMAP)
                }
            )
        }
    }

    val printPdfWithAd: () -> Unit = {
        val doPrint = {
            val res = result
            if (res != null) {
                val ok = PdfExportService.generateAndPrintPdf(
                    context = context,
                    params = parameters,
                    result = res,
                    mcResult = mcResult,
                    portfolioSummary = portfolioSummary,
                    isIndianCurrency = isIndian
                )
                if (!ok) {
                    scope.launch { snackbarHostState.showSnackbar("Unable to create PDF document") }
                }
            }
        }
        if (proState.isProUnlocked) {
            doPrint()
        } else {
            AdMobService.showRewardedWithFallback(
                context = context,
                onLoadingChanged = { isAdLoading = it },
                onReward = doPrint
            )
        }
    }

    val sharePdfWithAd: () -> Unit = {
        val doShare = {
            val res = result
            if (res != null) {
                val ok = PdfExportService.generateAndSharePdf(
                    context = context,
                    params = parameters,
                    result = res,
                    mcResult = mcResult,
                    portfolioSummary = portfolioSummary,
                    isIndianCurrency = isIndian
                )
                if (!ok) {
                    scope.launch { snackbarHostState.showSnackbar("Unable to share PDF document") }
                }
            }
        }
        if (proState.isProUnlocked) {
            doShare()
        } else {
            AdMobService.showRewardedWithFallback(
                context = context,
                onLoadingChanged = { isAdLoading = it },
                onReward = doShare
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, EmeraldGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_aegis_emblem_1794662932086),
                                    contentDescription = "Aegis FIRE Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_tagline),
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    color = TextPrimary,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showProDialog = true },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(48.dp)
                                .background(
                                    if (proState.isProUnlocked) AmberGold.copy(alpha = 0.16f) else SurfaceCard,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (proState.isProUnlocked) AmberGold else AmberGold.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("aegis_pro_button")
                        ) {
                            if (proState.isProUnlocked) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Aegis Pro Active",
                                    tint = AmberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "PRO",
                                    color = AmberGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleCurrency()
                            },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(48.dp)
                                .background(SurfaceCard, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .testTag("currency_toggle_button")
                        ) {
                            Text(
                                text = if (isIndian) "INR" else "USD",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.selectTab(AppTab.ROADMAP)
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(48.dp)
                                .background(SurfaceCard, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .testTag("open_roadmap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bookmarks,
                                contentDescription = "Saved Scenarios and Roadmap",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!proState.isProUnlocked) {
                        AdMobBanner(
                            modifier = if (isWideScreen) Modifier.navigationBarsPadding() else Modifier
                        )
                    }
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            contentColor = TextPrimary
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepObsidian,
                                        selectedTextColor = EmeraldGreen,
                                        indicatorColor = EmeraldGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = SurfaceCard,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, fontSize = 11.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepObsidian,
                                    selectedTextColor = EmeraldGreen,
                                    indicatorColor = EmeraldGreen,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        val showMessage: (String) -> Unit = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                        when (selectedTab) {
                            AppTab.FIRE -> FirePlannerTab(
                                viewModel = viewModel,
                                isAdLoading = isAdLoading,
                                onRunMonteCarlo = runMonteCarloWithAd,
                                onExportPdf = printPdfWithAd
                            )
                            AppTab.CALCULATORS -> CalculatorsTab(
                                viewModel = viewModel,
                                onExportFeedback = showMessage,
                                onOpenProUpgrade = { showProDialog = true }
                            )
                            AppTab.PORTFOLIO -> PortfolioTab(
                                viewModel = viewModel,
                                onSyncedToFire = {
                                    showMessage("Blended portfolio synced to FIRE Planner")
                                },
                                onExportFeedback = showMessage,
                                onOpenProUpgrade = { showProDialog = true }
                            )
                            AppTab.ROADMAP -> RoadmapTab(
                                viewModel = viewModel,
                                isAdLoading = isAdLoading,
                                onRunMonteCarlo = runMonteCarloWithAd,
                                onPrintPdf = printPdfWithAd,
                                onSharePdf = sharePdfWithAd,
                                onLoadProfile = { profileName ->
                                    val loadAction = {
                                        val loaded = viewModel.loadProfile(context, profileName)
                                        if (loaded) {
                                            viewModel.selectTab(AppTab.FIRE)
                                            showMessage("Loaded scenario: $profileName")
                                        }
                                    }
                                    if (proState.isProUnlocked) {
                                        loadAction()
                                    } else {
                                        AdMobService.showInterstitial(context) {
                                            loadAction()
                                        }
                                    }
                                },
                                onExportFeedback = showMessage
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProDialog) {
        ProUpgradeDialog(
            proState = proState,
            onPurchaseClick = {
                val activity = context.findHostActivity()
                if (activity != null) {
                    viewModel.launchProPurchase(activity)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Unable to start Play Store checkout") }
                }
            },
            onRestoreClick = {
                viewModel.restoreProPurchases()
            },
            onDismiss = { showProDialog = false }
        )
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector = when (tab) {
    AppTab.FIRE -> if (selected) Icons.Rounded.LocalFireDepartment else Icons.Outlined.LocalFireDepartment
    AppTab.CALCULATORS -> if (selected) Icons.Rounded.Calculate else Icons.Outlined.Calculate
    AppTab.PORTFOLIO -> if (selected) Icons.Rounded.PieChart else Icons.Outlined.PieChart
    AppTab.ROADMAP -> if (selected) Icons.Rounded.Bookmarks else Icons.Outlined.Bookmarks
}
