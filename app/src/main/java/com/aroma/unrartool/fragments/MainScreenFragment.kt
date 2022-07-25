/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool.fragments


import android.view.animation.Animation
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import com.aroma.unrartool.R
import android.text.TextUtils.TruncateAt
import android.text.method.ScrollingMovementMethod
import android.view.animation.AnimationUtils
import com.aroma.unrartool.MainActivity
import android.os.AsyncTask
import com.aroma.unrartool.Unrar
import android.os.PowerManager.WakeLock
import android.os.PowerManager
import android.animation.ValueAnimator
import androidx.appcompat.app.AlertDialog
import android.content.Context
import com.aroma.unrartool.Unrar.CallBackListener
import androidx.appcompat.widget.AppCompatCheckBox
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.aroma.unrartool.utils.Utils
import com.aroma.unrartool.utils.Utils.buildProgressDialog
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.atomic.AtomicBoolean

class MainScreenFragment : Fragment(), View.OnClickListener {
    private lateinit var unrarBtn: Button
    private lateinit var openArchive: Button
    private lateinit var extractToBtn: Button
    private lateinit var openedFile: TextView
    private lateinit var processedFile: TextView
    private lateinit var extractionPathText: TextView
    private lateinit var archiveData: TextView
    private var path: String? = null
    private var extractionPath: String? = null
    private var scaleUp: Animation? = null
    private lateinit var mainLogo: ImageView
    private lateinit var task: UnrarTask

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.main_screen, container, false)
        init(context, root)
        return root
    }

    override fun onStart() {
        super.onStart()
        if (extractionPath != null) {
            extractionPathText.text = extractionPath
        }
        if (path != null) {
            openedFile.text = path
            unrarBtn.isEnabled = true
            archiveData.text = ""
        }
    }

    private fun init(context: Context?, root: View) {
        mainLogo = root.findViewById(R.id.main_logo)
        openedFile = root.findViewById(R.id.archive_name_txt)
        with(openedFile) {
            isSelected = true
            ellipsize = TruncateAt.MARQUEE
        }
        processedFile = root.findViewById(R.id.progress_box)
        extractionPathText = root.findViewById(R.id.extractionpath)
        with(extractionPathText) {
            isSelected = true
            ellipsize = TruncateAt.MARQUEE
        }
        extractToBtn = root.findViewById(R.id.extract_to_btn)
        extractToBtn.setOnClickListener(this)
        archiveData = root.findViewById(R.id.archivedata)
        archiveData.movementMethod = ScrollingMovementMethod()
        archiveData.visibility = View.INVISIBLE
        unrarBtn = root.findViewById(R.id.unRarBtn)
        unrarBtn.setOnClickListener(this)
        openArchive = root.findViewById(R.id.openFile)
        openArchive.setOnClickListener(this)
        scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
    }

    var openedArchivePath: String?
        get() = path
        set(path) {
            this.path = path
            openedFile.text = path
            unrarBtn.isEnabled = true
            archiveData.text = ""
        }

    fun setSelectedExtractionPath(path: String?) {
        extractionPath = path
        extractionPathText.text = path
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.extract_to_btn -> {
                val act = requireActivity() as MainActivity
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FOLDER)
                act.switchScreens()
            }
            R.id.openFile -> {
                val act = requireActivity() as MainActivity
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FILE)
                act.switchScreens()
            }
            R.id.unRarBtn -> {
                processedFile.text = ""
                archiveData.text = ""
                if (path == null) {
                    Utils.showMsg(requireView(),R.string.no_file_selected)
                    return
                }
                if (extractionPath == null) {
                    Utils.showMsg(requireView(),R.string.no_directory_selected)
                    return
                }
                task = UnrarTask()
                task.execute(path, extractionPath)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::task.isInitialized && !task.isCancelled)
            task.cancel(true)
    }

    internal inner class UnrarTask : AsyncTask<String, String, Int>() {
        private lateinit var pd: AlertDialog
        private lateinit var unrar: Unrar
        var numOfItems = 1
        var progressUpdate = 0
        private lateinit var shakeAnimator: ObjectAnimator
        private lateinit var wl: WakeLock
        private val isCancelled = AtomicBoolean(false)
        override fun onPreExecute() {
            super.onPreExecute()
            if (!archiveData.isShown) {
                archiveData.startAnimation(scaleUp)
                archiveData.visibility = View.VISIBLE
            }
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":MyWakeLock")
            wl.acquire()
            shakeAnimator = ObjectAnimator.ofFloat(
                mainLogo, "rotation",
                -0.10f, 10f
            )
            with(shakeAnimator) {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 200
                start()
            }

            pd = buildProgressDialog {
                synchronized(this@MainScreenFragment){
                    isCancelled.set(true)
                }
            }.also {
                it.show()
            }
        }

        override fun onProgressUpdate(vararg values: String) {
            super.onProgressUpdate(*values)
            var archiveDataTxt = ""
            if (values.size == 1)
                processedFile.text = values[0]
            val yes = context!!.getString(R.string.yes)
            val no = context!!.getString(R.string.no)
            archiveDataTxt += if (unrar.archiveComment != null) {
                """
                        ${resources.getString(R.string.arcCmt)}: ${unrar.archiveComment}
                
                """.trimIndent()
            } else
                """
                        ${resources.getString(R.string.arcCmt)}:$no
                        
                """.trimIndent()
            archiveDataTxt += """
                        ${resources.getString(R.string.arcSld)}:${if (unrar.solid) yes else no}
                        
                        """.trimIndent()
            archiveDataTxt += """
                        ${resources.getString(R.string.arcSnd)}:${if (unrar.signed) yes else no}
                        
                        """.trimIndent()
            archiveDataTxt += """
                        ${resources.getString(R.string.arcRrp)}:${if (unrar.recoveryRecord) yes else no}
                        
                        """.trimIndent()
            archiveDataTxt += """
                        ${resources.getString(R.string.arcVol)}:${if (unrar.volume) yes else no}
                        
                        """.trimIndent()
            archiveDataTxt += resources.getString(R.string.arcLoc) + ":" + if (unrar.locked) yes else no
            archiveData.text = archiveDataTxt
        }

        override fun doInBackground(vararg params: String): Int {
            val rarFile = params[0]
            val extractionPath = params[1]
            unrar = Unrar()
            unrar.setCallBackListener(object : CallBackListener {
                override fun onFileProcessed(msgID: Int, filename: String) {
                    val message = when (msgID) {
                        Unrar.ERAR_SUCCESS -> resources.getString(R.string.processing) + ":" + filename +
                                if (numOfItems > 0) String.format(
                                    " %.2f %s", 100 * (progressUpdate.toFloat() / numOfItems), "%"
                                ) else ""
                        Unrar.ERAR_BAD_DATA -> String.format(
                            "Error:unable to process %s File CRC error!",
                            filename
                        )
                        Unrar.ERAR_BAD_ARCHIVE ->
                            "Error:Volume is not valid RAR archive !"
                        Unrar.ERAR_UNKNOWN_FORMAT -> "Error:Unknown archive format !"
                        Unrar.ERAR_EOPEN -> "Error:Volume open error !"
                        Unrar.ERAR_ECREATE ->
                            String.format("Error:File create error! in: %s", filename)
                        Unrar.ERAR_ECLOSE ->
                            String.format("Error:File close error ! in: %s", filename)
                        Unrar.ERAR_EREAD ->
                            String.format("Error:Read error ! in: %s", filename)
                        Unrar.ERAR_EWRITE ->
                            String.format("Error:Write error ! in: %s", filename)
                        Unrar.ERAR_BAD_PASSWORD ->
                            String.format("Error:Bad Password error ! in: %s", filename)
                        else -> "Error:Unknown error !"
                    }
                    publishProgress(message)
                }

                override fun onPassWordRequired() {
                    val mainActivity = requireActivity() as MainActivity
                    mainActivity.runOnUiThread {
                        Log.d(TAG, "Asking for the password...")
                        showPasswordDialog(unrar)
                    }
                }

                override fun onDataProcessed(bytesProcessed: Int): Int {
                    Log.d(TAG, "Processed Bytes:$bytesProcessed")
                    return synchronized(this@UnrarTask) { if (isCancelled.get()) -1 else 1 }
                }
            })
            numOfItems = unrar.RarGetArchiveItems(rarFile)
            Log.d(TAG, "Archive Num of items is: $numOfItems")
            if (unrar.archiveComment != null)
                publishProgress("comment", unrar.archiveComment)
            return unrar.RarOpenArchive(rarFile, extractionPath)
        }

        override fun onPostExecute(result: Int) {
            super.onPostExecute(result)
            if(pd.isShowing)
                pd.dismiss()
            processedFile.setText(
                when (result) {
                    Unrar.ERAR_SUCCESS -> R.string.unrardone
                    Unrar.ERAR_BAD_DATA -> R.string.filecrcerr
                    Unrar.ERAR_BAD_ARCHIVE -> R.string.filenotvaliderr
                    Unrar.ERAR_UNKNOWN_FORMAT -> R.string.archiveformaterr
                    Unrar.ERAR_EOPEN -> R.string.archiveopenerr
                    Unrar.ERAR_ECREATE -> R.string.filecreaterr
                    Unrar.ERAR_ECLOSE -> R.string.filecloserr
                    Unrar.ERAR_EREAD -> R.string.readerr
                    Unrar.ERAR_EWRITE -> R.string.writerr
                    Unrar.ERAR_BAD_PASSWORD -> R.string.bad_pass_err
                    else -> R.string.archiveunknownerr
                }
            )
            if (unrar.volume && !unrar.firstVolume) processedFile.text = """
                ${processedFile.text}
                ${resources.getString(R.string.notfirstvol)}
            """.trimIndent()
            if (wl.isHeld) {
                Log.d(TAG, "Releasing WakeLock...")
                wl.release()
            }
            shakeAnimator.cancel()
            mainLogo.rotation = 0f
        }

        override fun onCancelled() {
            super.onCancelled()
            isCancelled.set(true)
        }

        override fun onCancelled(result: Int?) {
            super.onCancelled(result)
            isCancelled.set(true)
        }
    }

    private fun showPasswordDialog(unrar: Unrar) {
        val root = LayoutInflater.from(context).inflate(R.layout.password_dialog, null)
        val passField = root.findViewById<EditText>(R.id.password_field)
        val showHideCheck = root.findViewById<AppCompatCheckBox>(R.id.pass_vis_check)
        val passOkBtn = root.findViewById<AppCompatButton>(R.id.passOkBtn)
        val passCancelBtn = root.findViewById<AppCompatButton>(R.id.passCancelBtn)
        lateinit var dialog: AlertDialog
        showHideCheck.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            passField.transformationMethod = if (isChecked) {
                // show password
                HideReturnsTransformationMethod.getInstance()
            } else {
                // hide password
                PasswordTransformationMethod.getInstance()
            }
        }
        passOkBtn.setOnClickListener {
            val pass = passField.text.toString()
            if (pass.trim().isNotEmpty()) {
                val act =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                act.hideSoftInputFromWindow(passField.windowToken, 0)
                unrar.setPassWord(pass)
                dialog.dismiss()
            } else {
                Snackbar.make(requireView(), "Invalid Password", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
        passCancelBtn.setOnClickListener {
            unrar.setPassWord(null)
            dialog.dismiss()
        }
        val builder = AlertDialog.Builder(requireContext())
        dialog = builder.setTitle(R.string.password)
            .setCancelable(false)
            .setView(root)
            .setOnDismissListener {
                Log.d(TAG, "Dialog Dismissed....")
                if (!unrar.isPassWordSet) {
                    unrar.setPassWord(null)
                }
            }
            .show()
    }

    companion object {
        private val TAG = MainScreenFragment::class.java.simpleName
    }
}