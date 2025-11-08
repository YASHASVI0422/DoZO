package com.campus.dozo.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.campus.dozo.LoginActivity
import com.campus.dozo.databinding.FragmentProfileBinding
import com.campus.dozo.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var profileListener: ListenerRegistration? = null
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        startProfileListener()
        setupClickListeners()
    }

    // ✅ Works on all Firebase SDK versions
    private fun startProfileListener() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        // 🔧 Old-style listener registration
        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener

                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    user?.let { displayUserInfo(it) }
                } else {
                    Toast.makeText(requireContext(), "Profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun displayUserInfo(user: User) {
        binding.apply {
            tvUserName.text = user.name
            tvUserEmail.text = user.email
            tvUserPhone.text = user.phone
            tvUserBio.text = user.bio

            tvTasksPosted.text = user.tasksPosted.toString()
            tvTasksCompleted.text = user.tasksCompleted.toString()
            tvRating.text = String.format("%.1f", user.rating)

            val initials = user.name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            tvInitials.text = initials
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditMode) saveProfile()
            else enableEditMode()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun enableEditMode() {
        isEditMode = true
        binding.apply {
            etName.isEnabled = true
            etPhone.isEnabled = true
            etBio.isEnabled = true

            etName.setText(tvUserName.text)
            etPhone.setText(tvUserPhone.text)
            etBio.setText(tvUserBio.text)

            tvUserName.visibility = View.GONE
            tvUserPhone.visibility = View.GONE
            tvUserBio.visibility = View.GONE

            layoutName.visibility = View.VISIBLE
            layoutPhone.visibility = View.VISIBLE
            layoutBio.visibility = View.VISIBLE

            btnEditProfile.text = "Save Changes"
        }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return

        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Name and phone required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnEditProfile.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bio" to bio
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.btnEditProfile.isEnabled = true
                isEditMode = false
                Toast.makeText(requireContext(), "Profile updated 🎉", Toast.LENGTH_SHORT).show()
                disableEditMode()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnEditProfile.isEnabled = true
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun disableEditMode() {
        binding.apply {
            etName.isEnabled = false
            etPhone.isEnabled = false
            etBio.isEnabled = false

            tvUserName.visibility = View.VISIBLE
            tvUserPhone.visibility = View.VISIBLE
            tvUserBio.visibility = View.VISIBLE

            layoutName.visibility = View.GONE
            layoutPhone.visibility = View.GONE
            layoutBio.visibility = View.GONE

            btnEditProfile.text = "Edit Profile"
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 🧹 Manually remove listener
        profileListener?.remove()
        _binding = null
    }
}
