/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool.model

import java.io.File

/**
 * file browser list item
 */
data class FileEntry(var file: File, var isDirectory:Boolean = false) {
    val absolutePath: String = file.absolutePath
    val fileName: String = file.name
}