package com.example.bankapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ExitGroupDialogFragment : DialogFragment() {

    private var listener: ExitGroupListener? = null

    interface ExitGroupListener {
        fun onExitConfirmed()
        fun onExitCancelled()
    }

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): ExitGroupDialogFragment {
            val fragment = ExitGroupDialogFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.exit_group_window, container, false)

        val btnConfirmExit: TextView = view.findViewById(R.id.exitBtn)
        val btnCancelExit: TextView = view.findViewById(R.id.cancelBtn)

        btnConfirmExit.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val groupId = arguments?.getString(ARG_GROUP_ID)

            if (userId != null && groupId != null) {
                val databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("Groups")
                    .child(groupId)

                databaseReference.removeValue()
                    .addOnSuccessListener {
                        listener?.onExitConfirmed()
                        // После успешного выхода, переходим на GroupsActivity
                        val intent = Intent(requireContext(), GroupsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        requireActivity().finish() // Закрываем текущее активити
                    }
                    .addOnFailureListener { error ->
                        Log.e("DeleteGroup", "Ошибка при удалении группы: ${error.message}")
                    }
            }
        }

        btnCancelExit.setOnClickListener {
            listener?.onExitCancelled()
            dismiss()
        }

        return view
    }

    fun setExitGroupListener(listener: ExitGroupListener) {
        this.listener = listener
    }
}
