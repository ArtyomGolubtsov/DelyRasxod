package com.example.bankapp

import android.app.Dialog
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

    private var groupId: String? = null // ID группы
    private var listener: ExitGroupListener? = null

    interface ExitGroupListener {
        fun onExitConfirmed()
        fun onExitCancelled()
    }

    // Установите ID группы
    fun setGroupId(id: String) {
        groupId = id
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
            if (userId != null && groupId != null) {
                val databaseReference = FirebaseDatabase.getInstance()
                    .getReference("Users").child(userId).child("Groups").child(groupId!!)

                // Удалить группу
                databaseReference.removeValue().addOnSuccessListener {
                    listener?.onExitConfirmed() // Уведомление об успешном выходе из группы
                    dismiss()
                }.addOnFailureListener { error ->
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
