package com.example.services

import com.example.models.AssetAllocationSlice
import com.example.models.AssetHolding
import com.example.models.AssetType
import com.example.models.FdParams
import com.example.models.FdResult
import com.example.models.GoalParams
import com.example.models.GoalResult
import com.example.models.InflationImpactParams
import com.example.models.InflationImpactResult
import com.example.models.LiabilityItem
import com.example.models.PortfolioSummary
import com.example.models.SipParams
import com.example.models.SipResult
import com.example.models.SipYearPoint
import com.example.models.SwpParams
import com.example.models.SwpResult
import kotlin.math.pow

object InvestmentEngine {

    fun calculateSip(params: SipParams): SipResult {
        val years = params.years.coerceAtLeast(1)
        val r = params.expectedReturn.coerceAtLeast(0.0)
        val s = params.annualStepUpRate.coerceAtLeast(0.0)
        val i = params.inflationRate.coerceAtLeast(0.0)
        val rm = if (r > 0) (1.0 + r).pow(1.0 / 12.0) - 1.0 else 0.0

        var totalInvested = params.lumpsum.coerceAtLeast(0.0)
        var currentValue = totalInvested
        val timeline = mutableListOf<SipYearPoint>()
        timeline.add(SipYearPoint(0, totalInvested, currentValue))

        for (k in 1..years) {
            val monthlyC = params.monthlySip.coerceAtLeast(0.0) * (1.0 + s).pow(k - 1.0)
            totalInvested += monthlyC * 12.0

            if (r == 0.0) {
                currentValue += monthlyC * 12.0
            } else {
                currentValue *= (1.0 + r)
                var yearContrib = 0.0
                for (m in 1..12) {
                    val monthsRemaining = 12 - m
                    yearContrib += monthlyC * (1.0 + rm).pow(monthsRemaining.toDouble())
                }
                currentValue += yearContrib
            }
            timeline.add(SipYearPoint(k, totalInvested, currentValue))
        }

        val wealthGain = (currentValue - totalInvested).coerceAtLeast(0.0)
        val inflationAdjusted = currentValue / (1.0 + i).pow(years.toDouble())

        return SipResult(
            totalInvested = totalInvested,
            wealthGain = wealthGain,
            maturityValue = currentValue,
            inflationAdjustedValue = inflationAdjusted,
            timeline = timeline
        )
    }

    fun calculateSwp(params: SwpParams): SwpResult {
        val years = params.years.coerceAtLeast(1)
        val totalMonths = years * 12
        val r = params.expectedReturn.coerceAtLeast(0.0)
        val rm = if (r > 0) (1.0 + r).pow(1.0 / 12.0) - 1.0 else 0.0
        val stepUp = params.annualWithdrawalStepUp.coerceAtLeast(0.0)

        var balance = params.initialCorpus.coerceAtLeast(0.0)
        var totalWithdrawn = 0.0
        var monthsLasted = 0
        var depletedYear: Int? = null
        val yearlyBalances = mutableListOf(balance)

        for (m in 1..totalMonths) {
            val yearIndex = (m - 1) / 12
            val currentWithdrawal = params.monthlyWithdrawal.coerceAtLeast(0.0) * (1.0 + stepUp).pow(yearIndex.toDouble())

            if (balance > 0.0) {
                balance *= (1.0 + rm)
                val actualDrawn = minOf(balance, currentWithdrawal)
                balance = (balance - actualDrawn).coerceAtLeast(0.0)
                totalWithdrawn += actualDrawn
                if (balance > 0.0) {
                    monthsLasted = m
                } else if (depletedYear == null) {
                    monthsLasted = m
                    depletedYear = ((m - 1) / 12) + 1
                }
            }

            if (m % 12 == 0) {
                yearlyBalances.add(balance)
            }
        }

        return SwpResult(
            initialCorpus = params.initialCorpus,
            totalWithdrawn = totalWithdrawn,
            finalBalance = balance,
            monthsLasted = monthsLasted,
            depletedYear = depletedYear,
            yearlyBalances = yearlyBalances
        )
    }

    fun calculateFd(params: FdParams): FdResult {
        val principal = params.principal.coerceAtLeast(0.0)
        val r = params.annualRate.coerceAtLeast(0.0)
        val years = params.years.coerceAtLeast(1)
        val n = params.compounding.periodsPerYear.coerceAtLeast(1)
        val tau = params.taxRate.coerceIn(0.0, 0.99)

        val grossMaturity = principal * (1.0 + r / n).pow((n * years).toDouble())
        val grossInterest = (grossMaturity - principal).coerceAtLeast(0.0)
        val taxDeducted = grossInterest * tau
        val postTaxMaturity = principal + (grossInterest - taxDeducted)

        val effectivePostTaxYield = if (principal > 0.0) {
            (postTaxMaturity / principal).pow(1.0 / years.toDouble()) - 1.0
        } else {
            0.0
        }

        return FdResult(
            principal = principal,
            grossMaturity = grossMaturity,
            grossInterest = grossInterest,
            taxDeducted = taxDeducted,
            postTaxMaturity = postTaxMaturity,
            effectivePostTaxYield = effectivePostTaxYield
        )
    }

