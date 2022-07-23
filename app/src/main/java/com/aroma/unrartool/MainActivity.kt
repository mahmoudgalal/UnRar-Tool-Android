/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */
package com.aroma.unrartool

import android.Manifest
import android.app.AlertDialog
import com.aroma.unrartool.utils.Utils.checkAllFilesAccess
import androidx.appcompat.app.AppCompatActivity
import com.aroma.unrartool.fragments.FileBrowserFragment
import androidx.core.app.ActivityCompat
import android.os.Bundle
import com.aroma.unrartool.fragments.MainScreenFragment
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val myToolbar = findViewById<Toolbar>(R.id.main_bar)
        setSupportActionBar(myToolbar)
        if (savedInstanceState == null) {
            val mainScreenFragment: Fragment = MainScreenFragment()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container, mainScreenFragment,
                    MainScreenFragment::class.java.simpleName
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
            MainScreenFragment::class.java.simpleName
        ) as MainScreenFragment?
        if (fragment != null) fragment.openedArchivePath = path
    }

    fun setSelectedExtractionPath(path: String?) {
        val fragment = supportFragmentManager.findFragmentByTag(
            MainScreenFragment::class.java.simpleName
        ) as MainScreenFragment?
        fragment?.setSelectedExtractionPath(path)
    }

    fun setFileBrowserMode(mode: Int) {
        lastRequestedBrowseMode = mode
    }

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
        // TODO Auto-generated method stub
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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
                    if(checkAllFilesAccess(this))
                        openFileBrowser()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private fun checkStoragePermissionAndRequest(): Boolean {

        // Here, thisActivity is the current activity
        return if (ContextCompat.checkSelfPermission(this, EXTERNAL_PERMS[0])
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
                Toast.makeText(
                    this, """
                    Please understand that you need to grant the app Read/Write Storage Permission
                    to be able to use it .Go to device settings and enable Storage Permission for
                     UnRar Tool.
                     """.trimIndent(),
                    Toast.LENGTH_LONG
                ).show()
                handler.postDelayed(permissionRunnable, 2600)
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this,
                    EXTERNAL_PERMS,
                    MY_PERMISSIONS_REQUEST
                )
            }
            false
        } else {
            // Permission has already been granted
            checkAllFilesAccess(this)
            //true
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
        private val MY_PERMISSIONS_REQUEST = 20101
    }
}