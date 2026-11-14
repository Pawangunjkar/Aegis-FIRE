package com.example.services

import com.example.models.DailyFinancialTip
import com.example.models.DailyWisdomCategory
import java.util.Calendar

object DailyTipProvider {

    val curatedTips: List<DailyFinancialTip> = listOf(
        DailyFinancialTip(
            id = 1,
            category = DailyWisdomCategory.COMPOUNDING,
            quote = "The first rule of compounding is to never interrupt it unnecessarily.",
            author = "Charlie Munger",
            actionableTip = "Automate your monthly Portfolio SIP in Cashflow so contributions invest regardless of market headlines."
        ),
        DailyFinancialTip(
            id = 2,
            category = DailyWisdomCategory.SAVINGS_RATE,
            quote = "Do not save what is left after spending, but spend what is left after saving.",
            author = "Warren Buffett",
            actionableTip = "Set category saving targets inside Budget mode before allocating discretionary flex spend."
        ),
        DailyFinancialTip(
            id = 3,
            category = DailyWisdomCategory.BEHAVIORAL,
            quote = "Wealth is what you don't see — the cars not purchased, the upgrades declined.",
            author = "Morgan Housel",
            actionableTip = "Compare every major upgrade against how many months closer it moves your FIRE crossover age."
        ),
        DailyFinancialTip(
            id = 4,
            category = DailyWisdomCategory.DEBT_FREEDOM,
            quote = "Every dollar of high-APR interest eliminated is a guaranteed, tax-free return.",
            author = "Aegis FIRE Principle",
            actionableTip = "Use the Avalanche strategy in Debt Payoff to clear 15%+ APR balances first."
        ),
        DailyFinancialTip(
            id = 5,
            category = DailyWisdomCategory.COMPOUNDING,
            quote = "Time is your friend; impulse is your enemy. Stay the course.",
            author = "John C. Bogle",
            actionableTip = "Increase your annual SIP Step-Up by even 3–5% each year as your income grows."
        ),
        DailyFinancialTip(
            id = 6,
            category = DailyWisdomCategory.TAX_ALPHA,
            quote = "It is not just what you earn or return, but what you keep after taxes and inflation.",
            author = "Aegis FIRE Principle",
            actionableTip = "Sync your effective rate from Tax Estimator to test realistic net post-tax FIRE projections."
        ),
        DailyFinancialTip(
            id = 7,
            category = DailyWisdomCategory.SAVINGS_RATE,
            quote = "Beware of little expenses; a small leak will sink a great ship.",
            author = "Benjamin Franklin",
            actionableTip = "Audit recurring subscriptions in your Budget planner and sweep freed cash to Debt Auto-Pay."
        ),
        DailyFinancialTip(
            id = 8,
            category = DailyWisdomCategory.BEHAVIORAL,
            quote = "Money is something you choose to trade your life energy for.",
            author = "Vicki Robin",
            actionableTip = "Track your Coast FIRE threshold so you know when existing assets can compound without new savings."
        ),
        DailyFinancialTip(
            id = 9,
            category = DailyWisdomCategory.COMPOUNDING,
            quote = "Compound interest is the eighth wonder of the world. He who understands it, earns it.",
            author = "Financial Maxim",
            actionableTip = "Diversify across equity, mutual funds, and bonds in Portfolio to balance return and volatility."
        ),
        DailyFinancialTip(
            id = 10,
            category = DailyWisdomCategory.DEBT_FREEDOM,
            quote = "Debt trades tomorrow's freedom for yesterday's consumption.",
            author = "Aegis FIRE Principle",
            actionableTip = "Once a loan is paid off, roll its entire minimum payment into the next balance to accelerate payoff."
        ),
        DailyFinancialTip(
            id = 11,
            category = DailyWisdomCategory.TAX_ALPHA,
            quote = "Maximizing tax-advantaged accounts adds years of runway to your retirement corpus.",
            author = "Aegis FIRE Principle",
            actionableTip = "Model 401(k), IRA, or Section 80C deductions in Tax Estimator before finalizing annual SIPs."
        ),
        DailyFinancialTip(
            id = 12,
            category = DailyWisdomCategory.BEHAVIORAL,
            quote = "Seek wealth, not money or status. Wealth is having assets that earn while you sleep.",
            author = "Naval Ravikant",
            actionableTip = "Monitor Net Worth rather than gross assets alone by tracking all loans in Liabilities."
        ),
        DailyFinancialTip(
            id = 13,
            category = DailyWisdomCategory.SAVINGS_RATE,
            quote = "Your savings rate matters far more than chasing an extra 1% of market alpha.",
            author = "Mr. Money Mustache",
            actionableTip = "Aim for a 30%+ monthly savings rate in Budget mode to raise your FI Score above 600."
        ),
        DailyFinancialTip(
            id = 14,
            category = DailyWisdomCategory.COMPOUNDING,
            quote = "An investment in knowledge pays the best interest.",
            author = "Benjamin Franklin",
            actionableTip = "Run a 1,000-path Monte Carlo simulation on your FIRE plan after major portfolio changes."
        ),
        DailyFinancialTip(
            id = 15,
            category = DailyWisdomCategory.DEBT_FREEDOM,
            quote = "When cashflow surplus sweeps straight into principal, debt timelines collapse fast.",
            author = "Aegis FIRE Principle",
            actionableTip = "Export your Full Backup CSV monthly to keep an encrypted offline audit trail of net worth."
        ),
        DailyFinancialTip(
            id = 16,
            category = DailyWisdomCategory.TAX_ALPHA,
            quote = "Inflation silently halves idle cash every 12 years at 6% — keep reserves working.",
            author = "Aegis FIRE Principle",
            actionableTip = "Check real Fisher yield in the Inflation Impact calculator for long-term cash goals."
        ),
        DailyFinancialTip(
            id = 17,
            category = DailyWisdomCategory.BEHAVIORAL,
            quote = "Calm investors survive bear markets; panicked sellers lock in temporary drops.",
            author = "Howard Marks",
            actionableTip = "Verify your 30-year SWP survival probability in Roadmap so downturns don't derail retirement."
        ),
        DailyFinancialTip(
            id = 18,
            category = DailyWisdomCategory.SAVINGS_RATE,
            quote = "A budget is telling your money where to go instead of wondering where it went.",
            author = "John C. Maxwell",
            actionableTip = "Link monthly salary and auto-SIPs in Cashflow so your Budget updates automatically each cycle."
        ),
        DailyFinancialTip(
            id = 19,
            category = DailyWisdomCategory.COMPOUNDING,
            quote = "Longevity risk is real — plan retirement payouts through age 85–95, not just day one.",
            author = "Aegis FIRE Principle",
            actionableTip = "Test life expectancy 90+ in the Retirement planner and sync your required corpus to FIRE."
        ),
        DailyFinancialTip(
            id = 20,
            category = DailyWisdomCategory.BEHAVIORAL,
            quote = "Financial independence is measured in months of freedom, not luxuries.",
            author = "Aegis FIRE Principle",
            actionableTip = "Check your Financial Independence Score milestones on the Roadmap tab for your next badge."
        ),
        DailyFinancialTip(
            id = 21,
            category = DailyWisdomCategory.TAX_ALPHA,
            quote = "Rebalancing tax-efficiently keeps risk aligned without triggering unnecessary capital gains.",
            author = "Aegis FIRE Principle",
            actionableTip = "Direct new monthly SIPs toward under-weighted asset classes instead of selling winners."
        )
    )

    fun dailyIndexForDate(calendar: Calendar = Calendar.getInstance()): Int {
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val seed = (year * 366 + dayOfYear).coerceAtLeast(0)
        return seed % curatedTips.size
    }

    fun tipAtIndex(index: Int): DailyFinancialTip {
        val size = curatedTips.size
        val normalized = ((index % size) + size) % size
        return curatedTips[normalized]
    }
}
