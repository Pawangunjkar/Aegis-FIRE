package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.example.models.FireParameters
import com.example.models.FireResult
import com.example.models.MonteCarloResult
import com.example.models.PortfolioSummary
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

object PdfExportService {

    fun generateRoadmapPdfFile(
        context: Context,
        params: FireParameters,
        result: FireResult,
        mcResult: MonteCarloResult?,
        portfolioSummary: PortfolioSummary?,
        isIndianCurrency: Boolean
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11.5f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val formatter = getCurrencyFormatter(isIndianCurrency)

        var y = 56f
        canvas.drawText("Aegis FIRE — Financial Independence Roadmap", 44f, y, titlePaint)
        y += 18f
        canvas.drawLine(44f, y, 568f, y, dividerPaint)
        y += 24f

        canvas.drawText("Executive Summary", 44f, y, headingPaint)
        y += 18f

        val summaryLines = listOf(
            "Target FIRE Corpus: ${formatter.format(result.targetFireNumber)}",
            "Coast FIRE Threshold: ${formatter.format(result.coastFireNumber)}",
            "Gross Accumulated (Age ${params.targetAge}): ${formatter.format(result.grossAccumulated)}",
            "Estimated Tax Drag: ${formatter.format(result.taxDrag)}",
            "Net Post-Tax Corpus: ${formatter.format(result.netAccumulated)}",
            "FIRE Crossover Age: ${result.fireCrossoverAge?.toString() ?: "Not reached by ${params.targetAge}"}",
            "Depletion Projection: ${result.depletionYear?.let { "Age $it" } ?: "Sustainable (>50 yrs)"}"
        )
        summaryLines.forEach { line ->
            canvas.drawText(line, 44f, y, bodyPaint)
            y += 16f
        }

        y += 10f
        canvas.drawText("Model Assumptions", 44f, y, headingPaint)
        y += 18f
        val assumptions = listOf(
            "Age Horizon: ${params.currentAge} -> ${params.targetAge} | Current Savings: ${formatter.format(params.currentSavings)}",
            "Monthly SIP: ${formatter.format(params.monthlySip)} (${(params.stepUpRate * 100).roundToInt()}%/yr step-up) | Monthly Expense: ${formatter.format(params.monthlyExpense)}",
            "Expected Return: ${(params.expectedReturn * 100).roundToInt()}% | Inflation: ${(params.inflationRate * 100).roundToInt()}% | Tax: ${(params.taxRate * 100).roundToInt()}% | SWR: ${String.format(Locale.US, "%.1f", params.safeWithdrawalRate * 100)}%"
        )
        assumptions.forEach { line ->
            canvas.drawText(line, 44f, y, bodyPaint)
            y += 16f
        }

        if (mcResult != null) {
            y += 10f
            canvas.drawText("Monte Carlo Stress Test (1,000 Paths)", 44f, y, headingPaint)
            y += 18f
            canvas.drawText(
                "30-Year Survival: ${String.format(Locale.US, "%.1f", mcResult.longevityProbability)}% | 40-Year Survival: ${String.format(Locale.US, "%.1f", mcResult.longevity40Probability)}%",
                44f, y, bodyPaint
            )
            y += 16f
            val yr30Idx = 30.coerceAtMost(mcResult.percentile50.lastIndex)
            canvas.drawText(
                "Year 30 Percentiles -> P10: ${formatter.format(mcResult.percentile10[yr30Idx])} | P50: ${formatter.format(mcResult.percentile50[yr30Idx])} | P90: ${formatter.format(mcResult.percentile90[yr30Idx])}",
                44f, y, bodyPaint
            )
            y += 16f
        }

        if (portfolioSummary != null && portfolioSummary.allocationSlices.isNotEmpty()) {
            y += 10f
            canvas.drawText("Blended Portfolio Snapshot", 44f, y, headingPaint)
            y += 18f
            val mix = portfolioSummary.allocationSlices.joinToString(" | ") {
                "${it.assetType.label}: ${String.format(Locale.US, "%.0f", it.percentage)}%"
            }
            canvas.drawText(mix, 44f, y, bodyPaint)
            y += 16f
        }

        y += 12f
        canvas.drawText("Milestone Schedule (5-Year Checkpoints)", 44f, y, headingPaint)
        y += 18f
        canvas.drawText("Age", 44f, y, headingPaint)
        canvas.drawText("Gross Balance", 120f, y, headingPaint)
        canvas.drawText("Net After-Tax", 290f, y, headingPaint)
        canvas.drawText("Target FIRE", 450f, y, headingPaint)
        y += 16f

        val milestones = result.trajectory.filterIndexed { index, pt ->
            index == 0 || pt.age % 5 == 0 || index == result.trajectory.lastIndex
        }.take(12)

        milestones.forEach { pt ->
            if (y < 760f) {
                canvas.drawText("${pt.age}", 44f, y, bodyPaint)
                canvas.drawText(formatter.format(pt.grossBalance), 120f, y, bodyPaint)
                canvas.drawText(formatter.format(pt.netBalance), 290f, y, bodyPaint)
                canvas.drawText(formatter.format(pt.targetBalance ?: result.targetFireNumber), 450f, y, bodyPaint)
                y += 15f
            }
        }

        document.finishPage(page)

        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            val file = File(dir, "Aegis_FIRE_Roadmap.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            file
        } catch (e: Exception) {
            Log.e("PdfExportService", "Error writing PDF", e)
            document.close()
            null
        }
    }

    fun generateAndPrintPdf(
        context: Context,
        params: FireParameters,
        result: FireResult,
        mcResult: MonteCarloResult?,
        portfolioSummary: PortfolioSummary?,
        isIndianCurrency: Boolean
    ): Boolean {
        val file = generateRoadmapPdfFile(context, params, result, mcResult, portfolioSummary, isIndianCurrency)
            ?: return false
        printPdf(context, file)
        return true
    }

    fun generateAndSharePdf(
        context: Context,
        params: FireParameters,
        result: FireResult,
        mcResult: MonteCarloResult?,
        portfolioSummary: PortfolioSummary?,
        isIndianCurrency: Boolean
    ): Boolean {
        val file = generateRoadmapPdfFile(context, params, result, mcResult, portfolioSummary, isIndianCurrency)
            ?: return false
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Aegis FIRE Financial Roadmap")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Aegis FIRE Roadmap"))
            true
        } catch (e: Exception) {
            Log.e("PdfExportService", "Error sharing PDF", e)
            false
        }
    }

    private fun printPdf(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    java.io.FileInputStream(file).use { input ->
                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print("Aegis FIRE Roadmap", printAdapter, null)
    }

    private fun getCurrencyFormatter(isIndian: Boolean): NumberFormat {
        return if (isIndian) {
            NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        } else {
            NumberFormat.getCurrencyInstance(Locale.US)
        }
    }
}
