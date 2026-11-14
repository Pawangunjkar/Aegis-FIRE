package com.example.services

import com.example.models.RetirementPlanParams
import com.example.models.RetirementPlanPoint
import com.example.models.RetirementPlanResult
import kotlin.math.pow

object RetirementEngine {

    fun calculateRetirementPlan(params: RetirementPlanParams): RetirementPlanResult {
        val currentAge = params.currentAge.coerceIn(18, 85)
        val retirementAge = params.retirementAge.coerceIn(currentAge + 1, 95)
        val lifeExpectancy = params.lifeExpectancy.coerceIn(retirementAge + 1, 110)

        val yearsToRetirement = retirementAge - currentAge
        val yearsInRetirement = lifeExpectancy - retirementAge

        val monthlyExpenseToday = params.targetMonthlyExpensesToday.coerceAtLeast(0.0)
        val inflation = params.annualInflationRate.coerceIn(0.0, 0.30)
        val preReturn = params.preRetirementReturn.coerceIn(0.0, 0.30)
        val postReturn = params.postRetirementReturn.coerceIn(0.0, 0.25)
        val currentSavings = params.currentRetirementSavings.coerceAtLeast(0.0)

        val monthlyInflation = (1.0 + inflation).pow(1.0 / 12.0) - 1.0
        val monthlyPreReturn = (1.0 + preReturn).pow(1.0 / 12.0) - 1.0
        val monthlyPostReturn = (1.0 + postReturn).pow(1.0 / 12.0) - 1.0

        val monthlyExpenseAtRetirement = monthlyExpenseToday * (1.0 + inflation).pow(yearsToRetirement.toDouble())
        val finalMonthlyExpenseAtLifeExpectancy =
            monthlyExpenseToday * (1.0 + inflation).pow((yearsToRetirement + yearsInRetirement).toDouble())

        // Exact present value of inflation-growing retirement expenses (annuity-due, monthly compounding)
        val retirementMonths = yearsInRetirement * 12
        var requiredCorpus = 0.0
        var totalNominalPayout = 0.0
        for (m in 0 until retirementMonths) {
            val withdrawal = monthlyExpenseAtRetirement * (1.0 + monthlyInflation).pow(m.toDouble())
            totalNominalPayout += withdrawal
            val discountFactor = (1.0 + monthlyPostReturn).pow(m.toDouble())
            requiredCorpus += if (discountFactor > 0.0) withdrawal / discountFactor else withdrawal
        }

        // Pre-retirement growth of existing savings
        val accumulationMonths = yearsToRetirement * 12
        val projectedCurrentSavings = currentSavings * (1.0 + monthlyPreReturn).pow(accumulationMonths.toDouble())
        val shortfall = (requiredCorpus - projectedCurrentSavings).coerceAtLeast(0.0)

        // End-of-month SIP required to accumulate the shortfall by retirement age
        val requiredMonthlySip = when {
            shortfall <= 0.0 -> 0.0
            monthlyPreReturn <= 1e-9 -> shortfall / accumulationMonths.toDouble()
            else -> {
                val factor = (1.0 + monthlyPreReturn).pow(accumulationMonths.toDouble()) - 1.0
                if (factor > 0.0) shortfall * monthlyPreReturn / factor else shortfall / accumulationMonths.toDouble()
            }
        }

        // Build full year-by-year lifecycle trajectory from currentAge to lifeExpectancy
        val trajectory = mutableListOf<RetirementPlanPoint>()
        var balance = currentSavings
        var annualExpenseNominal = monthlyExpenseToday * 12.0

        trajectory.add(
            RetirementPlanPoint(
                age = currentAge,
                projectedCorpus = balance,
                targetCorpusBenchmark = requiredCorpus,
                annualExpenseNominal = annualExpenseNominal
            )
        )

        // Accumulation phase
        for (year in 1..yearsToRetirement) {
            for (m in 1..12) {
                balance = balance * (1.0 + monthlyPreReturn) + requiredMonthlySip
            }
            annualExpenseNominal = (monthlyExpenseToday * 12.0) * (1.0 + inflation).pow(year.toDouble())
            trajectory.add(
                RetirementPlanPoint(
                    age = currentAge + year,
                    projectedCorpus = balance,
                    targetCorpusBenchmark = requiredCorpus,
                    annualExpenseNominal = annualExpenseNominal
                )
            )
        }

        // Drawdown phase
        var retMonthIdx = 0
        for (year in 1..yearsInRetirement) {
            var yearlyOutflow = 0.0
            for (m in 1..12) {
                val monthlyDraw = monthlyExpenseAtRetirement * (1.0 + monthlyInflation).pow(retMonthIdx.toDouble())
                yearlyOutflow += monthlyDraw
                balance = ((balance - monthlyDraw) * (1.0 + monthlyPostReturn)).coerceAtLeast(0.0)
                retMonthIdx++
            }
            trajectory.add(
                RetirementPlanPoint(
                    age = retirementAge + year,
                    projectedCorpus = balance,
                    targetCorpusBenchmark = requiredCorpus,
                    annualExpenseNominal = yearlyOutflow
                )
            )
        }

        val realPostReturn = ((1.0 + postReturn) / (1.0 + inflation)) - 1.0

        return RetirementPlanResult(
            currentAge = currentAge,
            retirementAge = retirementAge,
            lifeExpectancy = lifeExpectancy,
            yearsToRetirement = yearsToRetirement,
            yearsInRetirement = yearsInRetirement,
            monthlyExpenseAtRetirement = monthlyExpenseAtRetirement,
            finalMonthlyExpenseAtLifeExpectancy = finalMonthlyExpenseAtLifeExpectancy,
            requiredCorpusAtRetirement = requiredCorpus,
            projectedCurrentSavingsAtRetirement = projectedCurrentSavings,
            corpusShortfall = shortfall,
            requiredMonthlySip = requiredMonthlySip,
            totalRetirementPayoutNominal = totalNominalPayout,
            realPostRetirementReturn = realPostReturn,
            lifecycleTrajectory = trajectory
        )
    }
}
