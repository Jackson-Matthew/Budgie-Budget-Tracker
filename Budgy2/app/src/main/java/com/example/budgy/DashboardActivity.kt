package com.example.budgy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardActivity : AppCompatActivity() {

    private val TAG = "DashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        Log.d(TAG, "DashboardActivity created")

        setupNavigation()
        setupSettingsButton()
        setupAddButton()
    }

    private fun setupSettingsButton() {
        try {
            val settingsButton = findViewById<ImageButton>(R.id.menuButton)

            settingsButton?.setOnClickListener {
                Log.d(TAG, "Settings button clicked")
                startActivity(Intent(this, SettingsActivity::class.java))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: ${e.message}")
        }
    }

    private fun setupAddButton() {
        try {
            val addExpenseButton = findViewById<FloatingActionButton>(R.id.addExpenseButton)

            addExpenseButton?.setOnClickListener {
                Log.d(TAG, "Plus button clicked")

                val intent = Intent(this, TransactionsActivity::class.java)
                startActivity(intent)

                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up add button: ${e.message}")
        }
    }

    private fun setupNavigation() {
        try {
            val home = findViewById<ImageButton>(R.id.navHome)
            val history = findViewById<ImageButton>(R.id.navHistory)
            val reports = findViewById<ImageButton>(R.id.navReports)
            val profile = findViewById<ImageButton>(R.id.navProfile)

            home?.setOnClickListener {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
            }

            history?.setOnClickListener {
                startActivity(Intent(this, HistoryActivity::class.java))
            }

            reports?.setOnClickListener {
                startActivity(Intent(this, ReportsActivity::class.java))
            }

            profile?.setOnClickListener {
                startActivity(Intent(this, ProfileActivity::class.java))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in navigation: ${e.message}")
        }
    }
}