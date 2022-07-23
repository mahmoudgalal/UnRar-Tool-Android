package com.aroma.unrartool.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import com.aroma.unrartool.BuildConfig
import com.aroma.unrartool.R
import com.aroma.unrartool.fragments.MainScreenFragment
import com.google.android.material.snackbar.Snackbar
import java.io.File

object Utils {

    fun deleteFile(path: String): Boolean {
        val cachedArchive = File(path)
        return cachedArchive.exists() && cachedArchive.delete()
    }

    fun checkAllFilesAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            // Access to all files
            val uri: Uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            return try {
                context.startActivity(intent)
                false
            } catch (ex: ActivityNotFoundException) {
                ex.printStackTrace();
                true
            }
        }
        return true
    }

    fun isAllFilesAccessGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        Environment.isExternalStorageManager()
    else
        true

    fun Fragment.buildProgressDialog(onCancel:()->Unit):AlertDialog {
        val root = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
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
        return dialog
    }
}