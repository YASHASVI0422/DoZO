package com.campus.dozo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.campus.dozo.databinding.ActivityTaskDetailsBinding
import com.campus.dozo.models.Task
import com.campus.dozo.models.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var taskId: String = ""
    private var currentTask: Task? = null
    private var currentUserName: String = ""
    private val TAG = "TaskDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        taskId = intent.getStringExtra("TASK_ID") ?: ""

        if (taskId.isEmpty()) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadCurrentUserName()
        loadTaskDetails()
        setupClickListeners()
    }

    private fun loadCurrentUserName() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                currentUserName = document?.getString("name")
                    ?: auth.currentUser?.displayName
                            ?: auth.currentUser?.email
                            ?: "User"
                Log.d(TAG, "Loaded username: $currentUserName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user name", e)
                currentUserName = auth.currentUser?.displayName ?: "User"
            }
    }

    private fun loadTaskDetails() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("tasks").document(taskId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    Log.e(TAG, "Error loading task", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    currentTask = snapshot.toObject(Task::class.java)
                    currentTask?.let { displayTaskDetails(it) }
                } else {
                    Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun displayTaskDetails(task: Task) {
        binding.apply {
            tvTaskTitle.text = task.title
            tvTaskDescription.text = task.description
            tvCategory.text = getCategoryDisplay(task.category)
            tvPostedBy.text = "Posted by: ${task.postedByName}"
            tvPostedTime.text = "Posted: ${getFormattedDate(task.createdAt)}"
            tvStatus.text = "Status: ${task.status}"

            // ✅ Use .name for proper string comparison
            when (task.status) {
                TaskStatus.OPEN.name -> {
                    tvStatus.setTextColor(getColor(R.color.success))
                    cardStatus.setCardBackgroundColor(getColor(R.color.success))
                    tvAcceptedBy.visibility = View.GONE
                }
                TaskStatus.ACCEPTED.name -> {
                    tvStatus.setTextColor(getColor(R.color.warning))
                    cardStatus.setCardBackgroundColor(getColor(R.color.warning))
                    tvAcceptedBy.visibility = View.VISIBLE
                    tvAcceptedBy.text = "✓ Accepted by: ${task.acceptedByName}"
                }
                TaskStatus.COMPLETED.name -> {
                    tvStatus.setTextColor(getColor(R.color.primary))
                    cardStatus.setCardBackgroundColor(getColor(R.color.primary))
                    tvAcceptedBy.visibility = View.VISIBLE
                    tvAcceptedBy.text = "✓ Completed by: ${task.acceptedByName}"
                }
                TaskStatus.CANCELLED.name -> {
                    tvStatus.setTextColor(getColor(R.color.error))
                    cardStatus.setCardBackgroundColor(getColor(R.color.error))
                    tvAcceptedBy.visibility = View.GONE
                }
            }

            val currentUserId = auth.currentUser?.uid ?: ""
            btnAcceptTask.visibility = View.GONE
            btnCompleteTask.visibility = View.GONE
            btnCancelTask.visibility = View.GONE

            when {
                task.status == TaskStatus.OPEN.name -> {
                    if (task.postedBy != currentUserId) {
                        btnAcceptTask.visibility = View.VISIBLE
                    } else {
                        btnCancelTask.visibility = View.VISIBLE
                    }
                }
                task.status == TaskStatus.ACCEPTED.name && task.acceptedBy == currentUserId -> {
                    btnCompleteTask.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAcceptTask.setOnClickListener { showAcceptDialog() }
        binding.btnCompleteTask.setOnClickListener { showCompleteDialog() }
        binding.btnCancelTask.setOnClickListener { showCancelDialog() }
    }

    private fun showAcceptDialog() {
        AlertDialog.Builder(this)
            .setTitle("Accept Task")
            .setMessage("Are you sure you want to accept this task?")
            .setPositiveButton("Yes") { _, _ -> acceptTask() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Complete Task")
            .setMessage("Mark this task as completed?")
            .setPositiveButton("Yes") { _, _ -> completeTask() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Task")
            .setMessage("Are you sure you want to cancel this task?")
            .setPositiveButton("Yes") { _, _ -> cancelTask() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun acceptTask() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnAcceptTask.isEnabled = false

        val taskRef = db.collection("tasks").document(taskId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(taskRef)
            if (!snapshot.exists()) throw Exception("Task not found")

            val status = snapshot.getString("status") ?: ""
            val acceptedBy = snapshot.getString("acceptedBy") ?: ""

            if (status != TaskStatus.OPEN.name) throw Exception("Task is not open")
            if (acceptedBy.isNotEmpty()) throw Exception("Already accepted")

            val updates = mapOf(
                "status" to TaskStatus.ACCEPTED.name,
                "acceptedBy" to userId,
                "acceptedByName" to currentUserName.ifEmpty { "User" },
                "acceptedAt" to System.currentTimeMillis()
            )
            transaction.update(taskRef, updates)
        }.addOnSuccessListener {
            binding.progressBar.visibility = View.GONE
            binding.btnAcceptTask.isEnabled = true
            Toast.makeText(this, "Task accepted 🎉", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            binding.btnAcceptTask.isEnabled = true
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun completeTask() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCompleteTask.isEnabled = false

        val taskRef = db.collection("tasks").document(taskId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(taskRef)
            if (!snapshot.exists()) throw Exception("Task not found")

            val status = snapshot.getString("status") ?: ""
            val acceptedBy = snapshot.getString("acceptedBy") ?: ""

            if (status != TaskStatus.ACCEPTED.name) throw Exception("Task is not accepted yet")
            if (acceptedBy != userId) throw Exception("You didn't accept this task")

            transaction.update(taskRef, mapOf(
                "status" to TaskStatus.COMPLETED.name,
                "completedAt" to System.currentTimeMillis()
            ))

            val userRef = db.collection("users").document(userId)
            transaction.update(userRef, "tasksCompleted", FieldValue.increment(1))
        }.addOnSuccessListener {
            binding.progressBar.visibility = View.GONE
            binding.btnCompleteTask.isEnabled = true
            Toast.makeText(this, "Task completed 🎊", Toast.LENGTH_SHORT).show()
            binding.root.postDelayed({ finish() }, 1000)
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            binding.btnCompleteTask.isEnabled = true
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelTask() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("tasks").document(taskId)
            .update("status", TaskStatus.CANCELLED.name)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Task cancelled", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getCategoryDisplay(category: String): String {
        return when (category) {
            "NOTES" -> "📚 Notes Sharing"
            "EVENT" -> "🎉 Event Partner"
            "HELP" -> "🤝 Campus Help"
            "STUDY" -> "📖 Study Group"
            "TRANSPORT" -> "🚗 Transport"
            else -> "🔧 Other"
        }
    }

    private fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
