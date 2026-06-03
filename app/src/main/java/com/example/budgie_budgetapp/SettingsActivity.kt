package com.example.budgie_budgetapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budgie_budgetapp.utils.PREFS_NAME

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back Arrow Button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Close this activity and go back
        }

        // Edit Profile Button
        val editProfileButton = findViewById<Button>(R.id.btnEditProfile)
        editProfileButton.setOnClickListener {
            // Navigate to Edit Profile Activity (create this if needed)
            Toast.makeText(this, "Edit Profile - Coming soon!", Toast.LENGTH_SHORT).show()
            // Uncomment when you create EditProfileActivity:
            // startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Logout Button
        val logoutButton = findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Light Mode Toggle
        val lightModeSwitch = findViewById<Switch>(R.id.lightModeSwitch)

        // Load saved theme preference
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isLightMode = sharedPref.getBoolean("light_mode", false)
        lightModeSwitch.isChecked = isLightMode

        lightModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("light_mode", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "Light mode enabled. Restart app for changes to take effect.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Dark mode enabled. Restart app for changes to take effect.", Toast.LENGTH_LONG).show()
            }
            // To apply theme immediately without restart:
            // recreate()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Clear all shared preferences
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()

                // Navigate to Login Activity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}