    fun calculateGoal(params: GoalParams): GoalResult {
        val years = params.years.coerceAtLeast(1)
        val r = params.expectedReturn.coerceAtLeast(0.0)
        val i = params.inflationRate.coerceAtLeast(0.0)

        val targetAdjusted = if (params.adjustForInflation) {
            params.targetGoal.coerceAtLeast(0.0) * (1.0 + i).pow(years.toDouble())
        } else {
            params.targetGoal.coerceAtLeast(0.0)
        }

        val fvCurrent = params.currentSavings.coerceAtLeast(0.0) * (1.0 + r).pow(years.toDouble())
        val shortfall = (targetAdjusted - fvCurrent).coerceAtLeast(0.0)

        val flatSip = if (shortfall <= 0.0) {
            0.0
        } else {
            val unitSipFv = calculateSip(
                SipParams(
                    lumpsum = 0.0,
                    monthlySip = 1.0,
                    annualStepUpRate = 0.0,
                    expectedReturn = r,
                    years = years,
                    inflationRate = 0.0
                )
            ).maturityValue
            if (unitSipFv > 0.0) shortfall / unitSipFv else 0.0
        }

        val stepUp10Sip = if (shortfall <= 0.0) {
            0.0
        } else {
            val unitStepUpFv = calculateSip(
                SipParams(
                    lumpsum = 0.0,
                    monthlySip = 1.0,
                    annualStepUpRate = 0.10,
                    expectedReturn = r,
                    years = years,
                    inflationRate = 0.0
                )
            ).maturityValue
            if (unitStepUpFv > 0.0) shortfall / unitStepUpFv else 0.0
        }

        return GoalResult(
            inflationAdjustedGoal = targetAdjusted,
            futureValueOfCurrentSavings = fvCurrent,
            shortfallToCover = shortfall,
            requiredMonthlySipFlat = flatSip,
            requiredMonthlySipStepUp10 = stepUp10Sip
        )
    }

