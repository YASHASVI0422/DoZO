package com.campus.dozo.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.campus.dozo.databinding.FragmentPostTaskBinding
import com.campus.dozo.models.Task
import com.campus.dozo.models.TaskCategory
import com.campus.dozo.models.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostTaskFragment : Fragment() {

    private var _binding: FragmentPostTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userName: String = "Anonymous"
    private val TAG = "PostTaskFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserName()
        setupCategorySpinner()
        setupPostButton()
    }

    /**
     * Load the name of the logged-in user from Firestore
     */
    private fun loadUserName() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: "Anonymous"
                Log.d(TAG, "✅ User name loaded: $userName")
            }
            .addOnFailureListener {
                Log.e(TAG, "⚠️ Error loading username")
                userName = "Anonymous"
            }
    }

    /**
     * Populate the spinner with task categories
     */
    private fun setupCategorySpinner() {
        val categories = TaskCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerCategory.adapter = adapter
    }

    /**
     * Handle Post button click
     */
    private fun setupPostButton() {
        binding.btnPostTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            val categoryPosition = binding.spinnerCategory.selectedItemPosition

            if (validateInputs(title, description)) {
                val selectedCategory = TaskCategory.values()[categoryPosition].name
                postTask(title, description, selectedCategory)
            }
        }
    }

    /**
     * Validate form inputs
     */
    private fun validateInputs(title: String, description: String): Boolean {
        if (title.isEmpty()) {
            binding.etTaskTitle.error = "Title required"
            return false
        }
        if (description.isEmpty()) {
            binding.etTaskDescription.error = "Description required"
            return false
        }
        if (title.length < 5) {
            binding.etTaskTitle.error = "Title too short (min 5 chars)"
            return false
        }
        return true
    }

    /**
     * Upload task to Firestore and update user's posted count
     */
    private fun postTask(title: String, description: String, category: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPostTask.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPostTask.text = "Posting..."

        // ✅ Create a document reference first — ensures ID consistency
        val taskRef = db.collection("tasks").document()
        val taskId = taskRef.id

        val task = Task(
            id = taskId,
            title = title,
            description = description,
            category = category,
            status = TaskStatus.OPEN.name,
            postedBy = userId,
            postedByName = userName,
            createdAt = System.currentTimeMillis(),
            acceptedBy = "",
            acceptedByName = "",
            acceptedAt = 0,
            completedAt = 0
        )

        // ✅ Add the task to Firestore
        taskRef.set(task)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                Toast.makeText(requireContext(), "✅ Task posted successfully", Toast.LENGTH_SHORT).show()

                // 🔁 Update user's task count instantly
                db.collection("users").document(userId)
                    .update("tasksPosted", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d(TAG, "📈 User stats updated instantly")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "⚠️ Failed to update user stats: ${e.message}")
                    }

                // 🧹 Reset UI
                binding.etTaskTitle.text?.clear()
                binding.etTaskDescription.text?.clear()
                binding.spinnerCategory.setSelection(0)
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "❌ Failed: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error posting task", e)
            }
            .addOnCompleteListener {
                if (!isAdded || _binding == null) return@addOnCompleteListener
                binding.progressBar.visibility = View.GONE
                binding.btnPostTask.isEnabled = true
                binding.btnPostTask.text = "Post Task"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
