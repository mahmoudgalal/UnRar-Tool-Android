/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool

import android.Manifest
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aroma.unrartool.fragments.FileBrowserFragment
import androidx.core.app.ActivityCompat
import android.os.Bundle
import com.aroma.unrartool.fragments.StartScreenFragment
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.aroma.unrartool.utils.Utils.showMsg

class MainActivity : AppCompatActivity() {
    private var lastRequestedBrowseMode = FileBrowserFragment.BROWSE_MODE_FILE
    private var fileBrowserShown = false
    private val handler = Handler()
    var permissionRunnable = Runnable {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            EXTERNAL_PERMS,
            MY_PERMISSIONS_REQUEST
        )
    }
    private var splash: Splash? = null
    private lateinit var mainContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val myToolbar = findViewById<Toolbar>(R.id.main_bar)
        mainContainer = findViewById(R.id.main_container)
        setSupportActionBar(myToolbar)
        if (savedInstanceState == null) {
            val startScreenFragment: Fragment = StartScreenFragment()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container, startScreenFragment,
                    StartScreenFragment::class.java.simpleName
                )
                .commit()
        }
        splash = Splash(this) {}
        addContentView(
            splash, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun switchScreens() {
        if (fileBrowserShown) {
            supportFragmentManager.popBackStack()
            fileBrowserShown = false
            return
        }
        if (!checkStoragePermissionAndRequest()) return
        openFileBrowser()
    }

    private fun openFileBrowser() {
        val fragment = FileBrowserFragment()
        val args = Bundle()
        args.putInt(FileBrowserFragment.BROWSE_MODE_KEY, lastRequestedBrowseMode)
        fragment.arguments = args
        supportFragmentManager.beginTransaction().setCustomAnimations(
            R.animator.screen_flip_left_in,
            R.animator.screen_flip_left_out,
            R.animator.screen_flip_right_in,
            R.animator.screen_flip_right_out
        ).replace(R.id.main_container, fragment, FileBrowserFragment::class.java.simpleName)
            .addToBackStack(null)
            .commit()
        fileBrowserShown = true
    }

    fun setSelectedFile(path: String?) {
        val fragment = supportFragmentManager.findFragmentByTag(
            StartScreenFragment::class.java.simpleName
        ) as StartScreenFragment?
        if (fragment != null) fragment.openedArchivePath = path
    }

    fun setSelectedExtractionPath(path: String?) {
        val fragment = supportFragmentManager.findFragmentByTag(
            StartScreenFragment::class.java.simpleName
        ) as StartScreenFragment?
        fragment?.setSelectedExtractionPath(path)
    }

    fun setFileBrowserMode(mode: Int) {
        lastRequestedBrowseMode = mode
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
            fileBrowserShown = false
        } else {
            handler.removeCallbacks(permissionRunnable)
            splash!!.closeSplash { finish() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            R.id.menu_share -> {
                run { share() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.aboutmsg))
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // read/write permission was granted,open file browser
                    openFileBrowser()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showMsg(mainContainer, R.string.permission_denied)
                }
            }
        }
    }

    private fun checkStoragePermissionAndRequest(): Boolean {

        // For Android 11 (API 30) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission has already been granted
                return true
            } else {
                // Permission is not granted, guide user to settings
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                return false
            }
        } else {
            // Permission is not granted yet
            if (ContextCompat.checkSelfPermission(this, EXTERNAL_PERMS[0])
                != PackageManager.PERMISSION_GRANTED
            ) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        EXTERNAL_PERMS[0]
                    )
                ) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage(R.string.permission_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            permissionRunnable.run()
                        }.show()
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(
                        this,
                        EXTERNAL_PERMS,
                        MY_PERMISSIONS_REQUEST
                    )
                }
                return false
            }
            return true
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(permissionRunnable)
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)

        // Add data to the intent, the receiving app will decide what to do with it.
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        intent.putExtra(Intent.EXTRA_TEXT, "Unrar Tool is WooW!")
        startActivity(Intent.createChooser(intent, getString(R.string.sharehdr)))
    }

    companion object{
        private val EXTERNAL_PERMS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        private const val MY_PERMISSIONS_REQUEST = 20101
    }
}