package com.example.services

import com.example.models.TaxBracketSlice
import com.example.models.TaxParams
import com.example.models.TaxRegime
import com.example.models.TaxResult

object TaxEngine {

    private data class Slab(val label: String, val upperLimit: Double, val rate: Double)

    fun calculateTax(params: TaxParams): TaxResult {
        val gross = params.annualIncome.coerceAtLeast(0.0)
        val capGains = params.realizedCapitalGains.coerceAtLeast(0.0)

        val coreWithDeductions = computeCoreTax(params, includeInvestmentDeductions = true)
        val coreWithoutDeductions = computeCoreTax(params, includeInvestmentDeductions = false)
        val taxSaved = (coreWithoutDeductions.totalTax - coreWithDeductions.totalTax).coerceAtLeast(0.0)

        val totalEarnings = gross + capGains
        val effectiveRate = if (totalEarnings > 0.0) {
            (coreWithDeductions.totalTax / totalEarnings).coerceIn(0.0, 0.99)
        } else {
            0.0
        }

        return TaxResult(
            grossIncome = gross,
            standardDeductionUsed = coreWithDeductions.standardDeduction,
            investmentDeductionsUsed = coreWithDeductions.investmentDeduction,
            totalDeductions = coreWithDeductions.standardDeduction + coreWithDeductions.investmentDeduction,
            taxableIncome = coreWithDeductions.taxableIncome,
            ordinaryIncomeTax = coreWithDeductions.ordinaryTax,
            capitalGainsTax = coreWithDeductions.capitalGainsTax,
            cessOrSurtax = coreWithDeductions.cessOrSurtax,
            totalTaxLiability = coreWithDeductions.totalTax,
            netTakeHomeIncome = (totalEarnings - coreWithDeductions.totalTax).coerceAtLeast(0.0),
            effectiveTaxRate = effectiveRate,
            marginalBracketRate = coreWithDeductions.marginalRate,
            taxSavedFromDeductions = taxSaved,
            bracketBreakdown = coreWithDeductions.breakdown
        )
    }

    private data class CoreTaxOutcome(
        val standardDeduction: Double,
        val investmentDeduction: Double,
        val taxableIncome: Double,
        val ordinaryTax: Double,
        val capitalGainsTax: Double,
        val cessOrSurtax: Double,
        val totalTax: Double,
        val marginalRate: Double,
        val breakdown: List<TaxBracketSlice>
    )

    private fun computeCoreTax(params: TaxParams, includeInvestmentDeductions: Boolean): CoreTaxOutcome {
        val gross = params.annualIncome.coerceAtLeast(0.0)
        val capGains = params.realizedCapitalGains.coerceAtLeast(0.0)

        val standardDeduction = if (!params.applyStandardDeduction) {
            0.0
        } else {
            when (params.regime) {
                TaxRegime.US_SINGLE -> 14_600.0
                TaxRegime.US_JOINT -> 29_200.0
                TaxRegime.IN_OLD -> 50_000.0
                TaxRegime.IN_NEW -> 75_000.0
            }
        }

        val rawInvestDeduction = if (!includeInvestmentDeductions) {
            0.0
        } else {
            when (params.regime) {
                TaxRegime.US_SINGLE, TaxRegime.US_JOINT -> {
                    val retCap = params.retirementDeduction.coerceIn(0.0, 30_500.0)
                    val hsaCap = params.additionalInvestmentDeduction.coerceIn(0.0, 12_000.0)
                    retCap + hsaCap
                }
                TaxRegime.IN_OLD -> {
                    val sec80C = params.retirementDeduction.coerceIn(0.0, 150_000.0)
                    val npsAndHealth = params.additionalInvestmentDeduction.coerceIn(0.0, 150_000.0)
                    sec80C + npsAndHealth
                }
                TaxRegime.IN_NEW -> {
                    // New India regime disallows 80C/80D personal deductions (employer NPS 80CCD(2) only)
                    0.0
                }
            }
        }

        val clampedStandard = minOf(gross, standardDeduction)
        val clampedInvest = minOf((gross - clampedStandard).coerceAtLeast(0.0), rawInvestDeduction)
        val taxableIncome = (gross - clampedStandard - clampedInvest).coerceAtLeast(0.0)

        val slabs = slabsForRegime(params.regime)
        val (ordinaryBeforeRebate, marginalRate, breakdown) = evaluateSlabs(taxableIncome, slabs)

        val ordinaryTax = applyRebate(params.regime, taxableIncome, ordinaryBeforeRebate)

        val capGainsTax = computeCapitalGainsTax(
            regime = params.regime,
            taxableOrdinaryIncome = taxableIncome,
            marginalRate = marginalRate,
            realizedGains = capGains,
            longTerm = params.longTermGains
        )

        val subTotal = ordinaryTax + capGainsTax
        val cess = if (params.regime.isIndian) subTotal * 0.04 else 0.0
        val total = subTotal + cess

        return CoreTaxOutcome(
            standardDeduction = clampedStandard,
            investmentDeduction = clampedInvest,
            taxableIncome = taxableIncome,
            ordinaryTax = ordinaryTax,
            capitalGainsTax = capGainsTax,
            cessOrSurtax = cess,
            totalTax = total,
            marginalRate = marginalRate,
            breakdown = breakdown
        )
    }

