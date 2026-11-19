package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.CommercialDocEntity
import com.example.data.local.InterventionEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ExportManager {

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportCommercialDocPdf(context: Context, doc: CommercialDocEntity): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply { color = Color.parseColor("#0F2744") }
        canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("GESTION PRO", 40f, 48f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#93C5FD")
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText("${doc.docType} N° ${doc.docNumber}", 40f, 74f, subPaint)
        canvas.drawText("Statut : ${doc.status}", 420f, 74f, subPaint)

        val darkText = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            isAntiAlias = true
        }
        val boldText = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 155f
        canvas.drawText("Client : ${doc.clientName}", 40f, y, boldText)
        y += 24f
        canvas.drawText("Objet : ${doc.title}", 40f, y, darkText)
        y += 22f
        canvas.drawText("Date d'émission : ${doc.issueDate}    |    Échéance : ${doc.dueDate}", 40f, y, darkText)
        y += 34f

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.5f
        }
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 28f

        canvas.drawText("Détail des prestations :", 40f, y, boldText)
        y += 24f
        canvas.drawText(doc.itemsSummary.ifBlank { doc.title }, 40f, y, darkText)
        y += 60f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        val fmt = { v: Double -> String.format(Locale.FRANCE, "%.2f €", v) }
        canvas.drawText("Total HT : ${fmt(doc.amountHt)}", 360f, y, darkText)
        y += 22f
        canvas.drawText("TVA (${doc.tvaRate.toInt()} %) : ${fmt(doc.amountHt * doc.tvaRate / 100.0)}", 360f, y, darkText)
        y += 26f
        canvas.drawText("Total TTC : ${fmt(doc.totalTtc)}", 360f, y, boldText)
        y += 24f
        canvas.drawText("Déjà réglé / Acompte : ${fmt(doc.amountPaid)}", 360f, y, darkText)
        y += 24f
        canvas.drawText("Reste à payer : ${fmt(doc.remainingDue)}", 360f, y, boldText)

        pdf.finishPage(page)
        val outFile = File(exportsDir(context), "${doc.docNumber}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun exportActivityReportPdf(
        context: Context,
        periodLabel: String,
        interventions: List<InterventionEntity>,
        docs: List<CommercialDocEntity>
    ): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply { color = Color.parseColor("#0F2744") }
        canvas.drawRect(0f, 0f, 595f, 95f, headerPaint)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("GESTION PRO — RAPPORT D'ACTIVITÉ", 36f, 45f, titlePaint)
        val subPaint = Paint().apply { color = Color.parseColor("#93C5FD"); textSize = 12f; isAntiAlias = true }
        canvas.drawText("Période : $periodLabel", 36f, 70f, subPaint)

        val bodyPaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 12f; isAntiAlias = true }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 130f
        val totalCa = docs.filter { it.docType == "FACTURE" }.sumOf { it.totalTtc }
        val paidCa = docs.filter { it.docType == "FACTURE" }.sumOf { it.amountPaid }
        canvas.drawText("Synthèse générale", 36f, y, sectionPaint)
        y += 22f
        canvas.drawText("• Interventions analysées : ${interventions.size} (Terminées : ${interventions.count { it.status == "TERMINEE" }})", 36f, y, bodyPaint)
        y += 20f
        canvas.drawText("• Chiffre d'affaires facturé TTC : ${String.format(Locale.FRANCE, "%.2f €", totalCa)}", 36f, y, bodyPaint)
        y += 20f
        canvas.drawText("• Encaissements réalisés : ${String.format(Locale.FRANCE, "%.2f €", paidCa)}", 36f, y, bodyPaint)
        y += 34f

        canvas.drawText("Détail des interventions (extrait)", 36f, y, sectionPaint)
        y += 22f
        interventions.take(18).forEach { item ->
            canvas.drawText("${item.reference} | ${item.scheduledDate} | ${item.status} | ${item.clientName} — ${item.title.take(32)}", 36f, y, bodyPaint)
            y += 18f
        }

        pdf.finishPage(page)
        val outFile = File(exportsDir(context), "Rapport_GestionPro_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    fun exportCsvForExcel(
        context: Context,
        interventions: List<InterventionEntity>,
        docs: List<CommercialDocEntity>
    ): File {
        val sb = StringBuilder()
        // UTF-8 BOM for seamless Excel accent opening
        sb.append("\uFEFF")
        sb.append("Type;Reference;Date;Client;Statut;Technicien_ou_MontantTTC\n")
        interventions.forEach {
            sb.append("Intervention;${it.reference};${it.scheduledDate};\"${it.clientName}\";${it.status};\"${it.technicianName}\"\n")
        }
        docs.forEach {
            sb.append("${it.docType};${it.docNumber};${it.issueDate};\"${it.clientName}\";${it.status};${String.format(Locale.US, "%.2f", it.totalTtc)}\n")
        }
        val outFile = File(exportsDir(context), "Export_GestionPro_${System.currentTimeMillis()}.csv")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
