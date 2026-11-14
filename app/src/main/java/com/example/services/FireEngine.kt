package com.example.services

import com.example.models.FireParameters
import com.example.models.FireResult
import com.example.models.MonteCarloResult
import com.example.models.TrajectoryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object FireEngine {

    fun calculateFire(params: FireParameters): FireResult {
        val nYears = params.targetAge - params.currentAge
        if (nYears <= 0) {
            return FireResult(0.0, 0.0, 0.0, 0.0, emptyList(), null, 0.0, null)
        }

        val r = params.expectedReturn
        val s = params.stepUpRate
        val p0 = params.currentSavings
        val c0 = params.monthlySip
        val i = params.inflationRate
        val tau = params.taxRate
        val swr = if (params.safeWithdrawalRate > 0) params.safeWithdrawalRate else 0.04

        val rm = if (r > 0) (1.0 + r).pow(1.0 / 12.0) - 1.0 else 0.0

        val eRet = params.monthlyExpense * 12.0 * (1.0 + i).pow(nYears.toDouble())
        val kTarget = eRet / swr
        val coastFire = if (r > 0) kTarget / (1.0 + r).pow(nYears.toDouble()) else kTarget

        var totalInvested = p0
        var currentGross = p0
        val trajectory = mutableListOf<TrajectoryPoint>()
        var crossoverAge: Int? = if (currentGross >= kTarget) params.currentAge else null

        trajectory.add(TrajectoryPoint(params.currentAge, currentGross, currentGross, kTarget))

        for (k in 1..nYears) {
            val ck = c0 * (1.0 + s).pow(k - 1.0)
            totalInvested += ck * 12.0

            if (r == 0.0) {
                currentGross += ck * 12.0
            } else {
                currentGross *= (1.0 + r)
                var yearContributions = 0.0
                for (m in 1..12) {
                    val monthsRemaining = 12 - m
                    yearContributions += ck * (1.0 + rm).pow(monthsRemaining.toDouble())
                }
                currentGross += yearContributions
            }

            val netBalance = totalInvested + (currentGross - totalInvested) * (1.0 - tau)
            val ageAtK = params.currentAge + k
            if (crossoverAge == null && netBalance >= kTarget) {
                crossoverAge = ageAtK
            }
            trajectory.add(TrajectoryPoint(ageAtK, currentGross, netBalance, kTarget))
        }

        val fvGross = currentGross
        val netLiquidated = totalInvested + (fvGross - totalInvested) * (1.0 - tau)
        val taxDrag = (fvGross - netLiquidated).coerceAtLeast(0.0)

        var wt = netLiquidated
        var depletionYear: Int? = null
        val rPost = r

        for (t in 1..50) {
            wt = wt * (1.0 + rPost) - eRet * (1.0 + i).pow(t.toDouble())
            if (wt <= 0 && depletionYear == null) {
                depletionYear = params.targetAge + t
                break
            }
        }

        return FireResult(
            targetFireNumber = kTarget,
            grossAccumulated = fvGross,
            taxDrag = taxDrag,
            netAccumulated = netLiquidated,
            trajectory = trajectory,
            depletionYear = depletionYear,
            coastFireNumber = coastFire,
            fireCrossoverAge = crossoverAge
        )
    }

    suspend fun runMonteCarloSimulation(
        params: FireParameters,
        startBalance: Double,
        random: Random = Random.Default
    ): MonteCarloResult = withContext(Dispatchers.Default) {
        val iterations = 1000
        val years = 40
        val mu = params.expectedReturn
        val sigma = 0.15
        val nYears = (params.targetAge - params.currentAge).coerceAtLeast(0)

        val eRet = params.monthlyExpense * 12.0 * (1.0 + params.inflationRate).pow(nYears.toDouble())

        var success30 = 0
        var success40 = 0

        val allPaths = Array(iterations) { DoubleArray(years + 1) }

        for (iter in 0 until iterations) {
            var wt = startBalance
            allPaths[iter][0] = wt
            for (t in 1..years) {
                val u1 = random.nextDouble().coerceIn(1e-9, 1.0)
                val u2 = random.nextDouble()
                val z = sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
                val rt = mu + sigma * z

                wt = (wt * (1.0 + rt) - eRet * (1.0 + params.inflationRate).pow(t.toDouble())).coerceAtLeast(0.0)
                allPaths[iter][t] = wt
            }
            if (allPaths[iter][30] > 0.0) success30++
            if (allPaths[iter][40] > 0.0) success40++
        }

        val longevity30 = (success30.toDouble() / iterations) * 100.0
        val longevity40 = (success40.toDouble() / iterations) * 100.0

        val p10 = DoubleArray(years + 1)
        val p50 = DoubleArray(years + 1)
        val p90 = DoubleArray(years + 1)

        for (t in 0..years) {
            val sortedAtT = DoubleArray(iterations) { allPaths[it][t] }
            sortedAtT.sort()
            p10[t] = sortedAtT[(iterations * 0.10).toInt()]
            p50[t] = sortedAtT[(iterations * 0.50).toInt()]
            p90[t] = sortedAtT[(iterations * 0.90).toInt()]
        }

        MonteCarloResult(
            longevityProbability = longevity30,
            longevity40Probability = longevity40,
            percentile10 = p10.toList(),
            percentile50 = p50.toList(),
            percentile90 = p90.toList()
        )
    }
}
