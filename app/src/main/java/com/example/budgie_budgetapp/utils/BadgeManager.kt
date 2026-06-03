package com.example.budgie_budgetapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.example.budgie_budgetapp.R
import com.example.budgie_budgetapp.models.Badge

class BadgeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("BadgePrefs", Context.MODE_PRIVATE)

    fun getAllBadges(): List<Badge> {
        return listOf(
            Badge(
                id = "welcome",
                name = "Welcome Aboard!",
                description = "Created your account",
                iconResId = R.drawable.badge_welcome,
                isUnlocked = isBadgeUnlocked("welcome")
            ),
            Badge(
                id = "first_transaction",
                name = "First Steps",
                description = "Added your first transaction",
                iconResId = R.drawable.badge_first_steps,
                isUnlocked = isBadgeUnlocked("first_transaction")
            ),
            Badge(
                id = "budget_master",
                name = "Budget Planner",
                description = "Created your first budget goal",
                iconResId = R.drawable.badge_budget_planner,
                isUnlocked = isBadgeUnlocked("budget_master")
            ),
            Badge(
                id = "saver",
                name = "Saver",
                description = "Added 10 transactions",
                iconResId = R.drawable.badge_saver,
                isUnlocked = isBadgeUnlocked("saver")
            ),
            Badge(
                id = "consistent",
                name = "Consistent Saver",
                description = "Added transactions for 7 days in a row",
                iconResId = R.drawable.badge_consistent_saver,
                isUnlocked = isBadgeUnlocked("consistent")
            ),
            Badge(
                id = "goal_achiever",
                name = "Goal Crusher",
                description = "Met a budget goal",
                iconResId = R.drawable.badge_goal_crusher,
                isUnlocked = isBadgeUnlocked("goal_achiever")
            )
        )
    }

    fun isBadgeUnlocked(badgeId: String): Boolean {
        return prefs.getBoolean(badgeId, false)
    }

    fun unlockBadge(badgeId: String) {
        if (!isBadgeUnlocked(badgeId)) {
            prefs.edit().putBoolean(badgeId, true).apply()
            showBadgeUnlockedNotification(badgeId)
        }
    }

    private fun showBadgeUnlockedNotification(badgeId: String) {
        val badge = getAllBadges().find { it.id == badgeId }
        badge?.let {
            Toast.makeText(
                context,
                "🎉 Achievement Unlocked: ${it.name}!\n${it.description}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun checkAndUnlockTransactionBadge(transactionCount: Int) {
        if (transactionCount >= 1 && !isBadgeUnlocked("first_transaction")) {
            unlockBadge("first_transaction")
        }
        if (transactionCount >= 10 && !isBadgeUnlocked("saver")) {
            unlockBadge("saver")
        }
    }

    fun checkAndUnlockBudgetBadge(budgetCount: Int) {
        if (budgetCount >= 1 && !isBadgeUnlocked("budget_master")) {
            unlockBadge("budget_master")
        }
    }

    fun checkAndUnlockWelcomeBadge() {
        if (!isBadgeUnlocked("welcome")) {
            unlockBadge("welcome")
        }
    }
}