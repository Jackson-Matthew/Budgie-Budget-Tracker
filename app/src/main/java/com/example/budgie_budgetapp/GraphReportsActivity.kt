package com.example.budgie_budgetapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgie_budgetapp.data.AppDatabase
import com.example.budgie_budgetapp.utils.KEY_USER_ID
import com.example.budgie_budgetapp.utils.PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GraphReportsActivity : AppCompatActivity() {

    private lateinit var categoryDetailsContainer: LinearLayout
    private lateinit var periodSpinner: Spinner
    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private lateinit var customDateLayout: LinearLayout
    private lateinit var btnCustomDate: Button

    private var userId = 0
    private var startDate = Date()
    private var endDate = Date()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph_reports)

        userId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_USER_ID, 0)

        // Initialize views
        categoryDetailsContainer = findViewById(R.id.categoryDetailsContainer)
        periodSpinner = findViewById(R.id.periodSpinner)
        startDateText = findViewById(R.id.startDateText)
        endDateText = findViewById(R.id.endDateText)
        customDateLayout = findViewById(R.id.customDateLayout)
        btnCustomDate = findViewById(R.id.btnCustomDate)

        // Hide the bar chart since we're using a custom visual representation
        val barChart = findViewById<View>(R.id.barChart)
        barChart.visibility = View.GONE

        // Set default dates (last 30 days)
        val cal = Calendar.getInstance()
        endDate = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -30)
        startDate = cal.time
        startDateText.text = dateFormat.format(startDate)
        endDateText.text = dateFormat.format(endDate)

        setupDatePickers()
        setupPeriodSpinner()
        setupBackButton()
        setupNavigation()
        loadData()
    }

    private fun setupPeriodSpinner() {
        val periods = arrayOf("Last 7 Days", "Last 30 Days", "This Month", "Last Month")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)
        periodSpinner.adapter = adapter

        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!btnCustomDate.isSelected) {
                    updateDateRange(periods[position])
                    loadData()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnCustomDate.setOnClickListener {
            btnCustomDate.isSelected = !btnCustomDate.isSelected
            customDateLayout.visibility = if (btnCustomDate.isSelected) View.VISIBLE else View.GONE
            periodSpinner.isEnabled = !btnCustomDate.isSelected

            if (!btnCustomDate.isSelected) {
                updateDateRange(periods[periodSpinner.selectedItemPosition])
                loadData()
            }
        }
    }

    private fun updateDateRange(period: String) {
        val cal = Calendar.getInstance()
        endDate = cal.time

        when (period) {
            "Last 7 Days" -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "Last 30 Days" -> cal.add(Calendar.DAY_OF_YEAR, -30)
            "This Month" -> cal.set(Calendar.DAY_OF_MONTH, 1)
            "Last Month" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        startDate = cal.time
    }

    private fun setupDatePickers() {
        startDateText.setOnClickListener {
            showDatePicker(startDate) { date ->
                startDate = date
                startDateText.text = dateFormat.format(date)
                if (btnCustomDate.isSelected) loadData()
            }
        }

        endDateText.setOnClickListener {
            showDatePicker(endDate) { date ->
                endDate = date
                endDateText.text = dateFormat.format(date)
                if (btnCustomDate.isSelected) loadData()
            }
        }
    }

    private fun showDatePicker(initialDate: Date, onDateSet: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        cal.time = initialDate
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                onDateSet(cal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@GraphReportsActivity)
            val totals = withContext(Dispatchers.IO) {
                db.transactionDao().getCategoryTotals(userId, startDate, endDate, "expense")
            }

            // Get budget goals for min/max display
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentYear = cal.get(Calendar.YEAR)
            val budgetGoals = withContext(Dispatchers.IO) {
                db.budgetGoalDao().getGoalsForMonth(userId, currentMonth, currentYear)
            }

            withContext(Dispatchers.Main) {
                displayData(totals, budgetGoals)
            }
        }
    }

    private fun displayData(totals: List<com.example.budgie_budgetapp.data.dao.CategoryTotal>,
                            budgetGoals: List<com.example.budgie_budgetapp.data.entity.BudgetGoal>) {
        categoryDetailsContainer.removeAllViews()

        if (totals.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No expenses recorded for this period"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
            }
            categoryDetailsContainer.addView(emptyText)
            return
        }

        // Calculate total for percentages
        val totalSpent = totals.sumOf { it.total }

        for (total in totals) {
            val percentage = if (totalSpent > 0) (total.total / totalSpent * 100) else 0.0
            val goal = budgetGoals.find { it.categoryName.equals(total.categoryName, ignoreCase = true) }

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

            // Category name
            val nameText = TextView(this).apply {
                text = total.categoryName
                setTextColor(resources.getColor(R.color.yellow_accent, null))
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            card.addView(nameText)

            // Amount and percentage
            val amountText = TextView(this).apply {
                text = "R${String.format("%.2f", total.total)} (${String.format("%.1f", percentage)}% of total)"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(R.color.expense_red, null))
                setPadding(0, 8, 0, 8)
            }
            card.addView(amountText)

            // Visual bar representing percentage
            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
            }

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (percentage * 4).toInt(),
                    20
                )
                setBackgroundColor(resources.getColor(R.color.yellow_accent, null))
            }
            barContainer.addView(bar)
            card.addView(barContainer)

            // Show budget goals if they exist
            if (goal != null) {
                val budgetSection = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 12
                    }
                    setPadding(8, 8, 8, 8)
                    setBackgroundColor(Color.parseColor("#3D3D3D"))
                }

                if (goal.minBudget != null && goal.minBudget > 0) {
                    val minText = TextView(this).apply {
                        text = "Minimum Goal: R${String.format("%.2f", goal.minBudget)}"
                        textSize = 12f
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        if (total.total < goal.minBudget) {
                            append(" ⚠️ Below by R${String.format("%.2f", goal.minBudget - total.total)}")
                            setTextColor(resources.getColor(R.color.yellow_accent, null))
                        } else {
                            append(" ✓ Met")
                            setTextColor(resources.getColor(R.color.income_green, null))
                        }
                    }
                    budgetSection.addView(minText)
                }

                if (goal.maxBudget != null && goal.maxBudget > 0) {
                    val maxText = TextView(this).apply {
                        text = "Maximum Goal: R${String.format("%.2f", goal.maxBudget)}"
                        textSize = 12f
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        if (total.total > goal.maxBudget) {
                            append(" ⚠️ Over by R${String.format("%.2f", total.total - goal.maxBudget)}")
                            setTextColor(resources.getColor(R.color.expense_red, null))
                        } else {
                            append(" ✓ Within budget")
                            setTextColor(resources.getColor(R.color.income_green, null))
                        }
                    }
                    budgetSection.addView(maxText)
                }

                card.addView(budgetSection)
            }

            categoryDetailsContainer.addView(card)
        }
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
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
        findViewById<ImageButton>(R.id.navReports)?.setOnClickListener {
            finish()
        }
        findViewById<ImageButton>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}