    private fun slabsForRegime(regime: TaxRegime): List<Slab> = when (regime) {
        TaxRegime.US_SINGLE -> listOf(
            Slab("10% ($0 – $11.6k)", 11_600.0, 0.10),
            Slab("12% ($11.6k – $47.15k)", 47_150.0, 0.12),
            Slab("22% ($47.15k – $100.5k)", 100_525.0, 0.22),
            Slab("24% ($100.5k – $191.9k)", 191_950.0, 0.24),
            Slab("32% ($191.9k – $243.7k)", 243_725.0, 0.32),
            Slab("35% ($243.7k – $609.3k)", 609_350.0, 0.35),
            Slab("37% (Over $609.3k)", Double.POSITIVE_INFINITY, 0.37)
        )
        TaxRegime.US_JOINT -> listOf(
            Slab("10% ($0 – $23.2k)", 23_200.0, 0.10),
            Slab("12% ($23.2k – $94.3k)", 94_300.0, 0.12),
            Slab("22% ($94.3k – $201.0k)", 201_050.0, 0.22),
            Slab("24% ($201.0k – $383.9k)", 383_900.0, 0.24),
            Slab("32% ($383.9k – $487.4k)", 487_450.0, 0.32),
            Slab("35% ($487.4k – $731.2k)", 731_200.0, 0.35),
            Slab("37% (Over $731.2k)", Double.POSITIVE_INFINITY, 0.37)
        )
        TaxRegime.IN_OLD -> listOf(
            Slab("0% (Up to ₹2.5L)", 250_000.0, 0.0),
            Slab("5% (₹2.5L – ₹5L)", 500_000.0, 0.05),
            Slab("20% (₹5L – ₹10L)", 1_000_000.0, 0.20),
            Slab("30% (Over ₹10L)", Double.POSITIVE_INFINITY, 0.30)
        )
        TaxRegime.IN_NEW -> listOf(
            Slab("0% (Up to ₹3L)", 300_000.0, 0.0),
            Slab("5% (₹3L – ₹7L)", 700_000.0, 0.05),
            Slab("10% (₹7L – ₹10L)", 1_000_000.0, 0.10),
            Slab("15% (₹10L – ₹12L)", 1_200_000.0, 0.15),
            Slab("20% (₹12L – ₹15L)", 1_500_000.0, 0.20),
            Slab("30% (Over ₹15L)", Double.POSITIVE_INFINITY, 0.30)
        )
    }

    private fun evaluateSlabs(taxableIncome: Double, slabs: List<Slab>): Triple<Double, Double, List<TaxBracketSlice>> {
        var prevCap = 0.0
        var totalTax = 0.0
        var marginal = slabs.firstOrNull()?.rate ?: 0.0
        val breakdown = mutableListOf<TaxBracketSlice>()

        for (slab in slabs) {
            if (taxableIncome <= prevCap) break
            val ceiling = if (slab.upperLimit.isInfinite()) taxableIncome else minOf(taxableIncome, slab.upperLimit)
            val sliceAmount = (ceiling - prevCap).coerceAtLeast(0.0)
            if (sliceAmount > 0.0) {
                val sliceTax = sliceAmount * slab.rate
                totalTax += sliceTax
                marginal = slab.rate
                breakdown.add(
                    TaxBracketSlice(
                        bracketLabel = slab.label,
                        rate = slab.rate,
                        taxableAmount = sliceAmount,
                        taxAmount = sliceTax
                    )
                )
            }
            prevCap = slab.upperLimit
        }

        return Triple(totalTax, marginal, breakdown)
    }

    private fun applyRebate(regime: TaxRegime, taxableIncome: Double, ordinaryTax: Double): Double = when (regime) {
        TaxRegime.IN_OLD -> if (taxableIncome <= 500_000.0) 0.0 else ordinaryTax
        TaxRegime.IN_NEW -> if (taxableIncome <= 700_000.0) 0.0 else ordinaryTax
        else -> ordinaryTax
    }

    private fun computeCapitalGainsTax(
        regime: TaxRegime,
        taxableOrdinaryIncome: Double,
        marginalRate: Double,
        realizedGains: Double,
        longTerm: Boolean
    ): Double {
        if (realizedGains <= 0.0) return 0.0
        return when (regime) {
            TaxRegime.US_SINGLE -> {
                if (!longTerm) {
                    realizedGains * marginalRate
                } else {
                    val combined = taxableOrdinaryIncome + realizedGains
                    val ltcgRate = when {
                        combined <= 47_025.0 -> 0.0
                        combined <= 518_900.0 -> 0.15
                        else -> 0.20
                    }
                    realizedGains * ltcgRate
                }
            }
            TaxRegime.US_JOINT -> {
                if (!longTerm) {
                    realizedGains * marginalRate
                } else {
                    val combined = taxableOrdinaryIncome + realizedGains
                    val ltcgRate = when {
                        combined <= 94_050.0 -> 0.0
                        combined <= 583_750.0 -> 0.15
                        else -> 0.20
                    }
                    realizedGains * ltcgRate
                }
            }
            TaxRegime.IN_OLD, TaxRegime.IN_NEW -> {
                if (longTerm) {
                    val exemptGains = (realizedGains - 125_000.0).coerceAtLeast(0.0)
                    exemptGains * 0.125
                } else {
                    realizedGains * 0.20
                }
            }
        }
    }
}
