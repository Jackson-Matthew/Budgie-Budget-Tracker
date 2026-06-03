package com.example.budgie_budgetapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgie_budgetapp.adapters.BadgeAdapter
import com.example.budgie_budgetapp.utils.BadgeManager
import com.example.budgie_budgetapp.utils.KEY_USERNAME
import com.example.budgie_budgetapp.utils.PREFS_NAME

class ProfileActivity : AppCompatActivity() {

    private lateinit var badgeManager: BadgeManager
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        badgeManager = BadgeManager(this)

        // Load and display user information
        loadUserInfo()

        setupBadges()
        setupNavigation()
        setupSettingsButton()
        setupRefreshButton()
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Get user info from SharedPreferences
        val userName = sharedPref.getString(KEY_USERNAME, "Guest User") ?: "Guest User"

        // Generate initials
        val userInitials = if (userName != "Guest User" && userName.length >= 2) {
            userName.take(2).uppercase()
        } else if (userName != "Guest User") {
            userName.take(1).uppercase()
        } else {
            "GU"
        }

        // Update UI
        findViewById<TextView>(R.id.tvUserName)?.text = userName
        findViewById<TextView>(R.id.tvUserInitials)?.text = userInitials
    }

    private fun setupBadges() {
        val badges = badgeManager.getAllBadges()
        val recyclerView = findViewById<RecyclerView>(R.id.gridBadges)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        badgeAdapter = BadgeAdapter(badges)
        recyclerView.adapter = badgeAdapter
    }

    private fun refreshBadges() {
        val updatedBadges = badgeManager.getAllBadges()
        badgeAdapter = BadgeAdapter(updatedBadges)
        findViewById<RecyclerView>(R.id.gridBadges).adapter = badgeAdapter
    }

    private fun setupSettingsButton() {
        val settingsButton = findViewById<ImageButton>(R.id.menuButton)
        settingsButton?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupRefreshButton() {
        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)
        refreshButton?.setOnClickListener {
            loadUserInfo()
            refreshBadges()
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        val home = findViewById<ImageButton>(R.id.navHome)
        val history = findViewById<ImageButton>(R.id.navHistory)
        val reports = findViewById<ImageButton>(R.id.navReports)
        val profile = findViewById<ImageButton>(R.id.navProfile)

        home?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        history?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        reports?.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
            finish()
        }

        profile?.setOnClickListener {
            refreshBadges()
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show()
        }
    }
}