    fun calculatePortfolio(
        holdings: List<AssetHolding>,
        projectionYears: Int = 10,
        liabilities: List<LiabilityItem> = emptyList()
    ): PortfolioSummary {
        val years = projectionYears.coerceAtLeast(1)
        val totalDebt = liabilities.sumOf { it.balance.coerceAtLeast(0.0) }
        if (holdings.isEmpty()) {
            return PortfolioSummary(
                totalCurrentValue = 0.0,
                totalMonthlyContribution = 0.0,
                weightedExpectedReturn = 0.08,
                weightedTaxRate = 0.15,
                projectedGross10Yr = 0.0,
                projectedNet10Yr = 0.0,
                projectedTaxDrag10Yr = 0.0,
                allocationSlices = emptyList(),
                yearlyNetTrajectory = List(years + 1) { 0.0 },
                totalInvestedCost = 0.0,
                totalUnrealizedPnL = 0.0,
                totalUnrealizedPnLPercent = 0.0,
                totalLiabilities = totalDebt,
                netWorth = -totalDebt
            )
        }

        val totalCurrent = holdings.sumOf { it.effectiveMarketValue.coerceAtLeast(0.0) }
        val totalCostBasis = holdings.sumOf { it.effectiveCostBasis.coerceAtLeast(0.0) }
        val totalPnL = totalCurrent - totalCostBasis
        val totalPnLPct = if (totalCostBasis > 0.0) (totalPnL / totalCostBasis) * 100.0 else 0.0
        val totalMonthly = holdings.sumOf { it.monthlyContribution.coerceAtLeast(0.0) }

        val weights = holdings.map { h ->
            if (totalCurrent > 0.0) h.effectiveMarketValue.coerceAtLeast(0.0)
            else h.monthlyContribution.coerceAtLeast(0.0)
        }
        val totalWeight = weights.sum()

        val weightedReturn = if (totalWeight > 0.0) {
            holdings.zip(weights).sumOf { (h, w) -> h.expectedReturn * w } / totalWeight
        } else {
            holdings.map { it.expectedReturn }.average()
        }

        val weightedTax = if (totalWeight > 0.0) {
            holdings.zip(weights).sumOf { (h, w) -> h.taxRate * w } / totalWeight
        } else {
            holdings.map { it.taxRate }.average()
        }

        var grossSum = 0.0
        var netSum = 0.0
        val yearlyNet = DoubleArray(years + 1) { 0.0 }

        holdings.forEach { holding ->
            val r = holding.expectedReturn.coerceAtLeast(0.0)
            val tau = holding.taxRate.coerceIn(0.0, 0.99)
            val rm = if (r > 0) (1.0 + r).pow(1.0 / 12.0) - 1.0 else 0.0

            var invested = holding.effectiveMarketValue.coerceAtLeast(0.0)
            var gross = invested
            yearlyNet[0] += invested

            for (y in 1..years) {
                val monthly = holding.monthlyContribution.coerceAtLeast(0.0)
                invested += monthly * 12.0
                if (r == 0.0) {
                    gross += monthly * 12.0
                } else {
                    gross *= (1.0 + r)
                    var contrib = 0.0
                    for (m in 1..12) {
                        contrib += monthly * (1.0 + rm).pow((12 - m).toDouble())
                    }
                    gross += contrib
                }
                val netAtY = invested + (gross - invested) * (1.0 - tau)
                yearlyNet[y] += netAtY
            }

            grossSum += gross
            val netFinal = invested + (gross - invested) * (1.0 - tau)
            netSum += netFinal
        }

        val slices = AssetType.entries.mapNotNull { type ->
            val typeMarketSum = holdings.filter { it.assetType == type }.sumOf { it.effectiveMarketValue.coerceAtLeast(0.0) }
            val typeWeightSum = holdings.filter { it.assetType == type }.sumOf {
                if (totalCurrent > 0.0) it.effectiveMarketValue.coerceAtLeast(0.0) else it.monthlyContribution.coerceAtLeast(0.0)
            }
            if (typeWeightSum > 0.0 && totalWeight > 0.0) {
                AssetAllocationSlice(
                    assetType = type,
                    currentValue = typeMarketSum,
                    percentage = (typeWeightSum / totalWeight) * 100.0
                )
            } else {
                null
            }
        }

        return PortfolioSummary(
            totalCurrentValue = totalCurrent,
            totalMonthlyContribution = totalMonthly,
            weightedExpectedReturn = weightedReturn,
            weightedTaxRate = weightedTax,
            projectedGross10Yr = grossSum,
            projectedNet10Yr = netSum,
            projectedTaxDrag10Yr = (grossSum - netSum).coerceAtLeast(0.0),
            allocationSlices = slices,
            yearlyNetTrajectory = yearlyNet.toList(),
            totalInvestedCost = totalCostBasis,
            totalUnrealizedPnL = totalPnL,
            totalUnrealizedPnLPercent = totalPnLPct,
            totalLiabilities = totalDebt,
            netWorth = totalCurrent - totalDebt
        )
    }

    fun calculateInflationImpact(params: InflationImpactParams): InflationImpactResult {
        val goalToday = params.goalAmountToday.coerceAtLeast(0.0)
        val inflation = params.annualInflationRate.coerceIn(0.0, 0.50)
        val years = params.years.coerceAtLeast(1)
        val nominalYield = params.expectedSavingsYield.coerceAtLeast(0.0)

        val inflatedCostCurve = mutableListOf<Double>()
        val purchasingPowerCurve = mutableListOf<Double>()

        for (y in 0..years) {
            val factor = (1.0 + inflation).pow(y.toDouble())
            inflatedCostCurve.add(goalToday * factor)
            purchasingPowerCurve.add(if (factor > 0.0) goalToday / factor else goalToday)
        }

        val futureCost = inflatedCostCurve.last()
        val uninvestedPower = purchasingPowerCurve.last()
        val costIncrease = (futureCost - goalToday).coerceAtLeast(0.0)
        val multiplier = if (goalToday > 0.0) futureCost / goalToday else 1.0
        val lostPercent = if (goalToday > 0.0) ((goalToday - uninvestedPower) / goalToday) * 100.0 else 0.0

        val realReturn = ((1.0 + nominalYield) / (1.0 + inflation)) - 1.0
        val investedRealVal = goalToday * (1.0 + realReturn).pow(years.toDouble())

        return InflationImpactResult(
            goalAmountToday = goalToday,
            futureInflatedCost = futureCost,
            inflationCostIncrease = costIncrease,
            costMultiplier = multiplier,
            uninvestedPurchasingPower = uninvestedPower,
            purchasingPowerLostPercent = lostPercent,
            realAnnualReturn = realReturn,
            investedRealValue = investedRealVal,
            inflatedCostTimeline = inflatedCostCurve,
            purchasingPowerTimeline = purchasingPowerCurve
        )
    }
}
