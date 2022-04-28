package com.algirm.arling.ui.splash

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.algirm.arling.databinding.ActivitySplashBinding
import com.algirm.arling.ui.main.MainActivity
import com.algirm.arling.ui.login.LoginActivity
import com.algirm.arling.util.Constants.REQUEST_CAMERA_PERMISSIONS_CODE
import com.algirm.arling.util.Constants.REQUEST_LOCATION_PERMISSIONS_CODE
import com.algirm.arling.util.PermissionUtil.hasCameraPermission
import com.algirm.arling.util.PermissionUtil.hasLocationPermissions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.AppSettingsDialogHolderActivity
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var checkLoginJob: Job? = null
    private var errorHandler: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        runnable = Runnable { checkPermission() }
        handler = Handler(Looper.getMainLooper())
        handler.postDelayed(runnable, 1000L)
    }

    private fun checkLogin() {
        checkLoginJob = lifecycleScope.launchWhenCreated {
            try {
                withTimeout(5000L) {
//                    firebaseAuth.signOut() // todo testing
                    val isLogin = firebaseAuth.currentUser != null
                    if (isLogin) sendToMainActivity() else sendToLoginActivity()
                }
            } catch (e: Exception) {
                Timber.e(e)
                Toast.makeText(
                    this@SplashActivity,
                    e.message ?: "Unexpected Error",
                    Toast.LENGTH_LONG
                ).show()
                delay(5000)
                sendToLoginActivity()
            }
        }
    }

    private fun sendToLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun sendToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handler.removeCallbacks(runnable)
        checkLoginJob?.cancel()
        errorHandler?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            checkPermission()
        }
    }

    private fun checkPermission() {
        if (hasLocationPermissions(this)) {
            if (hasCameraPermission(this)) {
                handler.removeCallbacks(runnable)
                runnable = Runnable { checkLogin() }
                handler.post(runnable)
            } else {
                requestCameraPermission()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        EasyPermissions.requestPermissions(
            this,
            "Aplikasi ini membutuhkan izin anda untuk akses lokasi pada perangkat.",
            REQUEST_LOCATION_PERMISSIONS_CODE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestCameraPermission() {
        EasyPermissions.requestPermissions(
            this,
            "Aplikasi ini membutuhkan izin anda untuk akses kamera pada perangkat.",
            REQUEST_CAMERA_PERMISSIONS_CODE,
            Manifest.permission.CAMERA
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        checkPermission()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            checkPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onRationaleDenied(requestCode: Int) {
        finish()
    }

    override fun onRationaleAccepted(requestCode: Int) {
        checkPermission()
    }

}