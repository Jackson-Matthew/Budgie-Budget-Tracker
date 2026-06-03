package com.example.budgie_budgetapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.budgie_budgetapp.data.entity.Transaction
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CSVExportHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    data class ExportData(
        val userName: String,
        val startDate: Date,
        val endDate: Date,
        val transactions: List<Transaction>,
        val totalIncome: Double,
        val totalExpenses: Double,
        val netBalance: Double
    )

    fun generateCSV(
        context: Context,
        data: ExportData,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Create CSV file in DOWNLOADS folder (easy to find!)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "budgie_export_$timestamp.csv"

            // Save to Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val csvFile = File(downloadsDir, fileName)

            FileWriter(csvFile).use { writer ->
                // Write header
                writer.write("Date,Category,Type,Amount,Description\n")

                // Write transactions
                if (data.transactions.isEmpty()) {
                    writer.write("No transactions found for this period,,,,\n")
                } else {
                    for (transaction in data.transactions) {
                        writer.write("${dateFormat.format(transaction.date)},")
                        writer.write("${transaction.categoryName},")
                        writer.write("${transaction.type},")
                        writer.write("${String.format("%.2f", transaction.amount)},")
                        writer.write("${transaction.description}\n")
                    }
                }

                // Write summary
                writer.write("\n")
                writer.write("SUMMARY,,,,,\n")
                writer.write("Total Income,,,R${String.format("%.2f", data.totalIncome)},\n")
                writer.write("Total Expenses,,,R${String.format("%.2f", data.totalExpenses)},\n")
                writer.write("Net Balance,,,R${String.format("%.2f", data.netBalance)},\n")
                writer.write("Export Date,,,${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())},\n")
            }

            onSuccess(csvFile)

        } catch (e: Exception) {
            e.printStackTrace()
            onError("Failed to generate CSV: ${e.message}")
        }
    }

    fun openCSV(context: Context, csvFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open CSV with..."))
        } catch (e: Exception) {
            // If no app can open CSV, show file location instead
            val toast = android.widget.Toast.makeText(
                context,
                "File saved to Downloads folder: ${csvFile.absolutePath}",
                android.widget.Toast.LENGTH_LONG
            )
            toast.show()
        }
    }
}