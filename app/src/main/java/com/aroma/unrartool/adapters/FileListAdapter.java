/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aroma.unrartool.R;
import com.aroma.unrartool.model.FileEntry;

import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private List<FileEntry> items;
    private OnItemClickListener onItemClickListener;
    private int browseMode;

    public FileListAdapter(List<FileEntry> items, int browseMode, OnItemClickListener listener) {
        this.browseMode = browseMode;
        this.items = items;
        onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.list_item, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FileEntry fe = items.get(position);
        holder.initialize(fe.getFileName(), fe.isDirectory() ?
                ViewHolder.FILE_TYPE_FOLDER :
                ViewHolder.FILE_TYPE_FILE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClicked(view, fe);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (onItemClickListener != null) {
                    return onItemClickListener.onItemLongClicked(view, fe);
                }
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClicked(View view, FileEntry fileEntry);

        boolean onItemLongClicked(View view, FileEntry fileEntry);
    }

    public void setItems(List<FileEntry> items) {
        this.items = items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView fileName = null;
        final static int FILE_TYPE_FOLDER = 1;
        final static int FILE_TYPE_FILE = 2;

        public ViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
        }

        public void initialize(String name, int type) {
            fileName.setText(name);
            if (type == FILE_TYPE_FOLDER)
                fileName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.openfoldericon, 0, 0, 0);
            else
                fileName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }
}
