/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.aroma.unrartool.R
import android.widget.TextView
import com.aroma.unrartool.model.FileEntry

class FileListAdapter(
    private var items: List<FileEntry>,
    private val browseMode: Int,
    private var onItemClickListener: OnItemClickListener?
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fe = items[position]
        holder.initialize(
            fe.fileName,
            if (fe.isDirectory) ViewHolder.FILE_TYPE_FOLDER else ViewHolder.FILE_TYPE_FILE
        )
        holder.itemView.setOnClickListener { view ->
            onItemClickListener?.onItemClicked(view, fe)
        }
        holder.itemView.setOnLongClickListener { view ->
            onItemClickListener?.onItemLongClicked(view, fe) ?: false
        }
    }

    override fun getItemCount() = items.size

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, fileEntry: FileEntry)
        fun onItemLongClicked(view: View, fileEntry: FileEntry): Boolean
    }

    fun setItems(items: List<FileEntry>) {
        this.items = items
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.file_name)

        fun initialize(name: String?, type: Int) {
            fileName.text = name
            if (type == FILE_TYPE_FOLDER)
                fileName.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.openfoldericon,
                    0,
                    0,
                    0
                ) else
                fileName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }

        companion object {
            const val FILE_TYPE_FOLDER = 1
            const val FILE_TYPE_FILE = 2
        }
    }
}