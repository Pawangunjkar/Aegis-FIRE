package com.example.providers

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.AppTab
import com.example.models.AssetHolding
import com.example.models.AssetType
import com.example.models.BudgetCategoryEntry
import com.example.models.BudgetPlanSummary
import com.example.models.CalculatorMode
import com.example.models.CashflowExecutionLog
import com.example.models.DailyFinancialTip
import com.example.models.DebtAccount
import com.example.models.DebtPayoffResult
import com.example.models.DebtPayoffStrategy
import com.example.models.FdParams
import com.example.models.FdResult
import com.example.models.FiAchievement
import com.example.models.FiScoreSummary
import com.example.models.FireParameters
import com.example.models.FireResult
import com.example.models.GoalParams
import com.example.models.GoalResult
import com.example.models.InflationImpactParams
import com.example.models.InflationImpactResult
import com.example.models.LiabilityItem
import com.example.models.MonteCarloResult
import com.example.models.PortfolioSummary
import com.example.models.ProEntitlementState
import com.example.models.RecurringCashflowSummary
import com.example.models.RecurringFrequency
import com.example.models.RecurringTargetLink
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
import com.example.models.RetirementPlanParams
import com.example.models.RetirementPlanResult
import com.example.models.SipParams
import com.example.models.SipResult
import com.example.models.SwpParams
import com.example.models.SwpResult
import com.example.models.TaxParams
import com.example.models.TaxRegime
import com.example.models.TaxResult
import com.example.services.BillingManager
import com.example.services.BudgetEngine
import com.example.services.CsvExportScope
import com.example.services.CsvExportService
import com.example.services.DailyTipProvider
import com.example.services.DebtPayoffEngine
import com.example.services.FiScoreEngine
import com.example.services.FireEngine
import com.example.services.InvestmentEngine
import com.example.services.RecurringTransactionEngine
import com.example.services.RetirementEngine
import com.example.services.TaxEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class FireViewModel : ViewModel() {

    private val _selectedTab = MutableStateFlow(AppTab.FIRE)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _parameters = MutableStateFlow(FireParameters())
    val parameters: StateFlow<FireParameters> = _parameters.asStateFlow()

    private val _result = MutableStateFlow<FireResult?>(null)
    val result: StateFlow<FireResult?> = _result.asStateFlow()

    private val _monteCarloResult = MutableStateFlow<MonteCarloResult?>(null)
    val monteCarloResult: StateFlow<MonteCarloResult?> = _monteCarloResult.asStateFlow()

    private val _isIndianCurrency = MutableStateFlow(false)
    val isIndianCurrency: StateFlow<Boolean> = _isIndianCurrency.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    // Calculator states
    private val _calculatorMode = MutableStateFlow(CalculatorMode.SIP)
    val calculatorMode: StateFlow<CalculatorMode> = _calculatorMode.asStateFlow()

    private val _sipParams = MutableStateFlow(SipParams())
    val sipParams: StateFlow<SipParams> = _sipParams.asStateFlow()
    private val _sipResult = MutableStateFlow(InvestmentEngine.calculateSip(_sipParams.value))
    val sipResult: StateFlow<SipResult> = _sipResult.asStateFlow()

    private val _swpParams = MutableStateFlow(SwpParams())
    val swpParams: StateFlow<SwpParams> = _swpParams.asStateFlow()
    private val _swpResult = MutableStateFlow(InvestmentEngine.calculateSwp(_swpParams.value))
    val swpResult: StateFlow<SwpResult> = _swpResult.asStateFlow()

    private val _fdParams = MutableStateFlow(FdParams())
    val fdParams: StateFlow<FdParams> = _fdParams.asStateFlow()
    private val _fdResult = MutableStateFlow(InvestmentEngine.calculateFd(_fdParams.value))
    val fdResult: StateFlow<FdResult> = _fdResult.asStateFlow()

    private val _goalParams = MutableStateFlow(GoalParams())
    val goalParams: StateFlow<GoalParams> = _goalParams.asStateFlow()
    private val _goalResult = MutableStateFlow(InvestmentEngine.calculateGoal(_goalParams.value))
    val goalResult: StateFlow<GoalResult> = _goalResult.asStateFlow()

    private val _inflationParams = MutableStateFlow(InflationImpactParams())
    val inflationParams: StateFlow<InflationImpactParams> = _inflationParams.asStateFlow()
    private val _inflationResult = MutableStateFlow(InvestmentEngine.calculateInflationImpact(_inflationParams.value))
    val inflationResult: StateFlow<InflationImpactResult> = _inflationResult.asStateFlow()

    // Debt Payoff state
    private val _debtAccounts = MutableStateFlow(defaultDebtAccounts(isIndian = false))
    val debtAccounts: StateFlow<List<DebtAccount>> = _debtAccounts.asStateFlow()

    private val _extraDebtPayment = MutableStateFlow(350.0)
    val extraDebtPayment: StateFlow<Double> = _extraDebtPayment.asStateFlow()

    private val _debtStrategy = MutableStateFlow(DebtPayoffStrategy.AVALANCHE)
    val debtStrategy: StateFlow<DebtPayoffStrategy> = _debtStrategy.asStateFlow()

    private val _debtPayoffResult = MutableStateFlow(
        DebtPayoffEngine.calculateDebtPayoff(_debtAccounts.value, _extraDebtPayment.value, _debtStrategy.value)
    )
    val debtPayoffResult: StateFlow<DebtPayoffResult> = _debtPayoffResult.asStateFlow()

    // Tax Liability Estimator state
    private val _taxParams = MutableStateFlow(TaxParams())
    val taxParams: StateFlow<TaxParams> = _taxParams.asStateFlow()
    private val _taxResult = MutableStateFlow(TaxEngine.calculateTax(_taxParams.value))
    val taxResult: StateFlow<TaxResult> = _taxResult.asStateFlow()

    // Retirement Planning Tool state
    private val _retirementPlanParams = MutableStateFlow(RetirementPlanParams())
    val retirementPlanParams: StateFlow<RetirementPlanParams> = _retirementPlanParams.asStateFlow()
    private val _retirementPlanResult = MutableStateFlow(
        RetirementEngine.calculateRetirementPlan(_retirementPlanParams.value)
    )
    val retirementPlanResult: StateFlow<RetirementPlanResult> = _retirementPlanResult.asStateFlow()

    // Multi-Asset Portfolio & Net Worth state
    private val _holdings = MutableStateFlow(defaultHoldings(isIndian = false))
    val holdings: StateFlow<List<AssetHolding>> = _holdings.asStateFlow()

    private val _liabilities = MutableStateFlow<List<LiabilityItem>>(emptyList())
    val liabilities: StateFlow<List<LiabilityItem>> = _liabilities.asStateFlow()

    private val _portfolioProjectionYears = MutableStateFlow(10)
    val portfolioProjectionYears: StateFlow<Int> = _portfolioProjectionYears.asStateFlow()

    private val _portfolioSummary = MutableStateFlow(
        InvestmentEngine.calculatePortfolio(_holdings.value, _portfolioProjectionYears.value, _liabilities.value)
    )
    val portfolioSummary: StateFlow<PortfolioSummary> = _portfolioSummary.asStateFlow()

    // Recurring Transaction Scheduler state
    private val _recurringTransactions = MutableStateFlow(defaultRecurringTransactions(isIndian = false))
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()

    private val _cashflowLogs = MutableStateFlow<List<CashflowExecutionLog>>(emptyList())
    val cashflowLogs: StateFlow<List<CashflowExecutionLog>> = _cashflowLogs.asStateFlow()

    private val _cashflowSummary = MutableStateFlow(
        RecurringTransactionEngine.calculateCashflowSummary(_recurringTransactions.value)
    )
    val cashflowSummary: StateFlow<RecurringCashflowSummary> = _cashflowSummary.asStateFlow()

    // Monthly Budget Planner state
    private val _budgetCategories = MutableStateFlow(defaultBudgetCategories(isIndian = false))
    val budgetCategories: StateFlow<List<BudgetCategoryEntry>> = _budgetCategories.asStateFlow()

    private val _useRecurringBudgetIncome = MutableStateFlow(true)
    val useRecurringBudgetIncome: StateFlow<Boolean> = _useRecurringBudgetIncome.asStateFlow()

    private val _customBudgetIncome = MutableStateFlow(7_500.0)
    val customBudgetIncome: StateFlow<Double> = _customBudgetIncome.asStateFlow()

    private val _budgetSummary = MutableStateFlow(
        BudgetEngine.calculateBudgetSummary(
            categories = _budgetCategories.value,
            recurringTransactions = _recurringTransactions.value,
            useRecurringIncome = _useRecurringBudgetIncome.value,
            customMonthlyIncome = _customBudgetIncome.value
        )
    )
    val budgetSummary: StateFlow<BudgetPlanSummary> = _budgetSummary.asStateFlow()

    // Financial Independence Score & Gamification state
    private var peakDebtBalance: Double = _debtAccounts.value.sumOf { it.balance }
    private val _fiScoreSummary = MutableStateFlow(
        FiScoreEngine.calculateScore(
            portfolioSummary = _portfolioSummary.value,
            debtAccounts = _debtAccounts.value,
            peakDebtBalance = peakDebtBalance,
            fireParams = _parameters.value,
            fireResult = _result.value,
            budgetSummary = _budgetSummary.value,
            recurringTransactions = _recurringTransactions.value,
            extraDebtPayment = _extraDebtPayment.value,
            isIndianCurrency = _isIndianCurrency.value
        )
    )
    val fiScoreSummary: StateFlow<FiScoreSummary> = _fiScoreSummary.asStateFlow()

    private val _recentAchievementUnlock = MutableStateFlow<FiAchievement?>(null)
    val recentAchievementUnlock: StateFlow<FiAchievement?> = _recentAchievementUnlock.asStateFlow()

    // Daily Offline Financial Quote & Tip state
    private var dailyTipIndex: Int = DailyTipProvider.dailyIndexForDate()
    private val _dailyTip = MutableStateFlow(DailyTipProvider.tipAtIndex(dailyTipIndex))
    val dailyTip: StateFlow<DailyFinancialTip> = _dailyTip.asStateFlow()

    private val _bookmarkedTipIds = MutableStateFlow<Set<Int>>(emptySet())
    val bookmarkedTipIds: StateFlow<Set<Int>> = _bookmarkedTipIds.asStateFlow()

    // Play Store One-Time Pro Purchase ($3.99) state
    private val _proState = MutableStateFlow(ProEntitlementState())
    val proState: StateFlow<ProEntitlementState> = _proState.asStateFlow()
    private var billingManager: BillingManager? = null

    private val _savedProfileNames = MutableStateFlow<List<String>>(emptyList())
    val savedProfileNames: StateFlow<List<String>> = _savedProfileNames.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val holdingListType = Types.newParameterizedType(List::class.java, AssetHolding::class.java)
    private val holdingListAdapter = moshi.adapter<List<AssetHolding>>(holdingListType)
    private val liabilityListType = Types.newParameterizedType(List::class.java, LiabilityItem::class.java)
    private val liabilityListAdapter = moshi.adapter<List<LiabilityItem>>(liabilityListType)
    private val debtListType = Types.newParameterizedType(List::class.java, DebtAccount::class.java)
    private val debtListAdapter = moshi.adapter<List<DebtAccount>>(debtListType)
    private val recurringListType = Types.newParameterizedType(List::class.java, RecurringTransaction::class.java)
    private val recurringListAdapter = moshi.adapter<List<RecurringTransaction>>(recurringListType)
    private val logListType = Types.newParameterizedType(List::class.java, CashflowExecutionLog::class.java)
    private val logListAdapter = moshi.adapter<List<CashflowExecutionLog>>(logListType)
    private val budgetListType = Types.newParameterizedType(List::class.java, BudgetCategoryEntry::class.java)
    private val budgetListAdapter = moshi.adapter<List<BudgetCategoryEntry>>(budgetListType)

    init {
        recalculateFire()
        recalculateFiScore(emitUnlockEvent = false)
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun selectCalculatorMode(mode: CalculatorMode) {
        _calculatorMode.value = mode
    }

    fun nextDailyTip() {
        dailyTipIndex += 1
        _dailyTip.value = DailyTipProvider.tipAtIndex(dailyTipIndex)
    }

    fun previousDailyTip() {
        dailyTipIndex -= 1
        _dailyTip.value = DailyTipProvider.tipAtIndex(dailyTipIndex)
    }

    fun toggleBookmarkDailyTip(context: Context) {
        val currentId = _dailyTip.value.id
        val currentSet = _bookmarkedTipIds.value.toMutableSet()
        if (!currentSet.add(currentId)) {
            currentSet.remove(currentId)
        }
        _bookmarkedTipIds.value = currentSet
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKMARKED_TIPS, currentSet.joinToString(",")).apply()
    }

    fun updateParameters(params: FireParameters) {
        val safeTargetAge = params.targetAge.coerceAtLeast(params.currentAge + 1)
        _parameters.value = params.copy(targetAge = safeTargetAge)
        recalculateFire()
    }

    fun applyFirePreset(name: String) {
        val isIn = _isIndianCurrency.value
        val preset = when (name) {
            "Lean" -> FireParameters(
                currentAge = 28,
                targetAge = 45,
                currentSavings = if (isIn) 15_00_000.0 else 50_000.0,
                monthlySip = if (isIn) 50_000.0 else 1_800.0,
                stepUpRate = 0.08,
                expectedReturn = if (isIn) 0.11 else 0.08,
                inflationRate = if (isIn) 0.06 else 0.03,
                taxRate = if (isIn) 0.125 else 0.15,
                monthlyExpense = if (isIn) 50_000.0 else 2_800.0,
                safeWithdrawalRate = 0.04
            )
            "Fat" -> FireParameters(
                currentAge = 32,
                targetAge = 50,
                currentSavings = if (isIn) 1_00_00_000.0 else 350_000.0,
                monthlySip = if (isIn) 2_00_000.0 else 5_500.0,
                stepUpRate = 0.10,
                expectedReturn = if (isIn) 0.12 else 0.085,
                inflationRate = if (isIn) 0.06 else 0.03,
                taxRate = if (isIn) 0.125 else 0.18,
                monthlyExpense = if (isIn) 1_75_000.0 else 8_000.0,
                safeWithdrawalRate = 0.035
            )
            else -> FireParameters(
                currentAge = 30,
                targetAge = 50,
                currentSavings = if (isIn) 30_00_000.0 else 100_000.0,
                monthlySip = if (isIn) 75_000.0 else 2_500.0,
                stepUpRate = 0.06,
                expectedReturn = if (isIn) 0.11 else 0.08,
                inflationRate = if (isIn) 0.06 else 0.03,
                taxRate = if (isIn) 0.125 else 0.15,
                monthlyExpense = if (isIn) 80_000.0 else 4_000.0,
                safeWithdrawalRate = 0.04
            )
        }
        updateParameters(preset)
    }

    fun toggleCurrency() {
        val switchingToInr = !_isIndianCurrency.value
        _isIndianCurrency.value = switchingToInr
        // Keep financial figures sensible when toggling between USD and INR scales
        val factor = if (switchingToInr) 25.0 else 1.0 / 25.0
        val p = _parameters.value
        _parameters.value = p.copy(
            currentSavings = (p.currentSavings * factor).coerceAtLeast(0.0),
            monthlySip = (p.monthlySip * factor).coerceAtLeast(0.0),
            monthlyExpense = (p.monthlyExpense * factor).coerceAtLeast(500.0),
            inflationRate = if (switchingToInr && p.inflationRate < 0.04) 0.06 else if (!switchingToInr && p.inflationRate > 0.05) 0.03 else p.inflationRate
        )
        recalculateFire()

        updateSipParams(
            _sipParams.value.let {
                it.copy(lumpsum = it.lumpsum * factor, monthlySip = it.monthlySip * factor)
            }
        )
        updateSwpParams(
            _swpParams.value.let {
                it.copy(initialCorpus = it.initialCorpus * factor, monthlyWithdrawal = it.monthlyWithdrawal * factor)
            }
        )
        updateFdParams(
            _fdParams.value.let {
                it.copy(principal = it.principal * factor)
            }
        )
        updateGoalParams(
            _goalParams.value.let {
                it.copy(targetGoal = it.targetGoal * factor, currentSavings = it.currentSavings * factor)
            }
        )
        updateInflationParams(
            _inflationParams.value.let {
                it.copy(goalAmountToday = it.goalAmountToday * factor)
            }
        )
        _holdings.value = _holdings.value.map {
            it.copy(
                currentValue = it.currentValue * factor,
                monthlyContribution = it.monthlyContribution * factor,
                avgBuyPrice = it.avgBuyPrice * factor,
                currentMarketPrice = it.currentMarketPrice * factor,
                investedCost = it.investedCost * factor
            )
        }
        _liabilities.value = _liabilities.value.map {
            it.copy(balance = it.balance * factor)
        }
        _debtAccounts.value = _debtAccounts.value.map {
            it.copy(balance = it.balance * factor, minimumPayment = it.minimumPayment * factor)
        }
        _extraDebtPayment.value = (_extraDebtPayment.value * factor).coerceAtLeast(0.0)
        recalculateDebtPayoff()
        val currentTax = _taxParams.value
        val targetRegime = if (switchingToInr) {
            if (currentTax.regime.isIndian) currentTax.regime else TaxRegime.IN_OLD
        } else {
            if (!currentTax.regime.isIndian) currentTax.regime else TaxRegime.US_SINGLE
        }
        updateTaxParams(
            currentTax.copy(
                regime = targetRegime,
                annualIncome = (currentTax.annualIncome * factor).coerceAtLeast(0.0),
                retirementDeduction = (currentTax.retirementDeduction * factor).coerceAtLeast(0.0),
                additionalInvestmentDeduction = (currentTax.additionalInvestmentDeduction * factor).coerceAtLeast(0.0),
                realizedCapitalGains = (currentTax.realizedCapitalGains * factor).coerceAtLeast(0.0)
            )
        )
        val currentRet = _retirementPlanParams.value
        updateRetirementPlanParams(
            currentRet.copy(
                targetMonthlyExpensesToday = (currentRet.targetMonthlyExpensesToday * factor).coerceAtLeast(500.0),
                currentRetirementSavings = (currentRet.currentRetirementSavings * factor).coerceAtLeast(0.0),
                annualInflationRate = if (switchingToInr && currentRet.annualInflationRate < 0.05) 0.06
                else if (!switchingToInr && currentRet.annualInflationRate > 0.05) 0.04
                else currentRet.annualInflationRate
            )
        )
        _recurringTransactions.value = _recurringTransactions.value.map {
            it.copy(amount = (it.amount * factor).coerceAtLeast(0.0))
        }
        _budgetCategories.value = _budgetCategories.value.map {
            it.copy(
                monthlyLimit = (it.monthlyLimit * factor).coerceAtLeast(0.0),
                manualSpending = (it.manualSpending * factor).coerceAtLeast(0.0),
                savingTarget = (it.savingTarget * factor).coerceAtLeast(0.0)
            )
        }
        _customBudgetIncome.value = (_customBudgetIncome.value * factor).coerceAtLeast(0.0)
        peakDebtBalance = (peakDebtBalance * factor).coerceAtLeast(0.0)
        recalculateCashflow()
        recalculatePortfolio()
        recalculateFiScore(emitUnlockEvent = false)
    }

    private fun recalculateFire() {
        _result.value = FireEngine.calculateFire(_parameters.value)
        _monteCarloResult.value = null
        recalculateFiScore(emitUnlockEvent = false)
    }

    fun runMonteCarlo() {
        viewModelScope.launch {
            _isSimulating.value = true
            val currentRes = _result.value
            val startBalance = currentRes?.netAccumulated ?: _parameters.value.currentSavings
            val res = FireEngine.runMonteCarloSimulation(_parameters.value, startBalance)
            _monteCarloResult.value = res
            _isSimulating.value = false
        }
    }

    // Calculators updaters
    fun updateSipParams(params: SipParams) {
        _sipParams.value = params
        _sipResult.value = InvestmentEngine.calculateSip(params)
    }

    fun updateSwpParams(params: SwpParams) {
        _swpParams.value = params
        _swpResult.value = InvestmentEngine.calculateSwp(params)
    }

    fun updateFdParams(params: FdParams) {
        _fdParams.value = params
        _fdResult.value = InvestmentEngine.calculateFd(params)
    }

    fun updateGoalParams(params: GoalParams) {
        _goalParams.value = params
        _goalResult.value = InvestmentEngine.calculateGoal(params)
    }

    fun updateInflationParams(params: InflationImpactParams) {
        _inflationParams.value = params
        _inflationResult.value = InvestmentEngine.calculateInflationImpact(params)
    }

    fun updateTaxParams(params: TaxParams) {
        _taxParams.value = params
        _taxResult.value = TaxEngine.calculateTax(params)
    }

    fun selectTaxRegime(regime: TaxRegime) {
        val current = _taxParams.value
        if (current.regime == regime) return
        val adjusted = if (current.regime.isIndian != regime.isIndian) {
            if (regime.isIndian) {
                current.copy(
                    regime = regime,
                    annualIncome = 1_800_000.0,
                    retirementDeduction = 150_000.0,
                    additionalInvestmentDeduction = 50_000.0,
                    realizedCapitalGains = 200_000.0
                )
            } else {
                current.copy(
                    regime = regime,
                    annualIncome = 110_000.0,
                    retirementDeduction = 15_000.0,
                    additionalInvestmentDeduction = 4_000.0,
                    realizedCapitalGains = 8_000.0
                )
            }
        } else {
            current.copy(regime = regime)
        }
        updateTaxParams(adjusted)
    }

    fun syncEffectiveTaxRateToFire() {
        val rate = _taxResult.value.effectiveTaxRate.coerceIn(0.0, 0.45)
        updateParameters(_parameters.value.copy(taxRate = rate))
    }

    fun updateRetirementPlanParams(params: RetirementPlanParams) {
        val safeCurrentAge = params.currentAge.coerceIn(18, 85)
        val safeRetirementAge = params.retirementAge.coerceIn(safeCurrentAge + 1, 95)
        val safeLifeExpectancy = params.lifeExpectancy.coerceIn(safeRetirementAge + 1, 110)
        val normalized = params.copy(
            currentAge = safeCurrentAge,
            retirementAge = safeRetirementAge,
            lifeExpectancy = safeLifeExpectancy,
            targetMonthlyExpensesToday = params.targetMonthlyExpensesToday.coerceAtLeast(0.0),
            annualInflationRate = params.annualInflationRate.coerceIn(0.0, 0.30),
            preRetirementReturn = params.preRetirementReturn.coerceIn(0.0, 0.30),
            postRetirementReturn = params.postRetirementReturn.coerceIn(0.0, 0.25),
            currentRetirementSavings = params.currentRetirementSavings.coerceAtLeast(0.0)
        )
        _retirementPlanParams.value = normalized
        _retirementPlanResult.value = RetirementEngine.calculateRetirementPlan(normalized)
    }

    fun syncRetirementPlanToFire() {
        val rp = _retirementPlanParams.value
        val res = _retirementPlanResult.value
        val current = _parameters.value
        updateParameters(
            current.copy(
                currentAge = res.currentAge,
                targetAge = res.retirementAge,
                currentSavings = rp.currentRetirementSavings,
                monthlyExpense = rp.targetMonthlyExpensesToday.coerceAtLeast(500.0),
                inflationRate = rp.annualInflationRate,
                expectedReturn = rp.preRetirementReturn.coerceIn(0.01, 0.25),
                monthlySip = res.requiredMonthlySip.coerceAtLeast(0.0)
            )
        )
    }

    // Portfolio & Net Worth actions
    fun initPortfolioAndProfiles(context: Context) {
        refreshSavedProfiles(context)
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HOLDINGS, null)
        if (json != null) {
            try {
                val parsed = holdingListAdapter.fromJson(json)
                if (!parsed.isNullOrEmpty()) {
                    _holdings.value = parsed
                }
            } catch (_: Exception) {
            }
        }
        val liabilitiesJson = prefs.getString(KEY_LIABILITIES, null)
        if (liabilitiesJson != null) {
            try {
                val parsedLiabilities = liabilityListAdapter.fromJson(liabilitiesJson)
                if (parsedLiabilities != null) {
                    _liabilities.value = parsedLiabilities
                }
            } catch (_: Exception) {
            }
        }
        val debtsJson = prefs.getString(KEY_DEBTS, null)
        if (debtsJson != null) {
            try {
                val parsedDebts = debtListAdapter.fromJson(debtsJson)
                if (parsedDebts != null) {
                    _debtAccounts.value = parsedDebts
                }
            } catch (_: Exception) {
            }
        }
        val recurringJson = prefs.getString(KEY_RECURRING, null)
        if (recurringJson != null) {
            try {
                val parsedRecurring = recurringListAdapter.fromJson(recurringJson)
                if (parsedRecurring != null) {
                    _recurringTransactions.value = parsedRecurring
                }
            } catch (_: Exception) {
            }
        }
        val logsJson = prefs.getString(KEY_CASHFLOW_LOGS, null)
        if (logsJson != null) {
            try {
                val parsedLogs = logListAdapter.fromJson(logsJson)
                if (parsedLogs != null) {
                    _cashflowLogs.value = parsedLogs
                }
            } catch (_: Exception) {
            }
        }
        val budgetJson = prefs.getString(KEY_BUDGET_CATEGORIES, null)
        if (budgetJson != null) {
            try {
                val parsedBudget = budgetListAdapter.fromJson(budgetJson)
                if (parsedBudget != null) {
                    _budgetCategories.value = parsedBudget
                }
            } catch (_: Exception) {
            }
        }
        if (prefs.contains(KEY_BUDGET_USE_RECURRING_INCOME)) {
            _useRecurringBudgetIncome.value = prefs.getBoolean(KEY_BUDGET_USE_RECURRING_INCOME, true)
        }
        if (prefs.contains(KEY_BUDGET_CUSTOM_INCOME)) {
            _customBudgetIncome.value = prefs.getFloat(KEY_BUDGET_CUSTOM_INCOME, 7_500f).toDouble()
        }
        if (prefs.contains(KEY_PEAK_DEBT_BALANCE)) {
            peakDebtBalance = prefs.getFloat(KEY_PEAK_DEBT_BALANCE, peakDebtBalance.toFloat()).toDouble()
        } else {
            val loadedDebt = maxOf(
                _debtAccounts.value.sumOf { it.balance },
                _liabilities.value.sumOf { it.balance }
            )
            peakDebtBalance = maxOf(peakDebtBalance, loadedDebt)
        }
        val savedTipIds = prefs.getString(KEY_BOOKMARKED_TIPS, null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()
        _bookmarkedTipIds.value = savedTipIds
        val cachedPro = prefs.getBoolean(KEY_PRO_UNLOCKED, false)
        if (cachedPro) {
            _proState.value = _proState.value.copy(isProUnlocked = true)
        }
        initBilling(context)
        processDueRecurringTransactions(context = context, requireAutoExecuteFlag = true)
        recalculateCashflow()
        recalculateDebtPayoff()
        recalculatePortfolio()
        recalculateFiScore(emitUnlockEvent = false)
    }

    // Google Play Billing ($3.99 One-Time Aegis Pro) actions
    fun initBilling(context: Context) {
        if (billingManager != null) {
            billingManager?.queryExistingPurchases(notifyIfNotFound = false)
            return
        }
        val appContext = context.applicationContext
        val manager = BillingManager(
            context = appContext,
            onProductPriceLoaded = { priceLabel, storeReady ->
                _proState.value = _proState.value.copy(
                    formattedPrice = priceLabel,
                    isStoreReady = storeReady
                )
            },
            onEntitlementUpdated = { isPro, isPending, message ->
                val prefs = appContext.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_PRO_UNLOCKED, isPro).apply()
                _proState.value = _proState.value.copy(
                    isProUnlocked = isPro,
                    isPurchasePending = isPending,
                    statusMessage = message
                )
            }
        )
        billingManager = manager
        manager.startConnection()
    }

    fun launchProPurchase(activity: Activity) {
        val manager = billingManager
        if (manager == null) {
            initBilling(activity)
        }
        billingManager?.launchProPurchase(activity)
    }

    fun restoreProPurchases() {
        billingManager?.queryExistingPurchases(notifyIfNotFound = true)
    }

    fun clearBillingStatusMessage() {
        _proState.value = _proState.value.copy(statusMessage = null)
    }

    @VisibleForTesting
    fun setProUnlockedForTest(unlocked: Boolean) {
        _proState.value = _proState.value.copy(isProUnlocked = unlocked)
    }

    override fun onCleared() {
        super.onCleared()
        billingManager?.destroy()
        billingManager = null
    }

    // Recurring Transaction Scheduler actions
    fun addOrUpdateRecurringTransaction(context: Context, transaction: RecurringTransaction) {
        val idx = _recurringTransactions.value.indexOfFirst { it.id == transaction.id }
        _recurringTransactions.value = if (idx >= 0) {
            _recurringTransactions.value.toMutableList().apply { set(idx, transaction) }
        } else {
            _recurringTransactions.value + transaction.copy(id = UUID.randomUUID().toString())
        }
        persistRecurringTransactions(context)
        recalculateCashflow()
    }

    fun toggleRecurringTransactionActive(context: Context, transactionId: String) {
        _recurringTransactions.value = _recurringTransactions.value.map {
            if (it.id == transactionId) it.copy(isActive = !it.isActive) else it
        }
        persistRecurringTransactions(context)
        recalculateCashflow()
    }

    fun removeRecurringTransaction(context: Context, transactionId: String) {
        _recurringTransactions.value = _recurringTransactions.value.filterNot { it.id == transactionId }
        persistRecurringTransactions(context)
        recalculateCashflow()
    }

    fun processDueRecurringTransactions(
        context: Context,
        requireAutoExecuteFlag: Boolean = false,
        yearMonth: String = RecurringTransactionEngine.currentYearMonth(),
        dayOfMonth: Int = RecurringTransactionEngine.currentDayOfMonth()
    ): Int {
        val outcome = RecurringTransactionEngine.processDueTransactions(
            transactions = _recurringTransactions.value,
            holdings = _holdings.value,
            debts = _debtAccounts.value,
            yearMonth = yearMonth,
            dayOfMonth = dayOfMonth,
            requireAutoExecuteFlag = requireAutoExecuteFlag
        )
        if (outcome.executedCount > 0) {
            _recurringTransactions.value = outcome.updatedTransactions
            _holdings.value = outcome.updatedHoldings
            _debtAccounts.value = outcome.updatedDebts
            _cashflowLogs.value = (outcome.newLogs + _cashflowLogs.value).take(30)
            persistRecurringTransactions(context)
            persistHoldings(context)
            persistDebtAccounts(context)
            persistCashflowLogs(context)
            recalculatePortfolio()
            recalculateDebtPayoff()
        }
        recalculateCashflow(yearMonth, dayOfMonth)
        return outcome.executedCount
    }

    fun applyCashflowDebtRepayment() {
        val summary = _cashflowSummary.value
        val targetExtra = if (summary.monthlyDebtAutoRepayment > 0.0) {
            summary.monthlyDebtAutoRepayment
        } else {
            summary.netMonthlyCashflow.coerceAtLeast(0.0)
        }
        updateExtraDebtPayment(targetExtra)
    }

    fun applyCashflowSurplusToDebt() {
        val surplus = _cashflowSummary.value.netMonthlyCashflow.coerceAtLeast(0.0)
        val combined = (_cashflowSummary.value.monthlyDebtAutoRepayment + surplus * 0.5).coerceAtLeast(0.0)
        updateExtraDebtPayment(combined)
    }

    fun applyCashflowToPortfolioSip(context: Context): Double {
        val summary = _cashflowSummary.value
        val autoSip = summary.monthlyPortfolioAutoContribution
        val holdingSipMap = _recurringTransactions.value
            .filter { it.isActive && it.targetLink == RecurringTargetLink.PORTFOLIO_SIP }
            .groupBy { it.linkedEntityId }

        if (holdingSipMap.isNotEmpty() && _holdings.value.isNotEmpty()) {
            val fallbackId = _holdings.value.first().id
            val perHolding = mutableMapOf<String, Double>()
            holdingSipMap.forEach { (entityId, rules) ->
                val targetId = if (!entityId.isNullOrBlank() && _holdings.value.any { it.id == entityId }) entityId else fallbackId
                val monthlySum = rules.sumOf { it.amount / it.frequency.monthsInterval.toDouble() }
                perHolding[targetId] = (perHolding[targetId] ?: 0.0) + monthlySum
            }
            _holdings.value = _holdings.value.map { h ->
                val scheduled = perHolding[h.id]
                if (scheduled != null) h.copy(monthlyContribution = scheduled) else h
            }
            persistHoldings(context)
            recalculatePortfolio()
        }
        return autoSip
    }

    private fun recalculateCashflow(
        yearMonth: String = RecurringTransactionEngine.currentYearMonth(),
        dayOfMonth: Int = RecurringTransactionEngine.currentDayOfMonth()
    ) {
        _cashflowSummary.value = RecurringTransactionEngine.calculateCashflowSummary(
            transactions = _recurringTransactions.value,
            yearMonth = yearMonth,
            dayOfMonth = dayOfMonth
        )
        recalculateBudget()
    }

    // Monthly Budget Planner actions
    fun addOrUpdateBudgetCategory(context: Context, category: BudgetCategoryEntry) {
        val idx = _budgetCategories.value.indexOfFirst { it.id == category.id }
        _budgetCategories.value = if (idx >= 0) {
            _budgetCategories.value.toMutableList().apply { set(idx, category) }
        } else {
            _budgetCategories.value + category.copy(id = UUID.randomUUID().toString())
        }
        persistBudgetState(context)
        recalculateBudget()
    }

    fun adjustBudgetManualSpending(context: Context, categoryId: String, delta: Double) {
        _budgetCategories.value = _budgetCategories.value.map { entry ->
            if (entry.id == categoryId) {
                entry.copy(manualSpending = (entry.manualSpending + delta).coerceAtLeast(0.0))
            } else {
                entry
            }
        }
        persistBudgetState(context)
        recalculateBudget()
    }

    fun removeBudgetCategory(context: Context, categoryId: String) {
        _budgetCategories.value = _budgetCategories.value.filterNot { it.id == categoryId }
        persistBudgetState(context)
        recalculateBudget()
    }

    fun updateBudgetIncomeSettings(
        context: Context,
        useRecurringIncome: Boolean,
        customMonthlyIncome: Double = _customBudgetIncome.value
    ) {
        _useRecurringBudgetIncome.value = useRecurringIncome
        _customBudgetIncome.value = customMonthlyIncome.coerceAtLeast(0.0)
        persistBudgetState(context)
        recalculateBudget()
    }

    fun syncBudgetExpensesToFire(): Double {
        val summary = _budgetSummary.value
        val livingSpend = summary.categories
            .filterNot { it.entry.name.equals("Investment SIP", ignoreCase = true) }
            .sumOf { it.effectiveSpent }
            .coerceAtLeast(500.0)
        updateParameters(_parameters.value.copy(monthlyExpense = livingSpend))
        return livingSpend
    }

    private fun recalculateBudget() {
        _budgetSummary.value = BudgetEngine.calculateBudgetSummary(
            categories = _budgetCategories.value,
            recurringTransactions = _recurringTransactions.value,
            useRecurringIncome = _useRecurringBudgetIncome.value,
            customMonthlyIncome = _customBudgetIncome.value
        )
        recalculateFiScore()
    }

    fun clearRecentAchievementUnlock() {
        _recentAchievementUnlock.value = null
    }

    private fun recalculateFiScore(emitUnlockEvent: Boolean = true) {
        val currentDebt = maxOf(
            _debtAccounts.value.sumOf { it.balance.coerceAtLeast(0.0) },
            _portfolioSummary.value.totalLiabilities.coerceAtLeast(0.0)
        )
        if (currentDebt > peakDebtBalance) {
            peakDebtBalance = currentDebt
        }
        val previousUnlockedIds = _fiScoreSummary.value.achievements
            .filter { it.isUnlocked }
            .map { it.id }
            .toSet()

        val updated = FiScoreEngine.calculateScore(
            portfolioSummary = _portfolioSummary.value,
            debtAccounts = _debtAccounts.value,
            peakDebtBalance = peakDebtBalance,
            fireParams = _parameters.value,
            fireResult = _result.value,
            budgetSummary = _budgetSummary.value,
            recurringTransactions = _recurringTransactions.value,
            extraDebtPayment = _extraDebtPayment.value,
            isIndianCurrency = _isIndianCurrency.value
        )
        _fiScoreSummary.value = updated

        if (emitUnlockEvent) {
            val newlyUnlocked = updated.achievements.firstOrNull {
                it.isUnlocked && !previousUnlockedIds.contains(it.id)
            }
            if (newlyUnlocked != null) {
                _recentAchievementUnlock.value = newlyUnlocked
            }
        }
    }

    private fun persistBudgetState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BUDGET_CATEGORIES, budgetListAdapter.toJson(_budgetCategories.value))
            .putBoolean(KEY_BUDGET_USE_RECURRING_INCOME, _useRecurringBudgetIncome.value)
            .putFloat(KEY_BUDGET_CUSTOM_INCOME, _customBudgetIncome.value.toFloat())
            .apply()
    }

    private fun persistRecurringTransactions(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECURRING, recurringListAdapter.toJson(_recurringTransactions.value)).apply()
    }

    private fun persistCashflowLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CASHFLOW_LOGS, logListAdapter.toJson(_cashflowLogs.value)).apply()
    }

    // Debt Payoff actions
    fun addOrUpdateDebtAccount(context: Context, account: DebtAccount) {
        val idx = _debtAccounts.value.indexOfFirst { it.id == account.id }
        _debtAccounts.value = if (idx >= 0) {
            _debtAccounts.value.toMutableList().apply { set(idx, account) }
        } else {
            _debtAccounts.value + account.copy(id = UUID.randomUUID().toString())
        }
        persistDebtAccounts(context)
        recalculateDebtPayoff()
    }

    fun removeDebtAccount(context: Context, accountId: String) {
        _debtAccounts.value = _debtAccounts.value.filterNot { it.id == accountId }
        persistDebtAccounts(context)
        recalculateDebtPayoff()
    }

    fun importLiabilitiesToDebtTracker(context: Context): Int {
        val existingIds = _debtAccounts.value.map { it.id }.toSet()
        val imported = _liabilities.value.filter { it.balance > 0.0 && !existingIds.contains(it.id) }.map { item ->
            val estimatedApr = if (item.category.contains("Credit", ignoreCase = true)) 0.21 else item.interestRate.coerceAtLeast(0.05)
            val monthlyInterest = item.balance * (estimatedApr / 12.0)
            val estMin = maxOf(monthlyInterest * 1.35, item.balance * 0.025)
            DebtAccount(
                id = item.id,
                name = item.name,
                category = item.category,
                balance = item.balance,
                annualApr = estimatedApr,
                minimumPayment = estMin
            )
        }
        if (imported.isNotEmpty()) {
            _debtAccounts.value = _debtAccounts.value + imported
            persistDebtAccounts(context)
            recalculateDebtPayoff()
        }
        return imported.size
    }

    fun updateExtraDebtPayment(extra: Double) {
        _extraDebtPayment.value = extra.coerceAtLeast(0.0)
        recalculateDebtPayoff()
    }

    fun selectDebtStrategy(strategy: DebtPayoffStrategy) {
        _debtStrategy.value = strategy
        recalculateDebtPayoff()
    }

    private fun recalculateDebtPayoff() {
        _debtPayoffResult.value = DebtPayoffEngine.calculateDebtPayoff(
            accounts = _debtAccounts.value,
            extraMonthlyPayment = _extraDebtPayment.value,
            selectedStrategy = _debtStrategy.value
        )
        recalculateFiScore()
    }

    private fun persistDebtAccounts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        val json = debtListAdapter.toJson(_debtAccounts.value)
        val currentDebt = maxOf(_debtAccounts.value.sumOf { it.balance }, _liabilities.value.sumOf { it.balance })
        if (currentDebt > peakDebtBalance) peakDebtBalance = currentDebt
        prefs.edit()
            .putString(KEY_DEBTS, json)
            .putFloat(KEY_PEAK_DEBT_BALANCE, peakDebtBalance.toFloat())
            .apply()
    }

    fun addOrUpdateHolding(context: Context, holding: AssetHolding) {
        val existingIndex = _holdings.value.indexOfFirst { it.id == holding.id }
        _holdings.value = if (existingIndex >= 0) {
            _holdings.value.toMutableList().apply { set(existingIndex, holding) }
        } else {
            _holdings.value + holding.copy(id = UUID.randomUUID().toString())
        }
        persistHoldings(context)
        recalculatePortfolio()
    }

    fun updateHoldingMarketValueOrPrice(context: Context, holdingId: String, newMarketInput: Double) {
        val safeVal = newMarketInput.coerceAtLeast(0.0)
        _holdings.value = _holdings.value.map { h ->
            if (h.id != holdingId) h
            else if (h.quantity > 0.0 && h.currentMarketPrice > 0.0) {
                h.copy(
                    currentMarketPrice = safeVal,
                    currentValue = h.quantity * safeVal
                )
            } else {
                h.copy(currentValue = safeVal)
            }
        }
        persistHoldings(context)
        recalculatePortfolio()
    }

    fun removeHolding(context: Context, holdingId: String) {
        _holdings.value = _holdings.value.filterNot { it.id == holdingId }
        persistHoldings(context)
        recalculatePortfolio()
    }

    fun addOrUpdateLiability(context: Context, liability: LiabilityItem) {
        val idx = _liabilities.value.indexOfFirst { it.id == liability.id }
        _liabilities.value = if (idx >= 0) {
            _liabilities.value.toMutableList().apply { set(idx, liability) }
        } else {
            _liabilities.value + liability.copy(id = UUID.randomUUID().toString())
        }
        persistLiabilities(context)
        recalculatePortfolio()
    }

    fun removeLiability(context: Context, liabilityId: String) {
        _liabilities.value = _liabilities.value.filterNot { it.id == liabilityId }
        persistLiabilities(context)
        recalculatePortfolio()
    }

    fun applyPortfolioPreset(context: Context, presetName: String) {
        val m = if (_isIndianCurrency.value) 25.0 else 1.0
        _holdings.value = when (presetName) {
            "Growth" -> listOf(
                AssetHolding(UUID.randomUUID().toString(), "S&P 500 Index ETF", AssetType.EQUITY, 60_000.0 * m, 1_400.0 * m, 0.12, 0.125, symbol = "VOO", quantity = 120.0, avgBuyPrice = 420.0 * m, currentMarketPrice = 500.0 * m, investedCost = 50_400.0 * m),
                AssetHolding(UUID.randomUUID().toString(), "Flexi-Cap Mutual Fund", AssetType.MUTUAL_FUND, 35_000.0 * m, 900.0 * m, 0.115, 0.125, symbol = "FLEXI", quantity = 350.0, avgBuyPrice = 85.0 * m, currentMarketPrice = 100.0 * m, investedCost = 29_750.0 * m),
                AssetHolding(UUID.randomUUID().toString(), "Emergency FD", AssetType.FIXED_DEPOSIT, 15_000.0 * m, 200.0 * m, 0.068, 0.25, symbol = "FD", investedCost = 14_200.0 * m)
            )
            "Conservative" -> listOf(
                AssetHolding(UUID.randomUUID().toString(), "High-Yield Fixed Deposit", AssetType.FIXED_DEPOSIT, 65_000.0 * m, 1_000.0 * m, 0.072, 0.30, symbol = "FD", investedCost = 60_000.0 * m),
                AssetHolding(UUID.randomUUID().toString(), "AAA Corporate & Govt Bonds", AssetType.BONDS, 40_000.0 * m, 700.0 * m, 0.078, 0.20, symbol = "BND", quantity = 400.0, avgBuyPrice = 96.0 * m, currentMarketPrice = 100.0 * m, investedCost = 38_400.0 * m),
                AssetHolding(UUID.randomUUID().toString(), "Large-Cap Mutual Fund", AssetType.MUTUAL_FUND, 25_000.0 * m, 500.0 * m, 0.105, 0.125, symbol = "LCAP", quantity = 250.0, avgBuyPrice = 90.0 * m, currentMarketPrice = 100.0 * m, investedCost = 22_500.0 * m)
            )
            else -> defaultHoldings(_isIndianCurrency.value)
        }
        persistHoldings(context)
        recalculatePortfolio()
    }

    fun updatePortfolioProjectionYears(years: Int) {
        _portfolioProjectionYears.value = years.coerceIn(1, 40)
        recalculatePortfolio()
    }

    fun syncPortfolioToFire() {
        val summary = _portfolioSummary.value
        val current = _parameters.value
        updateParameters(
            current.copy(
                currentSavings = summary.netWorth.coerceAtLeast(0.0),
                monthlySip = summary.totalMonthlyContribution,
                expectedReturn = summary.weightedExpectedReturn.coerceIn(0.01, 0.25),
                taxRate = summary.weightedTaxRate.coerceIn(0.0, 0.50)
            )
        )
        _selectedTab.value = AppTab.FIRE
    }

    private fun recalculatePortfolio() {
        _portfolioSummary.value = InvestmentEngine.calculatePortfolio(
            _holdings.value,
            _portfolioProjectionYears.value,
            _liabilities.value
        )
        recalculateFiScore()
    }

    fun generateCsvContent(scope: CsvExportScope): String {
        val portfolioCsv = CsvExportService.buildPortfolioCsv(
            holdings = _holdings.value,
            liabilities = _liabilities.value,
            summary = _portfolioSummary.value,
            projectionYears = _portfolioProjectionYears.value,
            isIndianCurrency = _isIndianCurrency.value
        )
        val projectionsCsv = CsvExportService.buildSavingsProjectionsCsv(
            fireParams = _parameters.value,
            fireResult = _result.value,
            monteCarloResult = _monteCarloResult.value,
            sipParams = _sipParams.value,
            sipResult = _sipResult.value,
            swpParams = _swpParams.value,
            swpResult = _swpResult.value,
            fdParams = _fdParams.value,
            fdResult = _fdResult.value,
            goalParams = _goalParams.value,
            goalResult = _goalResult.value,
            inflationParams = _inflationParams.value,
            inflationResult = _inflationResult.value,
            isIndianCurrency = _isIndianCurrency.value,
            debtPayoffResult = _debtPayoffResult.value,
            taxParams = _taxParams.value,
            taxResult = _taxResult.value,
            retirementParams = _retirementPlanParams.value,
            retirementResult = _retirementPlanResult.value,
            recurringSummary = _cashflowSummary.value,
            recurringTransactions = _recurringTransactions.value,
            budgetSummary = _budgetSummary.value,
            fiScoreSummary = _fiScoreSummary.value
        )
        return when (scope) {
            CsvExportScope.PORTFOLIO -> portfolioCsv
            CsvExportScope.PROJECTIONS -> projectionsCsv
            CsvExportScope.FULL_BACKUP -> "$portfolioCsv\n$projectionsCsv"
        }
    }

    private fun persistHoldings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        val json = holdingListAdapter.toJson(_holdings.value)
        prefs.edit().putString(KEY_HOLDINGS, json).apply()
    }

    private fun persistLiabilities(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PORTFOLIO, Context.MODE_PRIVATE)
        val json = liabilityListAdapter.toJson(_liabilities.value)
        prefs.edit().putString(KEY_LIABILITIES, json).apply()
    }

    // Currency formatting
    fun formatCurrency(amount: Double, forceIndian: Boolean = _isIndianCurrency.value): String {
        val formatter = if (forceIndian) {
            NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        } else {
            NumberFormat.getCurrencyInstance(Locale.US)
        }
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    fun formatShortCurrency(amount: Double, forceIndian: Boolean = _isIndianCurrency.value): String {
        val sign = if (amount < 0) "-" else ""
        val value = abs(amount)
        val symbol = if (forceIndian) "₹" else "$"

        return if (forceIndian) {
            when {
                value >= 1_00_00_000.0 -> "$sign$symbol${String.format(Locale.US, "%.2f", value / 1_00_00_000.0)} Cr"
                value >= 1_00_000.0 -> "$sign$symbol${String.format(Locale.US, "%.2f", value / 1_00_000.0)} L"
                else -> {
                    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
                    fmt.format(amount)
                }
            }
        } else {
            when {
                value >= 1_000_000.0 -> "$sign$symbol${String.format(Locale.US, "%.2f", value / 1_000_000.0)}M"
                value >= 1_000.0 -> "$sign$symbol${String.format(Locale.US, "%.1f", value / 1_000.0)}k"
                else -> {
                    val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
                    fmt.format(amount)
                }
            }
        }
    }

    // Profiles persistence
    fun refreshSavedProfiles(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
        _savedProfileNames.value = prefs.all.keys.sorted()
    }

    fun saveProfile(context: Context, profileName: String) {
        val cleanName = profileName.trim().ifEmpty { "Scenario ${_savedProfileNames.value.size + 1}" }
        val prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
        val json = moshi.adapter(FireParameters::class.java).toJson(_parameters.value)
        prefs.edit().putString(cleanName, json).apply()
        refreshSavedProfiles(context)
    }

    fun loadProfile(context: Context, profileName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
        val json = prefs.getString(profileName, null) ?: return false
        return try {
            val params = moshi.adapter(FireParameters::class.java).fromJson(json)
            if (params != null) {
                updateParameters(params)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun deleteProfile(context: Context, profileName: String) {
        val prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
        prefs.edit().remove(profileName).apply()
        refreshSavedProfiles(context)
    }

    fun getSavedProfiles(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE)
        return prefs.all.keys.sorted()
    }

    private fun defaultHoldings(isIndian: Boolean): List<AssetHolding> {
        val m = if (isIndian) 25.0 else 1.0
        return listOf(
            AssetHolding(
                id = UUID.randomUUID().toString(),
                name = "Core Equity Mutual Fund",
                assetType = AssetType.MUTUAL_FUND,
                currentValue = 45_000.0 * m,
                monthlyContribution = 1_100.0 * m,
                expectedReturn = 0.115,
                taxRate = 0.125,
                symbol = "MF-EQ",
                quantity = 300.0,
                avgBuyPrice = 125.0 * m,
                currentMarketPrice = 150.0 * m,
                investedCost = 37_500.0 * m
            ),
            AssetHolding(
                id = UUID.randomUUID().toString(),
                name = "Direct Equities / Index ETF",
                assetType = AssetType.EQUITY,
                currentValue = 30_000.0 * m,
                monthlyContribution = 500.0 * m,
                expectedReturn = 0.12,
                taxRate = 0.125,
                symbol = "VTI",
                quantity = 100.0,
                avgBuyPrice = 240.0 * m,
                currentMarketPrice = 300.0 * m,
                investedCost = 24_000.0 * m
            ),
            AssetHolding(
                id = UUID.randomUUID().toString(),
                name = "Bank Fixed Deposit",
                assetType = AssetType.FIXED_DEPOSIT,
                currentValue = 20_000.0 * m,
                monthlyContribution = 250.0 * m,
                expectedReturn = 0.07,
                taxRate = 0.25,
                symbol = "FD",
                investedCost = 18_800.0 * m
            ),
            AssetHolding(
                id = UUID.randomUUID().toString(),
                name = "Treasury & Corporate Bonds",
                assetType = AssetType.BONDS,
                currentValue = 15_000.0 * m,
                monthlyContribution = 200.0 * m,
                expectedReturn = 0.075,
                taxRate = 0.20,
                symbol = "AGG",
                quantity = 150.0,
                avgBuyPrice = 98.0 * m,
                currentMarketPrice = 100.0 * m,
                investedCost = 14_700.0 * m
            )
        )
    }

    private fun defaultDebtAccounts(isIndian: Boolean): List<DebtAccount> {
        val m = if (isIndian) 25.0 else 1.0
        return listOf(
            DebtAccount(
                id = UUID.randomUUID().toString(),
                name = "Rewards Credit Card",
                category = "Credit Card",
                balance = 6_500.0 * m,
                annualApr = 0.219,
                minimumPayment = 195.0 * m
            ),
            DebtAccount(
                id = UUID.randomUUID().toString(),
                name = "Auto Loan",
                category = "Auto Loan",
                balance = 14_000.0 * m,
                annualApr = 0.068,
                minimumPayment = 320.0 * m
            ),
            DebtAccount(
                id = UUID.randomUUID().toString(),
                name = "Student / Personal Loan",
                category = "Personal Loan",
                balance = 4_200.0 * m,
                annualApr = 0.095,
                minimumPayment = 135.0 * m
            )
        )
    }

    private fun defaultRecurringTransactions(isIndian: Boolean): List<RecurringTransaction> {
        val m = if (isIndian) 25.0 else 1.0
        val currentCycle = RecurringTransactionEngine.currentYearMonth()
        return listOf(
            RecurringTransaction(
                id = UUID.randomUUID().toString(),
                title = "Monthly Salary",
                amount = 7_500.0 * m,
                type = RecurringType.INCOME,
                category = "Salary",
                dayOfMonth = 1,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.GENERAL,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = currentCycle
            ),
            RecurringTransaction(
                id = UUID.randomUUID().toString(),
                title = "Core Equity Auto-SIP",
                amount = 1_100.0 * m,
                type = RecurringType.EXPENSE,
                category = "Investment SIP",
                dayOfMonth = 5,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.PORTFOLIO_SIP,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = currentCycle
            ),
            RecurringTransaction(
                id = UUID.randomUUID().toString(),
                title = "Living & Housing Expenses",
                amount = 3_600.0 * m,
                type = RecurringType.EXPENSE,
                category = "Housing",
                dayOfMonth = 3,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.GENERAL,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = currentCycle
            ),
            RecurringTransaction(
                id = UUID.randomUUID().toString(),
                title = "Extra Debt Accelerator",
                amount = 350.0 * m,
                type = RecurringType.EXPENSE,
                category = "Debt Payment",
                dayOfMonth = 10,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.DEBT_PAYMENT,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = currentCycle
            )
        )
    }

    private fun defaultBudgetCategories(isIndian: Boolean): List<BudgetCategoryEntry> {
        val m = if (isIndian) 25.0 else 1.0
        return listOf(
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Housing",
                monthlyLimit = 3_800.0 * m,
                manualSpending = 0.0,
                savingTarget = 200.0 * m,
                isEssential = true
            ),
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Food & Dining",
                monthlyLimit = 750.0 * m,
                manualSpending = 480.0 * m,
                savingTarget = 150.0 * m,
                isEssential = true
            ),
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Utilities",
                monthlyLimit = 300.0 * m,
                manualSpending = 190.0 * m,
                savingTarget = 50.0 * m,
                isEssential = true
            ),
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Debt Payment",
                monthlyLimit = 450.0 * m,
                manualSpending = 0.0,
                savingTarget = 100.0 * m,
                isEssential = true
            ),
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Investment SIP",
                monthlyLimit = 1_200.0 * m,
                manualSpending = 0.0,
                savingTarget = 100.0 * m,
                isEssential = false
            ),
            BudgetCategoryEntry(
                id = UUID.randomUUID().toString(),
                name = "Subscription",
                monthlyLimit = 250.0 * m,
                manualSpending = 135.0 * m,
                savingTarget = 75.0 * m,
                isEssential = false
            )
        )
    }

    companion object {
        private const val PREFS_PROFILES = "FireProfiles"
        private const val PREFS_PORTFOLIO = "AegisPortfolio"
        private const val KEY_HOLDINGS = "holdings_json"
        private const val KEY_LIABILITIES = "liabilities_json"
        private const val KEY_DEBTS = "debt_accounts_json"
        private const val KEY_RECURRING = "recurring_transactions_json"
        private const val KEY_CASHFLOW_LOGS = "cashflow_logs_json"
        private const val KEY_BUDGET_CATEGORIES = "budget_categories_json"
        private const val KEY_BUDGET_USE_RECURRING_INCOME = "budget_use_recurring_income"
        private const val KEY_BUDGET_CUSTOM_INCOME = "budget_custom_income"
        private const val KEY_PEAK_DEBT_BALANCE = "peak_debt_balance"
        private const val KEY_BOOKMARKED_TIPS = "bookmarked_daily_tips"
        private const val KEY_PRO_UNLOCKED = "aegis_pro_unlocked"
    }
}
