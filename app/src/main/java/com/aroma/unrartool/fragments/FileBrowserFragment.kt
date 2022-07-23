/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool.fragments

import android.app.AlertDialog
import com.aroma.unrartool.adapters.FileListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.aroma.unrartool.R
import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.aroma.unrartool.MainActivity
import android.widget.EditText
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.aroma.unrartool.model.FileEntry
import com.aroma.unrartool.viewmodels.FilebrowserViewModel
import java.io.File

class FileBrowserFragment : Fragment(), FileListAdapter.OnItemClickListener {
    private lateinit var upBtn: Button
    private lateinit var newFolder: Button
    private lateinit var okBtn: Button
    private lateinit var fileList: RecyclerView
    private lateinit var listHeader: TextView
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var fileEntries: MutableList<FileEntry>
    private var browseMode = BROWSE_MODE_FILE
    private var rootPath: String? = null
    private val viewModel: FilebrowserViewModel by viewModels()
    private lateinit var progressView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        browseMode = args!!.getInt(BROWSE_MODE_KEY, BROWSE_MODE_FILE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.file_browser, container, false)
        init(root)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeList()
    }

    private fun init(root: View) {
        okBtn = root.findViewById(R.id.okBtn)
        fileList = root.findViewById(R.id.filelist)
        upBtn = root.findViewById(R.id.path_up)
        newFolder = root.findViewById(R.id.new_folder)
        progressView = root.findViewById(R.id.progress_circular)
        rootPath = Environment.getExternalStorageDirectory().path
        okBtn.visibility = if (browseMode == BROWSE_MODE_FOLDER) {
            View.VISIBLE
        } else {
            View.GONE
        }
        listHeader = root.findViewById(R.id.listheader)
        listHeader.text = ""
        fileEntries = ArrayList()
        val linearLayoutManager = LinearLayoutManager(
            getContext(),
            LinearLayoutManager.VERTICAL, false
        )
        val mDividerItemDecoration = DividerItemDecoration(
            fileList.context,
            linearLayoutManager.orientation
        )
        fileList.setHasFixedSize(true)
        fileList.addItemDecoration(mDividerItemDecoration)
        fileList.layoutManager = linearLayoutManager
        fileListAdapter = FileListAdapter(fileEntries, browseMode, this)
        fileList.setAdapter(fileListAdapter)
        upBtn.setOnClickListener { v: View? ->
            viewModel.loadUpDirectory()
        }
        newFolder.setOnClickListener { showNewFolderDialog() }
        okBtn.setOnClickListener { v: View? ->
            val currentFile = File(viewModel.currentPath.value)
            if (!currentFile.isDirectory) return@setOnClickListener
            if (currentFile.canWrite()) {
                val act = activity as MainActivity
                act.setSelectedExtractionPath(viewModel.currentPath.value)
                act.switchScreens()
            }
        }
    }

    private fun initializeList() {
        rootPath?.let {
            viewModel.setInitialRoot(it)
            viewModel.loadDirectory(Environment.getExternalStorageDirectory().absolutePath)
        }
        viewModel.filesList.observe(this.viewLifecycleOwner) {
            with(fileListAdapter) {
                setItems(it)
                notifyDataSetChanged()
            }
        }
        viewModel.loading.observe(this.viewLifecycleOwner) {
            progressView.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.currentPath.observe(this.viewLifecycleOwner) {
            if (it.isBlank()) return@observe
            val mCurrentDir = File(it)
            listHeader.text =
                if (mCurrentDir.name.compareTo("") == 0) mCurrentDir
                    .path else mCurrentDir.name
            with(mCurrentDir.canWrite()) {
                newFolder.isEnabled = this
                okBtn.isEnabled = this
            }
        }
    }

    fun setBrowsingMode(mode: Int) {
        browseMode = mode
        okBtn.visibility = if (browseMode == BROWSE_MODE_FOLDER) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onItemClicked(view: View, fileEntry: FileEntry) {
        if (fileEntry.file.isDirectory) {
            viewModel.loadDirectory(fileEntry.file.absolutePath)
        } else {
            if (fileEntry.file.canRead()) {
                val filename = fileEntry.file.name
                if (filename.endsWith("rar", true)) {
                    val act = requireActivity() as MainActivity
                    act.setSelectedFile(fileEntry.file.absolutePath)
                    act.switchScreens()
                }
            }
        }
    }

    override fun onItemLongClicked(view: View, fileEntry: FileEntry): Boolean {
        return false
    }

    private fun showNewFolderDialog() {
        val folderNameTxt = EditText(context)
        folderNameTxt.isSingleLine = true
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.newfolderdialogtitle)
            .setCancelable(false)
            .setPositiveButton("OK") { _: DialogInterface?, i: Int ->
                val newFolderName = folderNameTxt.text.toString().trim()
                if (newFolderName.isNotEmpty()) {
                    val mCurrentDir =
                        File(viewModel.currentPath.value + File.separator + newFolderName)
                    Log.d(TAG, "Creating Directory:  ${mCurrentDir.name}")
                    if (!mCurrentDir.exists()) {
                        if (!mCurrentDir.mkdirs()) {
                            Toast.makeText(
                                context, R.string.dircreationerr, Toast.LENGTH_LONG
                            ).show()
                        } else {
                            viewModel.refreshCurrentDirectory()
                        }
                    } else {
                        Toast.makeText(
                            context, R.string.direxists, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface?, i: Int -> }
            .setView(folderNameTxt)
            .show()
    }

    companion object {
        const val TAG = "FileBrowserFragment"
        const val BROWSE_MODE_FILE = 0
        const val BROWSE_MODE_FOLDER = 1
        const val BROWSE_MODE_KEY = "com.aroma.unrartool.FileBrowserFragment_BROWSE_MODE_KEY"
        const val FILE_PICKED = "com.aroma.unrartool.FileBrowserFragment_file_picked"
    }
}