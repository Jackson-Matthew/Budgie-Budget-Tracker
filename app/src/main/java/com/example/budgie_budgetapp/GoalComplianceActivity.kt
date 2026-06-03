package com.example.budgie_budgetapp

import android.content.Intent
import android.os.Bundle
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

class GoalComplianceActivity : AppCompatActivity() {

    private lateinit var complianceContainer: LinearLayout
    private lateinit var tvMonthYear: TextView
    private lateinit var tvComplianceScore: TextView
    private lateinit var tvComplianceMessage: TextView

    private var userId = 0
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_compliance)

        userId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_USER_ID, 0)

        setupViews()
        setupMonthNavigation()
        setupBackButton()
        setupNavigation()
        loadData()
    }

    private fun setupViews() {
        complianceContainer = findViewById(R.id.complianceContainer)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvComplianceScore = findViewById(R.id.tvComplianceScore)
        tvComplianceMessage = findViewById(R.id.tvComplianceMessage)
        updateMonthDisplay()
    }

    private fun setupMonthNavigation() {
        findViewById<ImageButton>(R.id.prevMonthButton).setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, 1)
            cal.add(Calendar.MONTH, -1)
            currentMonth = cal.get(Calendar.MONTH)
            currentYear = cal.get(Calendar.YEAR)
            updateMonthDisplay()
            loadData()
        }

        findViewById<ImageButton>(R.id.nextMonthButton).setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, 1)
            cal.add(Calendar.MONTH, 1)
            if (cal.time <= Calendar.getInstance().time) {
                currentMonth = cal.get(Calendar.MONTH)
                currentYear = cal.get(Calendar.YEAR)
                updateMonthDisplay()
                loadData()
            } else {
                Toast.makeText(this, "Cannot view future months", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMonthDisplay() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        tvMonthYear.text = monthYearFormat.format(cal.time)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@GoalComplianceActivity)

            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, 1, 0, 0, 0)
            val startDate = cal.time
            cal.set(currentYear, currentMonth, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val endDate = cal.time

            val goals = withContext(Dispatchers.IO) {
                db.budgetGoalDao().getGoalsForMonth(userId, currentMonth + 1, currentYear)
            }

            val totals = withContext(Dispatchers.IO) {
                db.transactionDao().getCategoryTotals(userId, startDate, endDate, "expense")
            }

            val spentMap = totals.associate { it.categoryName to it.total }

            withContext(Dispatchers.Main) {
                displayCompliance(goals, spentMap)
            }
        }
    }

    private fun displayCompliance(goals: List<com.example.budgie_budgetapp.data.entity.BudgetGoal>, spentMap: Map<String, Double>) {
        complianceContainer.removeAllViews()

        if (goals.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No budget goals set for this month.\nGo to Budget Goals to set your targets."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
            }
            complianceContainer.addView(emptyText)
            tvComplianceScore.text = "0%"
            tvComplianceMessage.text = "Set budget goals to track compliance"
            return
        }

        var totalScore = 0.0
        var categoryCount = 0

        for (goal in goals) {
            val spent = spentMap[goal.categoryName] ?: 0.0
            val card = createComplianceCard(goal, spent)
            complianceContainer.addView(card)

            var score = 100.0
            if (goal.minBudget != null && goal.minBudget > 0 && spent < goal.minBudget) {
                score -= ((goal.minBudget - spent) / goal.minBudget * 50).coerceAtMost(50.0)
            }
            if (goal.maxBudget != null && goal.maxBudget > 0 && spent > goal.maxBudget) {
                score -= ((spent - goal.maxBudget) / goal.maxBudget * 100).coerceAtMost(100.0)
            }
            totalScore += score.coerceIn(0.0, 100.0)
            categoryCount++
        }

        val overallScore = if (categoryCount > 0) (totalScore / categoryCount).toInt() else 0
        tvComplianceScore.text = "$overallScore%"

        val message = when {
            overallScore >= 80 -> "Excellent! Great job staying on budget! 🎉"
            overallScore >= 60 -> "Good! Keep working on your spending goals. 👍"
            overallScore >= 40 -> "Fair. Review your budget categories. 📊"
            else -> "Needs improvement. Adjust your spending habits. ⚠️"
        }
        tvComplianceMessage.text = message

        val color = when {
            overallScore >= 70 -> resources.getColor(R.color.income_green, null)
            overallScore >= 40 -> resources.getColor(R.color.yellow_accent, null)
            else -> resources.getColor(R.color.expense_red, null)
        }
        tvComplianceScore.setTextColor(color)
    }

    private fun createComplianceCard(goal: com.example.budgie_budgetapp.data.entity.BudgetGoal, spent: Double): LinearLayout {
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
            text = goal.categoryName
            setTextColor(resources.getColor(R.color.yellow_accent, null))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        card.addView(nameText)

        val spentText = TextView(this).apply {
            text = "Spent: R${String.format("%.2f", spent)}"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)

            val isBelowMin = goal.minBudget != null && goal.minBudget > 0 && spent < goal.minBudget
            val isOverMax = goal.maxBudget != null && goal.maxBudget > 0 && spent > goal.maxBudget

            when {
                isBelowMin -> {
                    setTextColor(resources.getColor(R.color.yellow_accent, null))
                    append(" ⚠️ Below minimum")
                }
                isOverMax -> {
                    setTextColor(resources.getColor(R.color.expense_red, null))
                    append(" ❌ Over budget")
                }
                else -> {
                    setTextColor(resources.getColor(R.color.income_green, null))
                    append(" ✅ On track")
                }
            }
        }
        card.addView(spentText)

        if (goal.minBudget != null && goal.minBudget > 0) {
            val minText = TextView(this).apply {
                text = "Minimum: R${String.format("%.2f", goal.minBudget)}"
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                if (spent < goal.minBudget) {
                    append(" (Need R${String.format("%.2f", goal.minBudget - spent)} more)")
                    setTextColor(resources.getColor(R.color.yellow_accent, null))
                }
            }
            card.addView(minText)
        }

        if (goal.maxBudget != null && goal.maxBudget > 0) {
            val maxText = TextView(this).apply {
                text = "Maximum: R${String.format("%.2f", goal.maxBudget)}"
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                if (spent > goal.maxBudget) {
                    append(" (Over by R${String.format("%.2f", spent - goal.maxBudget)})")
                    setTextColor(resources.getColor(R.color.expense_red, null))
                }
            }
            card.addView(maxText)
        }

        if ((goal.minBudget != null && goal.minBudget > 0) || (goal.maxBudget != null && goal.maxBudget > 0)) {
            val maxValue = goal.maxBudget ?: maxOf(spent, goal.minBudget ?: 0.0) * 1.5
            val percentage = ((spent / maxValue) * 100).coerceIn(0.0, 100.0).toInt()

            val progressBar = ProgressBar(this).apply {
                max = 100
                progress = percentage
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    12
                ).apply {
                    topMargin = 12
                }

                val color = when {
                    goal.maxBudget != null && spent > goal.maxBudget -> resources.getColor(R.color.expense_red, null)
                    goal.minBudget != null && spent < goal.minBudget -> resources.getColor(R.color.yellow_accent, null)
                    else -> resources.getColor(R.color.income_green, null)
                }
                progressTintList = android.content.res.ColorStateList.valueOf(color)
            }
            card.addView(progressBar)
        }

        return card
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun setupNavigation() {
        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navHistory = findViewById<ImageButton>(R.id.navHistory)
        val navReports = findViewById<ImageButton>(R.id.navReports)
        val navProfile = findViewById<ImageButton>(R.id.navProfile)

        navHome?.setOnClickListener {
            val intent = Intent(this@GoalComplianceActivity, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        navHistory?.setOnClickListener {
            val intent = Intent(this@GoalComplianceActivity, HistoryActivity::class.java)
            startActivity(intent)
            finish()
        }

        navReports?.setOnClickListener {
            finish()
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this@GoalComplianceActivity, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}