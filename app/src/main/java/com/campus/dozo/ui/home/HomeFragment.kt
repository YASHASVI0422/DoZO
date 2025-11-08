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
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

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
        loadTasks()
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

    private fun loadTasks() {
        val currentUserId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        db.collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                // ✅ Prevent crashes if fragment view is destroyed
                if (!isAdded || _binding == null) return@addSnapshotListener

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading tasks: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                taskList.clear()

                snapshot?.documents?.forEach { doc ->
                    val task = doc.toObject(Task::class.java) ?: return@forEach
                    if (task.status.equals(TaskStatus.OPEN.name, ignoreCase = true)
                        && task.postedBy != currentUserId
                    ) {
                        taskList.add(task)
                    }
                }

                taskAdapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility =
                    if (taskList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTasks()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
