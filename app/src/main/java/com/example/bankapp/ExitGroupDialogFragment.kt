package com.example.bankapp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class ExitGroupDialogFragment : DialogFragment() {

    private var listener: ExitGroupListener? = null

    interface ExitGroupListener {
        fun onExitConfirmed()
        fun onExitCancelled()
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
            listener?.onExitConfirmed()
            dismiss()
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
