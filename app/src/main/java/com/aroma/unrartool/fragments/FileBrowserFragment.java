/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aroma.unrartool.MainActivity;
import com.aroma.unrartool.R;
import com.aroma.unrartool.adapters.FileListAdapter;
import com.aroma.unrartool.model.FileEntry;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class FileBrowserFragment extends Fragment implements FileListAdapter.OnItemClickListener {

	private Button upBtn=null,newFolder=null,okBtn=null;
	private RecyclerView fileList = null;
	private String currentPath=null;
	private TextView listHeader=null;
	private FileListAdapter fileListAdapter=null;
	private ArrayList<FileEntry> fileEntries=null;
	public static final int BROWSE_MODE_FILE = 0,
			BROWSE_MODE_FOLDER=1;
	public static final String BROWSE_MODE_KEY =
			"com.aroma.unrartool.FileBrowserFragment_BROWSE_MODE_KEY";
	private int browseMode = BROWSE_MODE_FILE;
	private boolean firstTime = false;
	private String rootPath;
	public static final String FILE_PICKED = "com.aroma.unrartool.FileBrowserFragment_file_picked";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		browseMode = args.getInt(BROWSE_MODE_KEY,BROWSE_MODE_FILE);
	}

	@Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.file_browser , container, false);
        init(getContext(),root);
        return root ;
    }

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initializeList();
	}

	private void init(Context context, View root){
		okBtn = root.findViewById(R.id.okBtn);
        fileList =  root.findViewById(R.id.filelist);
        upBtn  = root.findViewById(R.id.path_up);
        newFolder = root.findViewById(R.id.new_folder);
		rootPath = Environment.getExternalStorageDirectory().getPath();
		if(browseMode == BROWSE_MODE_FOLDER)
		{
			okBtn.setVisibility(VISIBLE);
		}
		else
		{
			okBtn.setVisibility(GONE);
		}
        listHeader = root.findViewById(R.id.listheader);
        listHeader.setText("");
        fileEntries = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
				LinearLayoutManager.VERTICAL,false);
		DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(fileList.getContext(),
				linearLayoutManager.getOrientation());

		fileList.setHasFixedSize(true);
		fileList.addItemDecoration(mDividerItemDecoration);
		fileList.setLayoutManager(linearLayoutManager);
        fileListAdapter = new FileListAdapter(fileEntries,browseMode,this);
        fileList.setAdapter(fileListAdapter);

        upBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File file=new File(currentPath);
                if(file.getParent()!=null && !file.getPath().equalsIgnoreCase(rootPath))
                {
                    LoadingTask task=new LoadingTask();
                    task.execute(file.getParent());
                }
            }
        });

        newFolder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
				showNewFolderDialog();
            }
        });
		okBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				File currentFile=new File(currentPath);
				if(!currentFile.isDirectory())
					return;
				if(currentFile.canWrite())
				{
					MainActivity act=((MainActivity)getContext());
					act.setSelectedExtractionPath(currentPath);
					act.switchScreens();
				}
			}
		});
    }

	public void initializeList()
	{
		LoadingTask task = new LoadingTask();
		task.execute(Environment.getExternalStorageDirectory().getAbsolutePath());
		
	}
	public void setBrowsingMode(int mode)
	{
		browseMode=mode;
		if(browseMode == BROWSE_MODE_FOLDER)
		{			
			okBtn.setVisibility(VISIBLE);
		}
		else
		{
			okBtn.setVisibility(GONE);
		}
	}
	
	public ArrayList<FileEntry> browseTo(String location)
	{
		currentPath = location;
		File mCurrentDir = new File(location);

		ArrayList<FileEntry> fl = new ArrayList<FileEntry>();
		
		if (mCurrentDir.getParentFile() != null)
		{
			if(!mCurrentDir.getPath().equals(rootPath)) {
				FileEntry fentry = new FileEntry();
				fentry.setFile(mCurrentDir.getParentFile());
				fl.add(fentry);
			}
		}
		File files[]= mCurrentDir.listFiles();
		if(files != null)
		for (File file :files) 
		{
			if (file.isDirectory()) {
				FileEntry fentry=new FileEntry();
				fentry.setDirectory(true);
				fentry.setFile(file);
				fl.add(fentry);
			} 
			else 
			{
				FileEntry fentry=new FileEntry();
				fentry.setDirectory(false);
				fentry.setFile(file);
				fl.add(fentry);
			}
		}
		return fl;
	}

	@Override
	public void onItemClicked(View view, FileEntry fileEntry) {
		File file = new File(rootPath).getParentFile();
		File parent = fileEntry.getFile().getParentFile();
		if (fileEntry.getFile().isDirectory() ) {
			LoadingTask task = new LoadingTask();
			task.execute(fileEntry.getFile().getAbsolutePath());
		}
		else
		{
			if(fileEntry.getFile().canRead())
			{
				String filename=fileEntry.getFile().getName();
				if(filename.endsWith("rar"))
				{
					MainActivity act=((MainActivity)getContext());
					act.setSelectedFile(fileEntry.getFile().getAbsolutePath());
					act.switchScreens();
				}
			}
		}
	}

	@Override
	public boolean onItemLongClicked(View view, FileEntry fileEntry) {
		return false;
	}

	class LoadingTask extends AsyncTask<String , Void, Void>
	{
		private ArrayList<FileEntry> fe = new ArrayList<>();
		private ProgressDialog pd=null;
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			
			fileEntries.clear();
			fileListAdapter.notifyDataSetChanged();
			if(firstTime)				
			   pd=ProgressDialog.show(getContext(), "",getResources().getString(R.string.loading), true, false);
			firstTime=true;
		}
		@Override
		protected Void doInBackground(String... params) {
			fileEntries = browseTo(params[0]);
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			File mCurrentDir=new File(currentPath);
			if(listHeader != null)
			listHeader.setText(mCurrentDir.getName().compareTo("") == 0 ? mCurrentDir
					.getPath() : mCurrentDir.getName());
			if(fileList != null)
			{
				fileListAdapter.setItems(fileEntries);
				fileListAdapter.notifyDataSetChanged();
			}
			if(mCurrentDir.canWrite())
			{
				newFolder.setEnabled(true);
				okBtn.setEnabled(true);
			}
			else
			{
				newFolder.setEnabled(false);
				okBtn.setEnabled(false);
			}
				
			if(pd != null)
				pd.dismiss();			
		}
		
	}

	private void showNewFolderDialog(){
		final EditText folderNameTxt = new EditText(getContext());
		folderNameTxt.setSingleLine(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.newfolderdialogtitle)
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						String newFolderName = folderNameTxt.getText().toString().trim();
						if(newFolderName.length()>0)
						{
							File mCurrentDir=new File(currentPath+File.separator+newFolderName);
							System.out.printf("Creating Directory:%s \n",mCurrentDir.getName());
							if(!mCurrentDir.exists())
							{
								if(!mCurrentDir.mkdirs())
									Toast.makeText(getContext(), R.string.dircreationerr
											, Toast.LENGTH_LONG).show();
								else
								{
									FileEntry fentry = new FileEntry();
									fentry.setDirectory(true) ;
									fentry.setFile(mCurrentDir);
									fileEntries.add(fentry);
									fileListAdapter.notifyDataSetChanged();
								}
							}
							else
							{
								Toast.makeText(getContext(),  R.string.direxists
										, Toast.LENGTH_LONG).show();
							}
						}
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

					}
				})
				.setView(folderNameTxt)
				.show();
	}
}
