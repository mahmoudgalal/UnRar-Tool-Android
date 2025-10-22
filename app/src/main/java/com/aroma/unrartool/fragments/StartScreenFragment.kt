package com.aroma.unrartool.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.text.TextUtils.TruncateAt
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aroma.unrartool.MainActivity
import com.aroma.unrartool.R
import com.aroma.unrartool.Unrar
import com.aroma.unrartool.Unrar.CallBackListener
import com.aroma.unrartool.utils.Utils
import com.aroma.unrartool.utils.Utils.buildProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StartScreenFragment : Fragment(), View.OnClickListener {
    private val TAG = javaClass.simpleName
    private var path: String? = null
    private var extractionPath: String? = null
    private lateinit var mainLogo: ImageView
    private lateinit var openedFile: TextView
    private lateinit var processedFile: TextView
    private lateinit var extractionPathText: TextView
    private lateinit var extractToBtn: Button
    private lateinit var archiveData: TextView
    private lateinit var unrarBtn: Button
    private lateinit var openArchive: Button
    private lateinit var scaleUp: Animation

    // Coroutine Job for the unrar task
    private var unrarJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.start_screen, container, false)
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
        val act = requireActivity() as MainActivity
        when (view.id) {
            R.id.extract_to_btn -> {
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FOLDER)
                act.switchScreens()
            }

            R.id.openFile -> {
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FILE)
                act.switchScreens()
            }

            R.id.unRarBtn -> {
                processedFile.text = ""
                archiveData.text = ""
                val currentPath = path
                val currentExtractionPath = extractionPath
                if (currentPath == null) {
                    Utils.showMsg(requireView(), R.string.no_file_selected)
                    return
                }
                if (currentExtractionPath == null) {
                    Utils.showMsg(requireView(), R.string.no_directory_selected)
                    return
                }
                // Launch the coroutine
                startUnrarTask(currentPath, currentExtractionPath)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The coroutine is automatically cancelled by lifecycleScope, but we can be explicit if needed.
        unrarJob?.cancel()
    }

    @SuppressLint("SetTextI18n")
    private fun startUnrarTask(rarFile: String, extractionPath: String) {
        // Cancel any previous job
        unrarJob?.cancel()

        unrarJob = viewLifecycleOwner.lifecycleScope.launch {
            // Setup for onPreExecute
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:MyWakeLock")
            val shakeAnimator = ObjectAnimator.ofFloat(mainLogo, "rotation", -0.10f, 10f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 200
            }
            val unrar = Unrar()
            lateinit var pd: Pair<AlertDialog, TextView>
            var result = Unrar.ERAR_SUCCESS

            try {
                withContext(Dispatchers.Main) {
                    if (!archiveData.isShown) {
                        archiveData.startAnimation(scaleUp)
                        archiveData.visibility = View.VISIBLE
                    }
                    wl.acquire(10 * 60 * 1000L /*10 minutes timeout*/)
                    shakeAnimator.start()
                    pd = buildProgressDialog {
                        unrarJob?.cancel()
                    }.also { it.first.show() }
                }

                // --- Start UnRar ---
                withContext(Dispatchers.IO) {
                    var numOfItems = 1
                    var progressUpdate = 0
                    var progressPercentage = 0.0
                    unrar.setCallBackListener(object : CallBackListener {

                        @SuppressLint("DefaultLocale")
                        override fun onFileProcessed(msgID: Int, filename: String) {
                            val message = when (msgID) {
                                Unrar.ERAR_SUCCESS -> {
                                    progressPercentage = 100 * ((++progressUpdate).toDouble() / numOfItems)
                                    val msg = getString(R.string.processing) + ":" + filename +
                                            if (numOfItems > 0) String.format(
                                                " %.2f %s",
                                                progressPercentage,
                                                "%"
                                            ) else ""
                                    Log.d(TAG, msg)
                                    msg
                                }

                                Unrar.ERAR_BAD_DATA -> String.format(
                                    "Error:unable to process %s File CRC error!",
                                    filename
                                )

                                Unrar.ERAR_BAD_ARCHIVE -> "Error:Volume is not valid RAR archive !"
                                Unrar.ERAR_UNKNOWN_FORMAT -> "Error:Unknown archive format !"
                                Unrar.ERAR_EOPEN -> "Error:Volume open error !"
                                Unrar.ERAR_ECREATE -> String.format(
                                    "Error:File create error! in: %s",
                                    filename
                                )

                                Unrar.ERAR_ECLOSE -> String.format(
                                    "Error:File close error ! in: %s",
                                    filename
                                )

                                Unrar.ERAR_EREAD -> String.format(
                                    "Error:Read error ! in: %s",
                                    filename
                                )

                                Unrar.ERAR_EWRITE -> String.format(
                                    "Error:Write error ! in: %s",
                                    filename
                                )

                                Unrar.ERAR_BAD_PASSWORD -> String.format(
                                    "Error:Bad Password error ! in: %s",
                                    filename
                                )

                                else -> "Error:Unknown error !"
                            }
                            // Update UI from background thread using launch
                            launch(Dispatchers.Main) {
                                processedFile.text = message
                                pd.second.text = String.format(
                                    "%s: %.2f %s",
                                    getString(R.string.processing),
                                    progressPercentage,
                                    "%"
                                )
                            }
                        }

                        override fun onPassWordRequired() {
                            launch(Dispatchers.Main) {
                                Log.d(TAG, "Asking for the password...")
                                showPasswordDialog(unrar)
                            }
                        }

                        override fun onDataProcessed(bytesProcessed: Int): Int {
                            Log.d(TAG, "Processed Bytes:$bytesProcessed")
                            // Check if the coroutine is active
                            val ret = if (isActive) 1 else -1
                            return ret
                        }
                    })

                    numOfItems = unrar.RarGetArchiveItems(rarFile)
                    Log.d(TAG, "Archive Num of items is: $numOfItems")
                    // Update UI for archive info
                    launch(Dispatchers.Main) {
                        updateArchiveInfo(unrar)
                    }
                    result = unrar.RarOpenArchive(rarFile, extractionPath)
                }

            } finally {
                // --- Final cleanup ---
                pd.first.dismiss()
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
        }
    }

    private fun updateArchiveInfo(unrar: Unrar) {
        val yes = requireContext().getString(R.string.yes)
        val no = requireContext().getString(R.string.no)
        val archiveDataTxt = buildString {
            append(
                if (unrar.archiveComment != null) {
                    """
                    ${resources.getString(R.string.arcCmt)}: ${unrar.archiveComment}
                    
                """.trimIndent()
                } else {
                    """
                    ${resources.getString(R.string.arcCmt)}:$no
                    
                """.trimIndent()
                }
            )
            append(
                """
                ${resources.getString(R.string.arcSld)}:${if (unrar.solid) yes else no}
                
            """.trimIndent()
            )
            append(
                """
                ${resources.getString(R.string.arcSnd)}:${if (unrar.signed) yes else no}
                
            """.trimIndent()
            )
            append(
                """
                ${resources.getString(R.string.arcRrp)}:${if (unrar.recoveryRecord) yes else no}
                
            """.trimIndent()
            )
            append(
                """
                ${resources.getString(R.string.arcVol)}:${if (unrar.volume) yes else no}
                
            """.trimIndent()
            )
            append(resources.getString(R.string.arcLoc) + ":" + if (unrar.locked) yes else no)
        }
        archiveData.text = archiveDataTxt
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
                HideReturnsTransformationMethod.getInstance()
            } else {
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
                passField.text.clear()
                Utils.showMsg(root, R.string.invalid_password)
            }
        }
        passCancelBtn.setOnClickListener {
            unrar.setPassWord(null)
            dialog.dismiss()
            unrarJob?.cancel() // Also cancel the task if user cancels password entry
        }
        dialog = AlertDialog.Builder(requireContext())
            .setView(root)
            .setCancelable(false)
            .create()
        dialog.show()
    }
}