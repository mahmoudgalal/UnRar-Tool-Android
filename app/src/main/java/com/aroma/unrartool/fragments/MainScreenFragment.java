/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool.fragments;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils.TruncateAt;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aroma.unrartool.MainActivity;
import com.aroma.unrartool.R;
import com.aroma.unrartool.Unrar;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MainScreenFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = MainScreenFragment.class.getSimpleName();
    private Button unrarBtn = null, openArchive = null, extractToBtn;
    private TextView openedFile = null, processedFile = null, extractionPathText = null,
            archiveData = null;
    private String path = null, extractionPath = null;
    private Animation scaleUp = null;
    private ImageView mainLogo;
    private ObjectAnimator shakeAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.main_screen, container, false);
        init(getContext(), root);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (extractionPath != null) {
            extractionPathText.setText(extractionPath);
        }
        if (path != null) {
            openedFile.setText(path);
            unrarBtn.setEnabled(true);
            archiveData.setText("");
        }
    }

    private void init(Context context, View root) {
        mainLogo = root.findViewById(R.id.main_logo);

        openedFile = root.findViewById(R.id.archive_name_txt);
        openedFile.setSelected(true);
        openedFile.setEllipsize(TruncateAt.MARQUEE);

        processedFile = root.findViewById(R.id.progress_box);

        extractionPathText = root.findViewById(R.id.extractionpath);
        extractionPathText.setSelected(true);
        extractionPathText.setEllipsize(TruncateAt.MARQUEE);

        extractToBtn = root.findViewById(R.id.extract_to_btn);
        extractToBtn.setOnClickListener(this);

        archiveData = root.findViewById(R.id.archivedata);
        archiveData.setMovementMethod(new ScrollingMovementMethod());
        archiveData.setVisibility(INVISIBLE);

        unrarBtn = root.findViewById(R.id.okBtn);
        unrarBtn.setOnClickListener(this);

        openArchive = root.findViewById(R.id.openFile);
        openArchive.setOnClickListener(this);

        scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up);
    }

    public void setOpenedArchivePath(String path) {
        this.path = path;
        openedFile.setText(path);
        unrarBtn.setEnabled(true);
        archiveData.setText("");
    }

    public String getOpenedArchivePath() {
        return path;
    }

    public void setSelectedExtractionPath(String path) {
        extractionPath = path;
        extractionPathText.setText(path);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.extract_to_btn: {
                MainActivity act = ((MainActivity) getContext());
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FOLDER);
                act.switchScreens();
            }
            break;
            case R.id.openFile:
                MainActivity act = ((MainActivity) getContext());
                act.setFileBrowserMode(FileBrowserFragment.BROWSE_MODE_FILE);
                act.switchScreens();
                break;
            case R.id.okBtn:
                if (path == null) {
                    Toast.makeText(getContext(), "Please select a Rar file !(Step :1)",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (extractionPath == null) {
                    Toast.makeText(getContext(), "Please select extraction directory !(Step :2)",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                UnrarTask task = new UnrarTask();
                task.execute(path, extractionPath);

                break;
        }
    }


    class UnrarTask extends AsyncTask<String, String, Void> {
        ProgressDialog pd = null;
        Unrar ur = null;
        int numOfItems = 1, progressUpdate = 0;
        int unrarResult = 0;
        PowerManager.WakeLock wl = null;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            if (!archiveData.isShown()) {

                archiveData.startAnimation(scaleUp);
                archiveData.setVisibility(VISIBLE);

            }
            PowerManager pm = (PowerManager) MainScreenFragment.this.getContext().
                    getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":MyWakeLock");
            wl.acquire();

            shakeAnimator = ObjectAnimator.ofFloat(mainLogo, "rotation",
                    -0.10f,10f);
            shakeAnimator.setRepeatCount(ValueAnimator.INFINITE);
            shakeAnimator.setRepeatMode(ValueAnimator.REVERSE);
            shakeAnimator.setDuration(200);
            shakeAnimator.start();

            pd = ProgressDialog.show(MainScreenFragment.this.getContext(), ""
                    , getContext().getString(R.string.wait), true, false);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            if (values != null && values.length == 1)
                processedFile.setText(values[0]);
            else {
                if (values == null) {
                    String yes = getContext().getString(R.string.yes);
                    String no = getContext().getString(R.string.no);
                    String archiveDataTxt = "";
                    archiveDataTxt += getResources().getString(R.string.arcCmt) + ":"/*"Archive Comment:"*/ + no/*"N/A"*/ + "\n";
                    archiveDataTxt += getResources().getString(R.string.arcSld) + ":"/*"Archive Solid:"*/ + (ur.solid ? yes : no) + "\n";
                    archiveDataTxt += getResources().getString(R.string.arcSnd) + ":"/*"Archive Signed:"*/ + (ur.signed ? yes : no) + "\n";
                    archiveDataTxt += getResources().getString(R.string.arcRrp) + ":"/*"Recovery Record Present:"*/ + (ur.recoveryRecord ? yes : no) + "\n";
                    archiveDataTxt += getResources().getString(R.string.arcVol) + ":"/*"Is Volume:"*/ + (ur.volume ? yes : no) + "\n";
                    archiveDataTxt += getResources().getString(R.string.arcLoc) + ":"/*"Archive Locked:"*/ + (ur.locked ? yes : no);
                    archiveData.setText(archiveDataTxt);
                } else
                    archiveData.setText(getResources().getString(R.string.arcCmt) + ": "/*"Archive Comment: "*/ + values[1]);
            }
        }

        @Override
        protected Void doInBackground(String... params) {
            ur = new Unrar();
            ur.setCallBackListener(new Unrar.CallBackListener() {
                String message = "";

                @Override
                public void onFileProcessed(int msgID, String filename) {
                    if (msgID == 0) {
                        progressUpdate++;
                        message = getResources().getString(R.string.processing) + ":" + filename +
                                (numOfItems > 0 ?
                                        String.format(" %.2f %s", (100 * ((float) (progressUpdate) / numOfItems))
                                                , "%") : "");

                        UnrarTask.this.publishProgress(message);
                    } else {
                        switch (msgID) {
                            case Unrar.ERAR_BAD_DATA:
                                message = String.format("Error:unable to process %s File CRC error!", filename);
                                break;
                            case Unrar.ERAR_BAD_ARCHIVE:
                                message = "Error:Volume is not valid RAR archive !";
                                break;
                            case Unrar.ERAR_UNKNOWN_FORMAT:
                                message = "Error:Unknown archive format !";
                                break;
                            case Unrar.ERAR_EOPEN:
                                message = "Error:Volume open error !";
                                break;
                            case Unrar.ERAR_ECREATE:
                                message = String.format("Error:File create error! in: %s", filename);
                                break;
                            case Unrar.ERAR_ECLOSE:
                                message = String.format("Error:File close error ! in: %s", filename);
                                break;
                            case Unrar.ERAR_EREAD:
                                message = String.format("Error:Read error ! in: %s", filename);
                                break;
                            case Unrar.ERAR_EWRITE:
                                message = String.format("Error:Write error ! in: %s", filename);
                                break;
                        }
                        UnrarTask.this.publishProgress(message);
                    }
                }

                @Override
                public void onPassWordRequired() {

                    MainActivity mact = (MainActivity) MainScreenFragment.this.getContext();
                    mact.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Log.d(TAG, "Asking for the password...");
                            showPasswordDialog(ur);
                        }
                    });
                }
            });
            numOfItems = ur.RarGetArchiveItems(params[0]);
            Log.d(TAG, "Archive Num of items is:" + numOfItems);
            if (ur.archiveComment != null)
                publishProgress("", ur.archiveComment);
            if (!ur.commentPresent)
                publishProgress((String[]) null);
            unrarResult = ur.RarOpenArchive(params[0], params[1]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if (pd != null)
                pd.dismiss();
            if (unrarResult == 0)
                processedFile.setText(getResources().getString(R.string.unrardone));//Unrar done !");
            else {
                switch (unrarResult) {
                    case Unrar.ERAR_BAD_DATA:
                        processedFile.setText(R.string.filecrcerr);//"Error:File CRC error !");
                        break;
                    case Unrar.ERAR_BAD_ARCHIVE:
                        processedFile.setText(R.string.filenotvaliderr);//"Error:File is not valid RAR archive !");
                        break;
                    case Unrar.ERAR_UNKNOWN_FORMAT:
                        processedFile.setText(R.string.archiveformaterr);//"Error:Unknown archive format !");
                        break;
                    case Unrar.ERAR_EOPEN:
                        processedFile.setText(R.string.archiveopenerr);//"Error:Archive open error !");
                        break;
                    case Unrar.ERAR_ECREATE:
                        processedFile.setText(R.string.filecreaterr);//"Error:File create error !");
                        break;
                    case Unrar.ERAR_ECLOSE:
                        processedFile.setText(R.string.filecloserr);//"Error:File close error !");
                        break;
                    case Unrar.ERAR_EREAD:
                        processedFile.setText(R.string.readerr);//"Error:Read error !");
                        break;
                    case Unrar.ERAR_EWRITE:
                        processedFile.setText(R.string.writerr);//"Error:Write error !");
                        break;
                }
            }
            if (ur.volume && !ur.firstVolume)
                processedFile.setText(processedFile.getText() + "\n" +
                        getResources().getString(R.string.notfirstvol));
            if (wl != null && wl.isHeld()) {
                Log.d(TAG, "Releasing WakeLock...");
                wl.release();
            }
            shakeAnimator.cancel();
            mainLogo.setRotation(0.f);
        }
    }

    private void showPasswordDialog(final Unrar unrar) {

        View root = LayoutInflater.from(getContext()).inflate(R.layout.password_dialog, null);
        final EditText passField = root.findViewById(R.id.password_field);
        final AppCompatCheckBox showHideCheck = root.findViewById(R.id.pass_vis_check);

        showHideCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    // show password
                    passField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    // hide password
                    passField.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Password:").setCancelable(false).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        unrar.setPassWord(null);
                    }
                }).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String pass = passField.getText().toString();
                if (pass != null && pass.length() > 0) {
                    InputMethodManager act = (InputMethodManager) MainScreenFragment.this.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (act != null)
                        act.hideSoftInputFromWindow(passField.getWindowToken()
                                , 0);
                    unrar.setPassWord(pass);

                }
            }
        }).setView(root)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        System.out.println("Dialog Dismissed....");
                        if (!unrar.isPassWordSet()) {
                            unrar.setPassWord(null);
                        }
                    }
                })
                .show();
    }
}
