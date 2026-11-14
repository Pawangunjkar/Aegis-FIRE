package com.example

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.models.DebtPayoffStrategy
import com.example.models.TaxRegime
import com.example.providers.FireViewModel
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.screens.CalculatorsTab
import com.example.ui.screens.FirePlannerTab
import com.example.ui.screens.PortfolioTab
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.RoadmapTab
import com.example.ui.theme.AegisFireTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalculatorAndPortfolioUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `calculators tab switches across SIP, SWP, FD and Goal modes`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MATURITY VALUE").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calc_mode_swp").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TOTAL WITHDRAWN").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calc_mode_fd_bonds").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("POST-TAX MATURITY").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calc_mode_goal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("REQUIRED MONTHLY SIP").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calc_mode_inflation").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("FUTURE GOAL COST").assertIsDisplayed()
        composeTestRule.onNodeWithText("PURCHASING POWER LEFT").assertIsDisplayed()

        composeTestRule.onNodeWithTag("inflation_rate_input").performScrollTo().performTextReplacement("10.0")
        composeTestRule.waitForIdle()
        assertEquals(0.10, vm.inflationParams.value.annualInflationRate, 0.001)

        composeTestRule.onNodeWithTag("export_projections_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_projections_csv_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `debt payoff calculator toggles Avalanche vs Snowball and adds debt account`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_mode_debt_payoff").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DEBT-FREE IN").assertIsDisplayed()
        composeTestRule.onNodeWithText("INTEREST SAVED").assertIsDisplayed()

        composeTestRule.onNodeWithTag("strategy_snowball").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(DebtPayoffStrategy.SNOWBALL, vm.debtStrategy.value)

        composeTestRule.onNodeWithTag("strategy_avalanche").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(DebtPayoffStrategy.AVALANCHE, vm.debtStrategy.value)

        val initialDebts = vm.debtAccounts.value.size
        val initialDebtBalance = vm.debtPayoffResult.value.totalDebtBalance
        composeTestRule.onNodeWithTag("add_debt_account_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("debt_name_input").performTextReplacement("Store Card")
        composeTestRule.onNodeWithTag("debt_balance_input").performTextReplacement("3000")
        composeTestRule.onNodeWithTag("debt_apr_input").performTextReplacement("24.0")
        composeTestRule.onNodeWithTag("debt_min_payment_input").performTextReplacement("90")
        composeTestRule.onNodeWithTag("save_debt_account_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialDebts + 1, vm.debtAccounts.value.size)
        assertEquals(initialDebtBalance + 3_000.0, vm.debtPayoffResult.value.totalDebtBalance, 0.01)
    }

    @Test
    fun `tax estimator switches regimes, renders bracket breakdown, and syncs effective rate to FIRE`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_mode_tax").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("EST. ANNUAL TAX").assertIsDisplayed()
        composeTestRule.onNodeWithText("EFFECTIVE TAX RATE").assertIsDisplayed()
        composeTestRule.onNodeWithText("DEDUCTION TAX SAVED").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tax_regime_us_joint").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(TaxRegime.US_JOINT, vm.taxParams.value.regime)

        composeTestRule.onNodeWithTag("tax_regime_in_old").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(TaxRegime.IN_OLD, vm.taxParams.value.regime)

        composeTestRule.onNodeWithText("PROGRESSIVE BRACKET BREAKDOWN").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("sync_tax_rate_to_fire_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(vm.taxResult.value.effectiveTaxRate, vm.parameters.value.taxRate, 0.001)
    }

    @Test
    fun `retirement planner calculates required corpus from ages, expenses, and inflation and syncs to FIRE`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_mode_retirement").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("REQUIRED CORPUS").assertIsDisplayed()
        composeTestRule.onNodeWithText("MONTHLY EXPENSE THEN").assertIsDisplayed()
        composeTestRule.onNodeWithText("REQUIRED MONTHLY SIP").assertIsDisplayed()

        val initialCorpus = vm.retirementPlanResult.value.requiredCorpusAtRetirement

        composeTestRule.onNodeWithTag("retirement_age_input").performScrollTo().performTextReplacement("58")
        composeTestRule.onNodeWithTag("retirement_life_expectancy_input").performScrollTo().performTextReplacement("90")
        composeTestRule.onNodeWithTag("retirement_monthly_expense_input").performScrollTo().performTextReplacement("5000")
        composeTestRule.onNodeWithTag("retirement_inflation_input").performScrollTo().performTextReplacement("6.0")
        composeTestRule.waitForIdle()

        assertEquals(58, vm.retirementPlanParams.value.retirementAge)
        assertEquals(90, vm.retirementPlanParams.value.lifeExpectancy)
        assertEquals(5_000.0, vm.retirementPlanParams.value.targetMonthlyExpensesToday, 0.01)
        assertEquals(0.06, vm.retirementPlanParams.value.annualInflationRate, 0.001)
        assertEquals(32, vm.retirementPlanResult.value.yearsInRetirement)
        assertEquals(true, vm.retirementPlanResult.value.requiredCorpusAtRetirement > initialCorpus)

        composeTestRule.onNodeWithTag("sync_retirement_to_fire_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(58, vm.parameters.value.targetAge)
        assertEquals(5_000.0, vm.parameters.value.monthlyExpense, 0.01)
        assertEquals(0.06, vm.parameters.value.inflationRate, 0.001)
    }

    @Test
    fun `cashflow scheduler adds recurring rule, processes due entries, and syncs to portfolio and debt`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_mode_cashflow").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MONTHLY INCOME").assertIsDisplayed()
        composeTestRule.onNodeWithText("MONTHLY EXPENSES").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET MONTHLY SURPLUS").assertIsDisplayed()

        val initialCount = vm.recurringTransactions.value.size
        val initialIncome = vm.cashflowSummary.value.totalMonthlyIncome

        composeTestRule.onNodeWithTag("add_recurring_rule_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("recurring_title_input").performTextReplacement("Side Consulting")
        composeTestRule.onNodeWithTag("recurring_amount_input").performTextReplacement("1200")
        composeTestRule.onNodeWithTag("recurring_day_input").performTextReplacement("1")
        composeTestRule.onNodeWithTag("save_recurring_rule_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialCount + 1, vm.recurringTransactions.value.size)
        assertEquals(initialIncome + 1_200.0, vm.cashflowSummary.value.totalMonthlyIncome, 0.01)

        composeTestRule.onNodeWithTag("process_due_transactions_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, vm.cashflowLogs.value.size)

        composeTestRule.onNodeWithTag("sync_cashflow_debt_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(vm.cashflowSummary.value.monthlyDebtAutoRepayment, vm.extraDebtPayment.value, 0.01)
    }

    @Test
    fun `budget planner displays recurring integration, adds categorized spending and saving target, and syncs to FIRE`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                CalculatorsTab(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_mode_budget").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MONTHLY BUDGETED").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTUAL + RECURRING").assertIsDisplayed()
        composeTestRule.onNodeWithText("CATEGORY SAVINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("UNALLOCATED SURPLUS").assertIsDisplayed()

        val initialCount = vm.budgetCategories.value.size
        val initialSavingTargets = vm.budgetSummary.value.totalCategorySavingTargets

        composeTestRule.onNodeWithTag("add_budget_category_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("budget_category_name_input").performTextReplacement("Transport")
        composeTestRule.onNodeWithTag("budget_limit_input").performTextReplacement("500")
        composeTestRule.onNodeWithTag("budget_spent_input").performTextReplacement("200")
        composeTestRule.onNodeWithTag("budget_saving_target_input").performTextReplacement("150")
        composeTestRule.onNodeWithTag("save_budget_category_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialCount + 1, vm.budgetCategories.value.size)
        assertEquals(initialSavingTargets + 150.0, vm.budgetSummary.value.totalCategorySavingTargets, 0.01)
        val transport = vm.budgetSummary.value.categories.first { it.entry.name == "Transport" }
        assertEquals(300.0, transport.projectedCategorySavings, 0.01)

        composeTestRule.onNodeWithTag("sync_budget_to_fire_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        val expectedLivingSpend = vm.budgetSummary.value.categories
            .filterNot { it.entry.name.equals("Investment SIP", ignoreCase = true) }
            .sumOf { it.effectiveSpent }
        assertEquals(expectedLivingSpend, vm.parameters.value.monthlyExpense, 0.01)
    }

    @Test
    fun `pro upgrade dialog displays 3_99 lifetime pricing and unlocks pro portfolio analytics`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                val proState = vm.proState.collectAsState().value
                PortfolioTab(viewModel = vm, onSyncedToFire = {})
                if (!proState.isProUnlocked) {
                    ProUpgradeDialog(
                        proState = proState,
                        onPurchaseClick = { vm.setProUnlockedForTest(true) },
                        onRestoreClick = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Upgrade to Aegis Pro").assertIsDisplayed()
        composeTestRule.onNodeWithTag("restore_pro_purchase_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("purchase_pro_confirm_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pro_portfolio_analytics_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("PRO PORTFOLIO ANALYTICS").assertIsDisplayed()
    }

    @Test
    fun `portfolio tab renders net worth, allows adding asset and liability, and syncs to FIRE`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                PortfolioTab(viewModel = vm, onSyncedToFire = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TOTAL NET WORTH").assertIsDisplayed()
        composeTestRule.onNodeWithTag("net_worth_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("ASSET CLASS ALLOCATION").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_portfolio_to_fire_button").performScrollTo().assertIsDisplayed()

        val initialCount = vm.holdings.value.size
        val initialNetWorth = vm.portfolioSummary.value.netWorth

        // Open Add Asset dialog and add 10 shares @ $200 market price ($2,000)
        composeTestRule.onNodeWithTag("add_holding_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("holding_name_input").performTextReplacement("Nvidia Corp")
        composeTestRule.onNodeWithTag("holding_symbol_input").performTextReplacement("NVDA")
        composeTestRule.onNodeWithTag("holding_quantity_input").performTextReplacement("10")
        composeTestRule.onNodeWithTag("holding_buy_price_input").performTextReplacement("150")
        composeTestRule.onNodeWithTag("holding_market_price_input").performTextReplacement("200")
        composeTestRule.onNodeWithTag("save_holding_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialCount + 1, vm.holdings.value.size)
        assertEquals(initialNetWorth + 2_000.0, vm.portfolioSummary.value.netWorth, 0.01)

        // Open Add Debt dialog and add $5,000 liability
        composeTestRule.onNodeWithTag("add_liability_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("liability_balance_input").performTextReplacement("5000")
        composeTestRule.onNodeWithTag("save_liability_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(5_000.0, vm.portfolioSummary.value.totalLiabilities, 0.01)
        assertEquals(initialNetWorth + 2_000.0 - 5_000.0, vm.portfolioSummary.value.netWorth, 0.01)

        composeTestRule.onNodeWithTag("export_portfolio_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_portfolio_csv_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `roadmap tab displays financial independence score, filters achievements, and unlocks milestones on debt reduction`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                RoadmapTab(
                    viewModel = vm,
                    isAdLoading = false,
                    onRunMonteCarlo = {},
                    onPrintPdf = {},
                    onSharePdf = {},
                    onLoadProfile = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fi_score_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fi_score_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("FINANCIAL INDEPENDENCE SCORE").assertIsDisplayed()

        // Filter by Debt Crush achievements
        composeTestRule.onNodeWithTag("fi_filter_debt").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("High-APR Slayer").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("100% Debt-Free").performScrollTo().assertIsDisplayed()

        val initialUnlocked = vm.fiScoreSummary.value.unlockedCount
        // Pay off all debts and verify unlocked count increases and UI updates
        vm.debtAccounts.value.toList().forEach { vm.removeDebtAccount(context, it.id) }
        composeTestRule.waitForIdle()

        val updatedUnlocked = vm.fiScoreSummary.value.unlockedCount
        assertEquals(true, updatedUnlocked > initialUnlocked)
        composeTestRule.onNodeWithText("$updatedUnlocked / ${vm.fiScoreSummary.value.totalAchievements} Unlocked").assertIsDisplayed()

        // Verify Author & Developer Card on RoadmapTab
        composeTestRule.onNodeWithTag("about_author_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Pawan Gunjkar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("pawangunjkar@gmail.com").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("contact_author_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_privacy_policy_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, vm.showPrivacyPolicy.value)
        vm.closePrivacyPolicy()
    }

    @Test
    fun `fire planner dashboard displays daily financial quote card, cycles tips offline, and opens author dialog`() {
        val vm = FireViewModel()
        composeTestRule.setContent {
            AegisFireTheme {
                FirePlannerTab(
                    viewModel = vm,
                    isAdLoading = false,
                    onRunMonteCarlo = {},
                    onExportPdf = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("daily_tip_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("DAILY FINANCIAL WISDOM").assertIsDisplayed()

        val firstTipId = vm.dailyTip.value.id
        composeTestRule.onNodeWithTag("daily_tip_next_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, vm.dailyTip.value.id != firstTipId)

        composeTestRule.onNodeWithTag("daily_tip_bookmark_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, vm.bookmarkedTipIds.value.contains(vm.dailyTip.value.id))
        composeTestRule.onNodeWithText("Saved to favorites").assertIsDisplayed()

        composeTestRule.onNodeWithTag("author_footer_badge").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Aegis FIRE • Developer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pawan Gunjkar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("pawangunjkar@gmail.com").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_privacy_policy_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, vm.showPrivacyPolicy.value)
    }

    @Test
    fun `privacy policy screen states offline first, zero data collection, author contact info, and navigates back`() {
        var backCalled = false
        composeTestRule.setContent {
            AegisFireTheme {
                PrivacyPolicyScreen(onBack = { backCalled = true })
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("privacy_policy_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline-First & Zero Data Collection").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. Offline-First Financial Engine").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2. Zero Personal or Financial Data Collection").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Author: Pawan Gunjkar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: pawangunjkar@gmail.com").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("privacy_contact_email_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("privacy_policy_back_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, backCalled)
    }
}
