package com.algirm.arling.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.*
import android.location.Geocoder
import android.location.Location
import android.opengl.Matrix
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.algirm.arling.R
import com.algirm.arling.ar.ARCamera
import com.algirm.arling.ar.AROverlayView
import com.algirm.arling.data.model.Petugas
import com.algirm.arling.databinding.ActivityMainBinding
import com.algirm.arling.ui.login.LoginActivity
import com.algirm.arling.ui.login.SectorActivity
import com.algirm.arling.ui.splash.SplashActivity
import com.algirm.arling.util.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private lateinit var arOverlayView: AROverlayView
    private lateinit var arCamera: ARCamera

    private lateinit var surfaceView: SurfaceView
    private lateinit var cameraContainerLayout: FrameLayout
    private lateinit var mapFragment: SupportMapFragment

    private var camera: Camera? = null
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()

    private var mListPetugas: MutableList<Petugas> = mutableListOf()
    private var selected: Petugas? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // keluar
        binding.tambahFab.setOnClickListener {
            startActivity(Intent(this, SectorActivity::class.java))
            finish()
        }
        binding.pingFab.setOnClickListener {
            viewModel.updatePingStatus()
        }
        lifecycleScope.launchWhenCreated {
            viewModel.pingState.collect {
                if (it) {
                    binding.pingFab.setImageResource(R.drawable.ic_outline_report_24)
                    binding.pingFab.backgroundTintList = ColorStateList.valueOf(Color.RED)
                } else {
                    binding.pingFab.setImageResource(R.drawable.ic_outline_report_off_24)
                    binding.pingFab.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
                }
            }
        }

        mapFragment =
            supportFragmentManager.findFragmentById(R.id.maps_fragment) as SupportMapFragment
        mapFragment.onCreate(savedInstanceState)
        cameraContainerLayout = binding.cameraContainerLayout
        surfaceView = binding.surfaceView
        arCamera = ARCamera(this, surfaceView)
        arOverlayView = AROverlayView(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService()!!
        getLocation()
        setUpMaps()

        lifecycleScope.launchWhenCreated {
            viewModel.listArPoint.collect { listArPoint ->
                arOverlayView.setData(listArPoint)
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.currentLoc.collect { lastLocation ->
                viewModel.getAllOnce()
                arOverlayView.updateCurrentLocation(lastLocation)
                updateMarker(mListPetugas)
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.listPetugas.collect { listPetugas ->
                Timber.d(listPetugas.toString())
                mListPetugas.clear()
                mListPetugas.addAll(listPetugas)
                updateMarker(mListPetugas)
            }
        }

        arOverlayView.setOnTouchListener {
            viewModel.touchCanvas(it)
        }
        lifecycleScope.launchWhenCreated {
            viewModel.selected.collect {
                Timber.d("Selected is ${it.toString()}")
                selected = it
                arOverlayView.setSelected(selected)
                updateMarker(mListPetugas)
            }
        }
    }

    private fun updateMarker(listPetugas: List<Petugas>) {
        if (currentLocation == null) {
            Timber.w("Location has not been determined yet")
            return
        }

        map?.let {
            it.clear()
            markers.clear()
        }

        val boundsBuilder = LatLngBounds.Builder().include(currentLocation!!.latLng)

        for (petugas in listPetugas) {
            val geocoder = Geocoder(this, Locale.getDefault())
            val address: String = try {
                geocoder.getFromLocation(petugas.lat, petugas.lon, 1)[0].getAddressLine(0)
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
            map?.let {
                val marker = it.addMarker(
                    MarkerOptions()
                        .position(petugas.getLocation().latLng)
                        .title(petugas.name)
                        .snippet(address)
                )
                marker?.tag = petugas.uid
                selected?.let { selectedPetugas ->
                    if (petugas.uid == selectedPetugas.uid) {
                        marker?.showInfoWindow()
                        Timber.d("marker ${marker.title} info window ${marker?.isInfoWindowShown}")
                    }
                }
                markers.add(marker!!)
                boundsBuilder.include(petugas.getLocation().latLng)
            }
        }
        val bounds = boundsBuilder.build()
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        selected?.let {
            map?.animateCamera(CameraUpdateFactory.newLatLng(it.getLocation().latLng))
        }
    }

    private fun setUpMaps() {
        mapFragment.getMapAsync { gMap ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // request permission
                return@getMapAsync
            }
            gMap.isMyLocationEnabled = true
            gMap.isBuildingsEnabled = false
            gMap.isIndoorEnabled = false
            getCurrentLocation {
                if (it == null) {
                    return@getCurrentLocation
                }
                currentLocation = it
                Timber.d("current location = $currentLocation")
                val pos = CameraPosition.fromLatLngZoom(currentLocation!!.latLng, 15f)
                gMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
            }
            gMap.setOnMarkerClickListener { marker ->
                Timber.d("touch marker ${marker.title} ${marker.isInfoWindowShown}")
                viewModel.selectFromMarker(marker.tag as String)
                return@setOnMarkerClickListener true
            }
            gMap.setOnMapClickListener {
                Timber.d("Map Clicked")
                viewModel.clearSelected()
            }
            gMap.setOnMyLocationButtonClickListener {
                viewModel.clearSelected()
                return@setOnMyLocationButtonClickListener true
            }
            map = gMap
        }
    }

    private fun getCurrentLocation(onSuccess: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // request permission
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            currentLocation = loc
            onSuccess(loc)
        }.addOnFailureListener {
            Timber.e("Could not get location")
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            currentLocation = result.lastLocation
            viewModel.updateMyLocation(currentLocation!!)
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission Required!", Toast.LENGTH_SHORT).show()
            // requestPermissions() todo
        } else {
            Timber.d("Getting Location..")
            val request = LocationRequest.create().apply {
                interval = Constants.LOCATION_UPDATE_INTERVAL
                fastestInterval = Constants.LOCATION_UPDATE_INTERVAL
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun registerSensors() {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun initAROverlayView() {
        if (arOverlayView.parent != null) {
            (arOverlayView.parent as ViewGroup).removeView(arOverlayView)
        }
        cameraContainerLayout.addView(arOverlayView)
        val viewTreeObserver = arOverlayView.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    arOverlayView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Timber.d("view/screen size = ${arOverlayView.width} ${arOverlayView.height}")
                    viewModel.setScreenSize(arOverlayView.width, arOverlayView.height)
                }
            })
        }
    }

    private fun initARCameraView() {
        reloadSurfaceView()
        if (arCamera.parent != null) {
            (arCamera.parent as ViewGroup).removeView(arCamera)
        }
        cameraContainerLayout.addView(arCamera)
        arCamera.keepScreenOn = true
        initCamera()
    }

    private fun reloadSurfaceView() {
        if (surfaceView.parent != null) {
            (surfaceView.parent as ViewGroup).removeView(surfaceView)
        }
        cameraContainerLayout.addView(surfaceView)
    }

    private fun initCamera() {
        val numCams = Camera.getNumberOfCameras()
        if (numCams > 0) {
            try {
                camera = Camera.open()
                camera?.startPreview()
                arCamera.setCamera(camera)
            } catch (ex: RuntimeException) {
                Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun releaseCamera() {
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        arCamera.setCamera(null)
        camera?.release()
        camera = null
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrixFromVector = FloatArray(16)
            val rotationMatrix = FloatArray(16)
            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, sensorEvent.values)
            when (this.windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                    rotationMatrixFromVector,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X, rotationMatrix
                )
                Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                    rotationMatrixFromVector,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X, rotationMatrix
                )
                Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                    rotationMatrixFromVector,
                    SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                    rotationMatrix
                )
                else -> SensorManager.remapCoordinateSystem(
                    rotationMatrixFromVector,
                    SensorManager.AXIS_X, SensorManager.AXIS_Y,
                    rotationMatrix
                )
            }
            val projectionMatrix = arCamera.projectionMatrix
            val rotatedProjectionMatrix = FloatArray(16)
            Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrix, 0)
            viewModel.updateRotatedProjectionMatrix(rotatedProjectionMatrix)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Timber.w("Orientation compass unreliable")
        }
    }

    override fun onResume() {
        super.onResume()
        initARCameraView()
        initAROverlayView()
        registerSensors()
        mapFragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
        sensorManager.unregisterListener(this)
        mapFragment.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapFragment.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        mapFragment.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapFragment.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapFragment.onDestroy()
    }
}

val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)