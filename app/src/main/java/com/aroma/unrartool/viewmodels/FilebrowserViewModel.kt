package com.aroma.unrartool.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aroma.unrartool.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilebrowserViewModel : ViewModel() {
    private val _filesList: MutableLiveData<List<FileEntry>> = MutableLiveData()
    val filesList: LiveData<List<FileEntry>> = _filesList

    private val _loading: MutableLiveData<Boolean> = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _currentPath: MutableLiveData<String> = MutableLiveData("")
    val currentPath: LiveData<String> = _currentPath


    lateinit var rootPath: String
        private set

    fun setInitialRoot(root: String) {
        rootPath = root
    }

    fun loadDirectory(location: String) {
        viewModelScope.launch {
            _loading.value = true
            val entries = browseTo(location)
            _currentPath.value = location
            _filesList.value = entries
            _loading.value = false
        }
    }

    fun loadUpDirectory() {
        val file = File(_currentPath.value)
        val parent = file.parent ?: return
        if (!file.path.equals(rootPath, ignoreCase = true)) {
            loadDirectory(parent)
        }
    }

    fun refreshCurrentDirectory() {
        _currentPath.value?.let { loadDirectory(it) }
    }

    private suspend fun browseTo(location: String): List<FileEntry> = withContext(Dispatchers.IO) {
        val mCurrentDir = File(location)
        val fl = mutableListOf<FileEntry>()
        mCurrentDir.parentFile?.let {
            if (mCurrentDir.path != rootPath) {
                val fentry = FileEntry(it)
                fl += fentry
            }
        }
        val files = mCurrentDir.listFiles()
        if (files != null)
            for (file in files) {
                val fentry = FileEntry(file, file.isDirectory)
                fl += fentry
            }
        fl
    }
}