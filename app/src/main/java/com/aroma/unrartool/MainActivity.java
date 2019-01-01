/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aroma.unrartool.fragments.FileBrowserFragment;
import com.aroma.unrartool.fragments.MainScreenFragment;


public class MainActivity extends AppCompatActivity {

    private int lastRequestedBrowseMode = FileBrowserFragment.BROWSE_MODE_FILE;
    private boolean fileBrowserShown = false;
    private Handler handler =  new Handler();
    Runnable permissionRunnable = new Runnable() {
        @Override
        public void run() {
            ActivityCompat.requestPermissions(MainActivity.this,
                    EXTERNAL_PERMS,
                    MY_PERMISSIONS_REQUEST);
        }
    };
    private Splash splash = null;
    public final String[] EXTERNAL_PERMS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private final int MY_PERMISSIONS_REQUEST = 20101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Toolbar myToolbar = findViewById(R.id.main_bar);
        setSupportActionBar(myToolbar);
        if (savedInstanceState == null) {
            Fragment mainScreenFragment = new MainScreenFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, mainScreenFragment,
                            MainScreenFragment.class.getSimpleName())
                    .commit();
        }
        splash = new Splash(this, new Runnable() {
            @Override
            public void run() {

            }
        });
        addContentView(splash, new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT));
    }

    public void switchScreens() {
        if (fileBrowserShown) {
            getSupportFragmentManager().popBackStack();
            fileBrowserShown = false;
            return;
        }
        if (!checkStoragePermissionAndRequest())
            return;
        openFileBrowser();
    }

    private void openFileBrowser(){
        FileBrowserFragment fragment = new FileBrowserFragment();
        Bundle args = new Bundle();
        args.putInt(FileBrowserFragment.BROWSE_MODE_KEY, lastRequestedBrowseMode);
        fragment.setArguments(args);
        getSupportFragmentManager().
                beginTransaction().setCustomAnimations(
                R.animator.screen_flip_left_in,
                R.animator.screen_flip_left_out,
                R.animator.screen_flip_right_in,
                R.animator.screen_flip_right_out
        ).replace(R.id.main_container, fragment, FileBrowserFragment.class.getSimpleName())
                .addToBackStack(null)
                .commit();
        fileBrowserShown = true;
    }

    public void setSelectedFile(String path) {
        MainScreenFragment fragment = (MainScreenFragment) getSupportFragmentManager().
                findFragmentByTag(MainScreenFragment.class.
                        getSimpleName());
        if (fragment != null)
            fragment.setOpenedArchivePath(path);
    }

    public void setSelectedExtractionPath(String path) {
        MainScreenFragment fragment = (MainScreenFragment) getSupportFragmentManager().
                findFragmentByTag(MainScreenFragment.class.
                        getSimpleName());
        if (fragment != null)
            fragment.setSelectedExtractionPath(path);
    }

    public void setFileBrowserMode(int mode) {
        lastRequestedBrowseMode = mode;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            fileBrowserShown = false;
        } else {
            handler.removeCallbacks(permissionRunnable);
            splash.closeSplash(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.menu_about:
                showAboutDialog();
                return true;
            case R.id.menu_share: {
                share();
            }
            return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void showAboutDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.aboutmsg))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // read/write permission was granted,open file browser
                    openFileBrowser();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
        }
    }

    private boolean checkStoragePermissionAndRequest() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                EXTERNAL_PERMS[0])
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    EXTERNAL_PERMS[0])) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "Please understand that you need to grant " +
                                "the app Read/Write Storage Permission to be able to use it.\nGo" +
                                " to device settings and enable Storage Permission for UnRar Tool.",
                        Toast.LENGTH_LONG).show();
                handler.postDelayed(permissionRunnable,2600);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        EXTERNAL_PERMS,
                        MY_PERMISSIONS_REQUEST);

            }
            return false;
        } else {
            // Permission has already been granted
            return true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(permissionRunnable);
    }

    private void share() {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide what to do with it.
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, "Unrar Tool is WooW!");
        startActivity(Intent.createChooser(intent, getString(R.string.sharehdr)));
    }

}
