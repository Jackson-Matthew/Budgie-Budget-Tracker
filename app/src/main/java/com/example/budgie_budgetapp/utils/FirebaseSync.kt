package com.example.budgie_budgetapp.utils

import android.content.Context
import android.util.Log
import com.example.budgie_budgetapp.data.AppDatabase
import com.example.budgie_budgetapp.data.entity.BudgetGoal
import com.example.budgie_budgetapp.data.entity.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * FirebaseSync — pushes local Room data up to Firestore.
 *
 * Structure in Firestore:
 *   users/{userId}/transactions/{transactionId}
 *   users/{userId}/budgetGoals/{goalId}
 *
 * Guest user is stored under userId "guest".
 */
object FirebaseSync {

    private const val TAG = "FirebaseSync"
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Convert userId int to a Firestore-friendly string key
    private fun userKey(userId: Int) = if (userId == 0) "guest" else "user_$userId"

    // ── Transactions ──────────────────────────────────────────────────────────

    fun syncTransaction(transaction: Transaction) {
        val data = hashMapOf(
            "id"           to transaction.id,
            "amount"       to transaction.amount,
            "categoryId"   to transaction.categoryId,
            "categoryName" to transaction.categoryName,
            "type"         to transaction.type,
            "description"  to transaction.description,
            "date"         to transaction.date.time,   // store as Long (epoch ms)
            "startTime"    to transaction.startTime,
            "endTime"      to transaction.endTime,
            "photoPath"    to transaction.photoPath,
            "userId"       to transaction.userId
        )

        db.collection("users")
            .document(userKey(transaction.userId))
            .collection("transactions")
            .document(transaction.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Transaction ${transaction.id} synced to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync transaction ${transaction.id}: ${e.message}")
            }
    }

    fun deleteTransaction(transactionId: Int, userId: Int) {
        db.collection("users")
            .document(userKey(userId))
            .collection("transactions")
            .document(transactionId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Transaction $transactionId deleted from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete transaction $transactionId: ${e.message}")
            }
    }

    // ── Budget Goals ──────────────────────────────────────────────────────────

    fun syncBudgetGoal(goal: BudgetGoal) {
        val data = hashMapOf(
            "id"           to goal.id,
            "categoryId"   to goal.categoryId,
            "categoryName" to goal.categoryName,
            "minBudget"    to goal.minBudget,
            "maxBudget"    to goal.maxBudget,
            "userId"       to goal.userId,
            "month"        to goal.month,
            "year"         to goal.year
        )

        db.collection("users")
            .document(userKey(goal.userId))
            .collection("budgetGoals")
            .document(goal.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Budget goal ${goal.id} synced to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync budget goal ${goal.id}: ${e.message}")
            }
    }

    fun deleteBudgetGoal(goalId: Int, userId: Int) {
        db.collection("users")
            .document(userKey(userId))
            .collection("budgetGoals")
            .document(goalId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Budget goal $goalId deleted from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete budget goal $goalId: ${e.message}")
            }
    }

    // ── Read from Firestore → Room ────────────────────────────────────────────

    /**
     * Fetches all transactions for [userId] from Firestore and upserts them
     * into the local Room database. Safe to call every time the app starts —
     * Room's REPLACE conflict strategy means no duplicates are created.
     */
    fun fetchTransactions(context: Context, userId: Int, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("users")
                    .document(userKey(userId))
                    .collection("transactions")
                    .get()
                    .await()

                val dao = AppDatabase.getDatabase(context).transactionDao()

                for (doc in snapshot.documents) {
                    try {
                        val transaction = Transaction(
                            id           = (doc.getLong("id") ?: 0L).toInt(),
                            amount       = doc.getDouble("amount") ?: 0.0,
                            categoryId   = (doc.getLong("categoryId") ?: 0L).toInt(),
                            categoryName = doc.getString("categoryName") ?: "",
                            type         = doc.getString("type") ?: "expense",
                            description  = doc.getString("description") ?: "",
                            date         = Date(doc.getLong("date") ?: 0L),
                            startTime    = doc.getString("startTime"),
                            endTime      = doc.getString("endTime"),
                            photoPath    = doc.getString("photoPath"),
                            userId       = (doc.getLong("userId") ?: 0L).toInt()
                        )
                        dao.insert(transaction)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping transaction doc ${doc.id}: ${e.message}")
                    }
                }

                Log.d(TAG, "Fetched ${snapshot.size()} transactions from Firestore for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "fetchTransactions failed: ${e.message}")
            } finally {
                onComplete?.invoke()
            }
        }
    }

    /**
     * Fetches all budget goals for [userId] from Firestore and upserts them
     * into the local Room database.
     */
    fun fetchBudgetGoals(context: Context, userId: Int, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("users")
                    .document(userKey(userId))
                    .collection("budgetGoals")
                    .get()
                    .await()

                val dao = AppDatabase.getDatabase(context).budgetGoalDao()

                for (doc in snapshot.documents) {
                    try {
                        val goal = BudgetGoal(
                            id           = (doc.getLong("id") ?: 0L).toInt(),
                            categoryId   = (doc.getLong("categoryId") ?: 0L).toInt(),
                            categoryName = doc.getString("categoryName") ?: "",
                            minBudget    = doc.getDouble("minBudget"),
                            maxBudget    = doc.getDouble("maxBudget"),
                            userId       = (doc.getLong("userId") ?: 0L).toInt(),
                            month        = (doc.getLong("month") ?: 0L).toInt(),
                            year         = (doc.getLong("year") ?: 0L).toInt()
                        )
                        dao.insert(goal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping budgetGoal doc ${doc.id}: ${e.message}")
                    }
                }

                Log.d(TAG, "Fetched ${snapshot.size()} budget goals from Firestore for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "fetchBudgetGoals failed: ${e.message}")
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
