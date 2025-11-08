package com.campus.dozo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campus.dozo.databinding.ItemTaskBinding
import com.campus.dozo.models.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                tvTaskTitle.text = task.title
                tvTaskDescription.text = task.description
                tvCategory.text = getCategoryEmoji(task.category)
                tvPostedBy.text = "Posted by: ${task.postedByName}"
                tvTime.text = getTimeAgo(task.createdAt)

                // Set category background color
                categoryBadge.setBackgroundColor(getCategoryColor(task.category))

                root.setOnClickListener {
                    onTaskClick(task)
                }
            }
        }

        private fun getCategoryEmoji(category: String): String {
            return when (category) {
                "NOTES" -> "📚 Notes"
                "EVENT" -> "🎉 Event"
                "HELP" -> "🤝 Help"
                "STUDY" -> "📖 Study"
                "TRANSPORT" -> "🚗 Transport"
                else -> "🔧 Other"
            }
        }

        private fun getCategoryColor(category: String): Int {
            return when (category) {
                "NOTES" -> android.graphics.Color.parseColor("#3B82F6")
                "EVENT" -> android.graphics.Color.parseColor("#8B5CF6")
                "HELP" -> android.graphics.Color.parseColor("#10B981")
                "STUDY" -> android.graphics.Color.parseColor("#F59E0B")
                "TRANSPORT" -> android.graphics.Color.parseColor("#EF4444")
                else -> android.graphics.Color.parseColor("#6B7280")
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                else -> "${diff / 86400000}d ago"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size
}