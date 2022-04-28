package com.algirm.arling.ar

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.opengl.Matrix
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import timber.log.Timber
import java.io.IOException
import kotlin.math.abs

/**
 * Created by ntdat on 1/13/17.
 */
@Suppress("DEPRECATION")
@TargetApi(Build.VERSION_CODES.KITKAT)
class ARCamera(context: Context, surfaceView: SurfaceView) :
    ViewGroup(context), SurfaceHolder.Callback {

    private var surfaceHolder: SurfaceHolder = surfaceView.holder
    private var previewSize: Camera.Size? = null
    private var supportedPreviewSizes: List<Camera.Size>? = null
    private var camera: Camera? = null
    private var parameters: Camera.Parameters? = null
    private var activity: Activity = context as Activity
    var projectionMatrix = FloatArray(16)
    private var cameraWidth = 0
    private var cameraHeight = 0

    init {
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    fun setCamera(camera: Camera?) {
        this.camera = camera
        if (this.camera != null) {
            supportedPreviewSizes = this.camera!!.parameters.supportedPreviewSizes
            requestLayout()
            val params = this.camera!!.parameters
            val focusModes = params.supportedFocusModes
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                this.camera!!.parameters = params
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed && childCount > 0) {
            val child = getChildAt(0)
            val width = right - left
            val height = bottom - top
            var previewWidth = width
            var previewHeight = height
            if (previewSize != null) {
                previewWidth = previewSize!!.width
                previewHeight = previewSize!!.height
            }
            if (width * previewHeight > height * previewWidth) {
                val scaledChildWidth = previewWidth * height / previewHeight
                child.layout(
                    (width - scaledChildWidth) / 2, 0,
                    (width + scaledChildWidth) / 2, height
                )
            } else {
                val scaledChildHeight = previewHeight * width / previewWidth
                child.layout(
                    0, (height - scaledChildHeight) / 2,
                    width, (height + scaledChildHeight) / 2
                )
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            if (camera != null) {
                parameters = camera!!.parameters
                val orientation = getCameraOrientation()
                camera!!.setDisplayOrientation(orientation)
                camera!!.parameters.setRotation(orientation)
                camera!!.setPreviewDisplay(holder)
            }
        } catch (exception: IOException) {
            Timber.e(exception)
        }
    }

    private fun getCameraOrientation(): Int {
        val info = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var orientation: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (info.orientation + degrees) % 360
            orientation = (360 - orientation) % 360
        } else {
            orientation = (info.orientation - degrees + 360) % 360
        }
        return orientation
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (camera != null) {
            camera!!.setPreviewCallback(null)
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
    }

    private fun getOptimalPreviewSize(
        sizes: List<Camera.Size>?,
        width: Int,
        height: Int
    ): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = width.toDouble() / height
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue
            }
            if (abs(size.height - height) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - height).toDouble()
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (abs(size.height - height) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - height).toDouble()
                }
            }
        }
        if (optimalSize == null) {
            optimalSize = sizes[0]
        }
        return optimalSize
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (camera != null) {
            cameraWidth = width
            cameraHeight = height
            val params = camera!!.parameters
            val previewSizes2 = parameters!!.supportedPreviewSizes
            for (i in previewSizes2.indices) {
                Timber.d("surfaceChanged: for i=$i/$previewSizes2")
                Timber.d("surfaceChanged: previewSize=${previewSizes2[i].width} x ${previewSizes2[i].height}")
                val mPreviewSize = previewSizes2[i]
                try {
                    params.setPreviewSize(mPreviewSize.width, mPreviewSize.height)
                    requestLayout()
                    camera!!.parameters = params
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            camera!!.startPreview()
            generateProjectionMatrix()
        }
    }

    private fun generateProjectionMatrix() {
        val ratio: Float = if (cameraWidth < cameraHeight) {
            cameraWidth.toFloat() / cameraHeight
        } else {
            cameraHeight.toFloat() / cameraWidth
        }
        val OFFSET = 0
        val LEFT = -ratio
        val RIGHT = ratio
        val BOTTOM = -1f
        val TOP = 1f
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, Z_NEAR, Z_FAR)
    }

    companion object {
        private const val Z_NEAR = 0.5f
        private const val Z_FAR = 10000f
    }

}