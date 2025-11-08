package com.campus.dozo.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.campus.dozo.TaskDetailsActivity
import com.campus.dozo.adapters.TaskAdapter
import com.campus.dozo.databinding.FragmentHomeBinding
import com.campus.dozo.models.Task
import com.campus.dozo.models.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class Homefragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private var taskListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupSwipeRefresh()
        startTaskListener()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(taskList) { task ->
            val intent = Intent(requireContext(), TaskDetailsActivity::class.java)
            intent.putExtra("TASK_ID", task.id)
            startActivity(intent)
        }

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    // ✅ Real-time Firestore listener for tasks (compatible with all Firebase versions)
    private fun startTaskListener() {
        val currentUserId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        // Remove previous listener if it exists
        taskListener?.remove()

        taskListener = db.collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                taskList.clear()

                snapshot?.documents?.forEach { doc ->
                    val task = doc.toObject(Task::class.java) ?: return@forEach

                    // ✅ Show:
                    // - Tasks that are OPEN and not posted by the user
                    // - Tasks the user has ACCEPTED or COMPLETED
                    if (
                        (task.status == TaskStatus.OPEN.name && task.postedBy != currentUserId) ||
                        (task.acceptedBy == currentUserId &&
                                (task.status == TaskStatus.ACCEPTED.name ||
                                        task.status == TaskStatus.COMPLETED.name))
                    ) {
                        taskList.add(task)
                    }
                }

                taskList.sortByDescending { it.createdAt }

                taskAdapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility =
                    if (taskList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            startTaskListener()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        taskListener?.remove()
        _binding = null
    }
}
