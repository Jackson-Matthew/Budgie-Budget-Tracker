package com.example.budgie_budgetapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgie_budgetapp.data.AppDatabase
import com.example.budgie_budgetapp.data.dao.CategoryTotal
import com.example.budgie_budgetapp.data.entity.BudgetGoal
import com.example.budgie_budgetapp.utils.CSVExportHelper
import com.example.budgie_budgetapp.utils.DateRangeHelper
import com.example.budgie_budgetapp.utils.KEY_USER_ID
import com.example.budgie_budgetapp.utils.KEY_USERNAME
import com.example.budgie_budgetapp.utils.PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var categoryBreakdownContainer: LinearLayout
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var btnSetBudget: Button
    private lateinit var btnViewGraph: Button
    private lateinit var btnViewCompliance: Button
    private lateinit var btnExportCSV: Button

    private var currentFilter = "Month"
    private var userId = 0
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        userId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_USER_ID, 0)

        initializeViews()
        setupFilterSpinner()
        setupButtons()
        setupNavigation()
        setupSettingsButton()
        loadReports()
    }

    private fun initializeViews() {
        categoryBreakdownContainer = findViewById(R.id.categoryBreakdownContainer)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        filterSpinner = findViewById(R.id.filterSpinner)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        btnViewGraph = findViewById(R.id.btnViewGraph)
        btnViewCompliance = findViewById(R.id.btnViewCompliance)
        btnExportCSV = findViewById(R.id.btnExportCSV)
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("Month", "Week", "Day")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilter = filters[position]
                loadReports()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        btnSetBudget.setOnClickListener {
            startActivity(Intent(this, BudgetGoalsActivity::class.java))
        }

        btnViewGraph.setOnClickListener {
            startActivity(Intent(this, GraphReportsActivity::class.java))
        }

        btnViewCompliance.setOnClickListener {
            startActivity(Intent(this, GoalComplianceActivity::class.java))
        }

        btnExportCSV.setOnClickListener {
            exportToCSV()
        }
    }

    private fun loadReports() {
        val (startDate, endDate) = DateRangeHelper.getDateRange(currentFilter)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ReportsActivity)

            val totalIncome = withContext(Dispatchers.IO) {
                db.transactionDao().getTotalByType(userId, "income", startDate, endDate) ?: 0.0
            }
            val totalExpenses = withContext(Dispatchers.IO) {
                db.transactionDao().getTotalByType(userId, "expense", startDate, endDate) ?: 0.0
            }
            val categoryTotals = withContext(Dispatchers.IO) {
                db.transactionDao().getCategoryTotals(userId, startDate, endDate, "expense")
            }
            val budgetGoals = withContext(Dispatchers.IO) {
                db.budgetGoalDao().getGoalsForMonth(userId, currentMonth, currentYear)
            }

            withContext(Dispatchers.Main) {
                tvTotalIncome.text = "R${String.format("%.2f", totalIncome)}"
                tvTotalExpenses.text = "R${String.format("%.2f", totalExpenses)}"
                displayCategoryBreakdown(categoryTotals, budgetGoals)
            }
        }
    }

    private fun displayCategoryBreakdown(categoryTotals: List<CategoryTotal>, budgetGoals: List<BudgetGoal>) {
        categoryBreakdownContainer.removeAllViews()

        if (categoryTotals.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No expenses recorded for this period"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
            }
            categoryBreakdownContainer.addView(emptyText)
            return
        }

        for (total in categoryTotals) {
            val goal = budgetGoals.find { it.categoryName.equals(total.categoryName, ignoreCase = true) }
            val card = createCategoryCard(total, goal)
            categoryBreakdownContainer.addView(card)
        }
    }

    private fun createCategoryCard(total: CategoryTotal, goal: BudgetGoal?): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.input_background)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val nameText = TextView(this).apply {
            text = total.categoryName
            setTextColor(resources.getColor(R.color.yellow_accent, null))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        card.addView(nameText)

        val amountText = TextView(this).apply {
            text = "-R${String.format("%.2f", total.total)}"
            setTextColor(resources.getColor(R.color.expense_red, null))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }
        card.addView(amountText)

        if (goal != null && (goal.minBudget != null || goal.maxBudget != null)) {
            if (goal.minBudget != null) {
                val minText = TextView(this).apply {
                    text = "Minimum: R${String.format("%.2f", goal.minBudget)}"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))

                    if (total.total < goal.minBudget!!) {
                        append(" ⚠️ Below by R${String.format("%.2f", goal.minBudget!! - total.total)}")
                        setTextColor(resources.getColor(R.color.yellow_accent, null))
                    } else if (total.total >= goal.minBudget!!) {
                        append(" ✓ Met")
                        setTextColor(resources.getColor(R.color.income_green, null))
                    }
                }
                card.addView(minText)
            }

            if (goal.maxBudget != null) {
                val maxText = TextView(this).apply {
                    text = "Maximum: R${String.format("%.2f", goal.maxBudget)}"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))

                    if (total.total > goal.maxBudget!!) {
                        append(" ⚠️ Over by R${String.format("%.2f", total.total - goal.maxBudget!!)}")
                        setTextColor(resources.getColor(R.color.expense_red, null))
                    } else if (total.total <= goal.maxBudget!!) {
                        append(" ✓ Within budget")
                        setTextColor(resources.getColor(R.color.income_green, null))
                    }
                }
                card.addView(maxText)
            }
        } else {
            val noBudgetText = TextView(this).apply {
                text = "No budget set for this category"
                textSize = 11f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 4, 0, 0)
            }
            card.addView(noBudgetText)
        }

        return card
    }

    private fun exportToCSV() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Generating CSV file...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ReportsActivity)
                val (startDate, endDate) = DateRangeHelper.getDateRange(currentFilter)

                // Get transactions for the period
                val transactions = withContext(Dispatchers.IO) {
                    db.transactionDao().getTransactionsBetweenDates(userId, startDate, endDate)
                }

                // Calculate totals
                val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
                val totalExpenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpenses

                // Get username
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val userName = prefs.getString(KEY_USERNAME, "User") ?: "User"

                val exportData = CSVExportHelper.ExportData(
                    userName = userName,
                    startDate = startDate,
                    endDate = endDate,
                    transactions = transactions,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    netBalance = netBalance
                )

                CSVExportHelper.generateCSV(
                    context = this@ReportsActivity,
                    data = exportData,
                    onSuccess = { csvFile ->
                        progressDialog.dismiss()
                        showCSVOptions(csvFile)
                    },
                    onError = { error ->
                        progressDialog.dismiss()
                        Toast.makeText(this@ReportsActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@ReportsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCSVOptions(csvFile: File) {
        val options = arrayOf("Share CSV", "Show File Location", "Delete CSV")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("CSV Generated Successfully!")
            .setMessage("File: ${csvFile.name}\nSize: ${csvFile.length() / 1024} KB\n\nFile saved to: Downloads folder")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareCSV(csvFile)
                    1 -> showFileLocation(csvFile)
                    2 -> {
                        if (csvFile.delete()) {
                            Toast.makeText(this, "CSV deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFileLocation(csvFile: File) {
        val message = """
        File saved to:
        ${csvFile.absolutePath}
        
        You can find it in your Downloads folder.
        
        To open:
        1. Open Files app
        2. Go to Downloads folder
        3. Tap on ${csvFile.name}
        4. Choose Excel, Google Sheets, or any text editor
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("File Location")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun shareCSV(csvFile: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Budgie Financial Export")
            putExtra(Intent.EXTRA_TEXT, "Here is my financial data from Budgie app.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share CSV via"))
    }

    private fun setupSettingsButton() {
        findViewById<ImageButton>(R.id.menuButton)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupNavigation() {
        findViewById<ImageButton>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.navHistory)?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.navReports)?.setOnClickListener { }
        findViewById<ImageButton>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}