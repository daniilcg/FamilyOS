package com.familyos.feature.billing.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates a real PDF report using Android [PdfDocument].
 */
@Singleton
class ExportPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ExportInput(
        val familyName: String,
        val tasks: List<TaskItem> = emptyList(),
        val shopping: List<ShoppingItem> = emptyList(),
        val budget: List<BudgetTransaction> = emptyList(),
    )

    suspend operator fun invoke(input: ExportInput): Result<File> = Result.runCatching {
        val doc = PdfDocument()
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        val titlePaint = Paint(paint).apply {
            textSize = 18f
            isFakeBoldText = true
        }
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 40f

        fun newPageIfNeeded(needed: Float = 20f) {
            if (y + needed > 800f) {
                doc.finishPage(page)
                pageNumber += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                y = 40f
            }
        }

        fun line(text: String, bold: Boolean = false) {
            newPageIfNeeded()
            canvas.drawText(text, 40f, y, if (bold) titlePaint else paint)
            y += if (bold) 28f else 18f
        }

        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        line("FamilyOS Export — ${input.familyName}", bold = true)
        line("Generated $stamp")
        line("")
        line("Tasks (${input.tasks.size})", bold = true)
        input.tasks.forEach { task ->
            line("- [${task.status}] ${task.title} (${task.priority})")
        }
        line("")
        line("Shopping (${input.shopping.size})", bold = true)
        input.shopping.forEach { item ->
            line("- ${item.quantity} ${item.unit.orEmpty()} ${item.title}".trim())
        }
        line("")
        line("Budget (${input.budget.size})", bold = true)
        input.budget.forEach { tx ->
            val sign = if (tx.isIncome) "+" else "-"
            line("- $sign${tx.amount} ${tx.currency} ${tx.title} (${tx.category})")
        }
        doc.finishPage(page)

        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(outDir, "familyos_export_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }
}

/**
 * Generates a spreadsheet-compatible file.
 *
 * Writes SpreadsheetML XML (Excel-openable) plus a CSV twin for maximum compatibility.
 */
@Singleton
class ExportExcelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ExportInput(
        val familyName: String,
        val tasks: List<TaskItem> = emptyList(),
        val shopping: List<ShoppingItem> = emptyList(),
        val budget: List<BudgetTransaction> = emptyList(),
    )

    data class ExportResult(
        val xmlFile: File,
        val csvFile: File,
    )

    suspend operator fun invoke(input: ExportInput): Result<ExportResult> = Result.runCatching {
        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val csvFile = File(outDir, "familyos_export_$stamp.csv")
        val xmlFile = File(outDir, "familyos_export_$stamp.xml")

        csvFile.writeText(buildString {
            appendLine("Section,Title,Status,Amount,Category,Notes")
            input.tasks.forEach {
                appendLine(csv("Task", it.title, it.status.name, "", it.priority.name, it.description.orEmpty()))
            }
            input.shopping.forEach {
                appendLine(csv("Shopping", it.title, it.status.name, it.quantity.toString(), it.category.name, it.notes.orEmpty()))
            }
            input.budget.forEach {
                appendLine(csv("Budget", it.title, if (it.isIncome) "INCOME" else "EXPENSE", it.amount.toString(), it.category.name, it.notes.orEmpty()))
            }
        })

        xmlFile.writeText(buildString {
            appendLine("""<?xml version="1.0"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""<Worksheet ss:Name="FamilyOS"><Table>""")
            fun row(vararg cells: String) {
                append("<Row>")
                cells.forEach { append("""<Cell><Data ss:Type="String">${escapeXml(it)}</Data></Cell>""") }
                appendLine("</Row>")
            }
            row("Family", input.familyName)
            row("Section", "Title", "Status", "Amount", "Category", "Notes")
            input.tasks.forEach {
                row("Task", it.title, it.status.name, "", it.priority.name, it.description.orEmpty())
            }
            input.shopping.forEach {
                row("Shopping", it.title, it.status.name, it.quantity.toString(), it.category.name, it.notes.orEmpty())
            }
            input.budget.forEach {
                row("Budget", it.title, if (it.isIncome) "INCOME" else "EXPENSE", it.amount.toString(), it.category.name, it.notes.orEmpty())
            }
            appendLine("</Table></Worksheet></Workbook>")
        })

        ExportResult(xmlFile = xmlFile, csvFile = csvFile)
    }

    private fun csv(vararg values: String): String =
        values.joinToString(",") { v ->
            val escaped = v.replace("\"", "\"\"")
            "\"$escaped\""
        }

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
