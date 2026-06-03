package com.example.budgie_budgetapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgie_budgetapp.R
import com.example.budgie_budgetapp.models.Badge

class BadgeAdapter(
    private val badges: List<Badge>
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount(): Int = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val badgeIcon: ImageView = itemView.findViewById(R.id.badgeIcon)
        private val badgeName: TextView = itemView.findViewById(R.id.badgeName)
        private val badgeDescription: TextView = itemView.findViewById(R.id.badgeDescription)
        private val lockedOverlay: View = itemView.findViewById(R.id.lockedOverlay)
        private val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)

        fun bind(badge: Badge) {
            badgeIcon.setImageResource(badge.iconResId)
            badgeName.text = badge.name
            badgeDescription.text = badge.description

            if (badge.isUnlocked) {
                lockedOverlay.visibility = View.GONE
                lockIcon.visibility = View.GONE
                badgeIcon.alpha = 1.0f
                badgeName.alpha = 1.0f
                badgeDescription.alpha = 1.0f
            } else {
                lockedOverlay.visibility = View.VISIBLE
                lockIcon.visibility = View.VISIBLE
                badgeIcon.alpha = 0.5f
                badgeName.alpha = 0.5f
                badgeDescription.alpha = 0.5f
            }
        }
    }
}