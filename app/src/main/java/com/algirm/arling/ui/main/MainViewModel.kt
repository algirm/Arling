package com.algirm.arling.ui.main

import android.location.Location
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algirm.arling.data.model.ARPoint
import com.algirm.arling.data.model.Petugas
import com.algirm.arling.domain.repository.PetugasRepo
import com.algirm.arling.util.Constants
import com.algirm.arling.util.LocationHelper
import com.algirm.rondar.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val petugasRepo: PetugasRepo
) : ViewModel() {

    private var screenHeight = 0
    private var screenWidth = 0

    private val _currentLoc = Channel<Location>()
    val currentLoc = _currentLoc.receiveAsFlow()

    private val _rotatedProjectionMatrix = MutableStateFlow(FloatArray(16))
    val rotatedProjectionMatrix: StateFlow<FloatArray> = _rotatedProjectionMatrix

    private val _curLoc = MutableStateFlow(Location(""))
    val curLoc: StateFlow<Location> = _curLoc

    private val _listPetugas: MutableStateFlow<List<Petugas>> = MutableStateFlow(emptyList())
    val listPetugas: StateFlow<List<Petugas>> = _listPetugas

    private val _listArPoint: MutableStateFlow<List<ARPoint>> = MutableStateFlow(emptyList())
    val listArPoint: StateFlow<List<ARPoint>> = _listArPoint

    private val _selected: MutableSharedFlow<Petugas?> = MutableSharedFlow()
    val selected: SharedFlow<Petugas?> = _selected

    private val _pingState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val pingState = _pingState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.main) {
            Timber.d("init data..")
            try {
                withContext(dispatcher.io) {
                    petugasRepo.getUserData().collect { userData ->
                        _pingState.value = userData.ping
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getPingStatus() = viewModelScope.launch(dispatcher.main) {
        Timber.d("get ping status..")
        try {
            withContext(dispatcher.io) {
                petugasRepo.getUserData().collect { userData ->
                    _pingState.value = userData.ping
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun updatePingStatus() = viewModelScope.launch(dispatcher.main) {
        Timber.d("Updating Ping Status")
        try {
            withContext(dispatcher.io) {
                petugasRepo.setPing(!pingState.value)
            }
            getPingStatus()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun getAllOnce() = viewModelScope.launch(dispatcher.main) {
        Timber.d("getAllOnce")
        try {
            withContext(dispatcher.io) {
                petugasRepo.getAllOnce().collect { resultListPetugas ->
                    _listPetugas.value = resultListPetugas
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun updateMyLocation(location: Location) = viewModelScope.launch(dispatcher.main) {
        try {
            withContext(dispatcher.io) {
                petugasRepo.updateUserData(location)
                _currentLoc.send(location)
                _curLoc.value = location
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun updateRotatedProjectionMatrix(matrix: FloatArray) = viewModelScope.launch(dispatcher.main) {
        _rotatedProjectionMatrix.value = matrix
        calculateScreenCoordinate()
    }

    fun touchCanvas(motionEvent: MotionEvent) = viewModelScope.launch(dispatcher.main) {
        val touchX = motionEvent.x.roundToInt()
        val touchY = motionEvent.y.roundToInt()
        val arPoints = listArPoint.value
        var pointX: Int
        var pointY: Int
        var selectedPoint: ARPoint? = null

        for (i in arPoints.indices) {
//            pointX = arPoints[i].x.roundToInt()
//            pointY = arPoints[i].y.roundToInt()
            pointX = arPoints[i].x.toInt()
            pointY = arPoints[i].y.toInt()

            if (Constants.TOUCH_RADIUS >= calculateDistanceBetweenPoints(
                    pointX.toDouble(),
                    pointY.toDouble(),
                    touchX.toDouble(),
                    touchY.toDouble()
                )
            ) {
                selectedPoint = arPoints[i]
                break
            }
        }
        _selected.emit(selectedPoint?.petugas)
    }

    private fun calculateScreenCoordinate() = viewModelScope.launch(dispatcher.main) {
        val arPoints = arrayListOf<ARPoint>()
        for (i in listPetugas.value.indices) {
            val cameraCoordinateVector = FloatArray(4)
            val currentLocation = curLoc.value
            currentLocation.altitude = 0.0
            val pointInENU =
                LocationHelper.WGS84toENU(currentLocation, listPetugas.value[i].getLocation())
            Matrix.multiplyMV(
                cameraCoordinateVector,
                0,
                rotatedProjectionMatrix.value,
                0,
                pointInENU,
                0
            )

            var isVisible: Boolean
            var x: Float
            var y: Float
            val distance = currentLocation.distanceTo(listPetugas.value[i].getLocation()).roundToInt()

            if (cameraCoordinateVector[2] > 0) {
                isVisible = true
                x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * screenWidth
                y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * screenHeight
                if (!(x.unaryPlus() <= screenWidth && x.unaryPlus() >= 0)) {
                    isVisible = false
                    x = if (cameraCoordinateVector[0] / cameraCoordinateVector[2] > 0) {
                        screenWidth.toFloat()
                    } else {
                        0f
                    }
                }
            } else {
                x = if (cameraCoordinateVector[0] / cameraCoordinateVector[2] > 0) {
                    0f
                } else {
                    screenWidth.toFloat()
                }
                y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * screenHeight
                isVisible = false
            }

            arPoints.add(ARPoint(listPetugas.value[i], x, y, distance, isVisible))

//            if (cameraCoordinateVector[2] > 0) {
//                val x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * screenWidth
//                val y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * screenHeight
//                val distance = currentLocation.distanceTo(listPetugas.value[i].getLocation()).roundToInt()
//                val visible = x.unaryPlus() <= screenWidth && x.unaryPlus() >= 0
//                arPoints.add(ARPoint(listPetugas.value[i], x, y, distance, visible))
////                Timber.i("$screenWidth x $screenHeight, $x x $y")
//            }
        }
        _listArPoint.value = arPoints
    }

    private fun calculateDistanceBetweenPoints(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double
    ): Double {
        return sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1))
    }

    fun selectFromMarker(uid: String) = viewModelScope.launch(dispatcher.main) {
        for (petugas in listPetugas.value) {
            if (petugas.uid == uid) {
                _selected.emit(petugas)
                break
            }
        }
    }

    fun clearSelected() = viewModelScope.launch(dispatcher.main) {
        _selected.emit(null)
    }

    fun setScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

}