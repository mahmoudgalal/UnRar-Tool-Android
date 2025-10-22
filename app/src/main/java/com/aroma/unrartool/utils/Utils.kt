package com.aroma.unrartool.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.aroma.unrartool.R
import com.google.android.material.snackbar.Snackbar

object Utils {
    fun Fragment.buildProgressDialog(onCancel: () -> Unit): Pair<AlertDialog, TextView> {
        val root = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        val messageView = root.findViewById<TextView>(R.id.progress_message)
        val unrarCancelBtn = root.findViewById<AppCompatButton>(R.id.unrarCancelBtn)
        lateinit var dialog: AlertDialog
        unrarCancelBtn.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }
        val builder = AlertDialog.Builder(requireContext())
        dialog = builder.setTitle(null)
            .setCancelable(false)
            .setView(root)
            .setOnDismissListener {
            }.create()
        return Pair(dialog, messageView)
    }

    fun showMsg(anchorView: View, @StringRes msg: Int) {
        Snackbar.make(anchorView, msg, Snackbar.LENGTH_SHORT).show()
    }
}