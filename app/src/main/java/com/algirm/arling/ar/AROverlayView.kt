package com.algirm.arling.ar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.view.View
import java.util.ArrayList
import android.text.TextPaint
import android.view.MotionEvent
import com.algirm.arling.R
import com.algirm.arling.data.model.ARPoint
import com.algirm.arling.data.model.Petugas

import com.algirm.arling.util.Constants.CIRCLE_MAX_RADIUS
import com.algirm.arling.util.Constants.CIRCLE_MIN_RADIUS
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.math.roundToInt

@AndroidEntryPoint
class AROverlayView(context: Context) : View(context) {

    private var currentLocation: Location? = null
    private var listARPoint: List<ARPoint> = ArrayList()
    private var selected: Petugas? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shapePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    init {
        fillPaint.style = Paint.Style.FILL
        fillPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        strokePaint.style = Paint.Style.STROKE
        strokePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        shapePaint.style = Paint.Style.FILL
        shapePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private var onItemTouchListener: ((MotionEvent) -> Unit)? = null

    fun setOnTouchListener(listener: (MotionEvent) -> Unit) {
        onItemTouchListener = listener
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            Timber.d("ACTION_UP")
            onItemTouchListener?.let { it(event) }
//            return true
        }
        return true
    }

    fun setData(data: List<ARPoint>) {
        listARPoint = data
        this.invalidate()
    }

    fun setSelected(data: Petugas?) {
        selected = data
        this.invalidate()
    }

    fun updateCurrentLocation(currentLocation: Location?) {
        this.currentLocation = currentLocation
        this.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentLocation == null) {
            return
        }

        val mutableListARPoint: MutableList<ARPoint> = listARPoint.toMutableList()
        selected?.let {
            for (i in listARPoint.indices)
            if (it.uid == listARPoint[i].petugas.uid) {
                mutableListARPoint.remove(listARPoint[i])
                mutableListARPoint.add(listARPoint[i])
            }
        }
        listARPoint = mutableListARPoint.toList()

        for (i in listARPoint.indices) {

            if (listARPoint[i].isVisible) {
                fillPaint.color = Color.WHITE
                fillPaint.textSize = 60f

                strokePaint.color = Color.BLACK
                strokePaint.textSize = 60f
                strokePaint.strokeWidth = 10f

                shapePaint.color = Color.WHITE
                shapePaint.setShadowLayer(30f, 5f, 5f, Color.BLACK)

                val distance = listARPoint[i].distance
                val circleRadius = if (distance > 15) {
                    val v = (distance.toFloat() / 100f) - (15f / 100f)
                    if (v < 1f) {
                        val radiusCorrected = CIRCLE_MAX_RADIUS * v
                        if ((CIRCLE_MAX_RADIUS - radiusCorrected) < CIRCLE_MIN_RADIUS) {
                            CIRCLE_MIN_RADIUS
                        } else {
                            CIRCLE_MAX_RADIUS - radiusCorrected
                        }
                    } else {
                        CIRCLE_MIN_RADIUS
                    }
                } else {
                    CIRCLE_MAX_RADIUS
                }
                val textSize = circleRadius * 2
                fillPaint.textSize = textSize
                strokePaint.textSize = textSize
                val text = listARPoint[i].petugas.name + "(" + distance + " m)"
                val xText = listARPoint[i].x - circleRadius * listARPoint[i].petugas.name!!.length / 2
                val yText = listARPoint[i].y - (circleRadius.toInt()) * 2

                selected?.let {
                    if (it.uid == listARPoint[i].petugas.uid) {
                        shapePaint.color = Color.GREEN
                    }
                }
                if (listARPoint[i].petugas.ping) {
                    shapePaint.color = Color.RED
                }

                canvas.drawCircle(listARPoint[i].x, listARPoint[i].y, circleRadius, shapePaint)
                canvas.drawRect(getTextBackgroundSize(xText, yText, text, fillPaint), shapePaint)
                canvas.drawText(text, xText, yText, strokePaint)
                canvas.drawText(text, xText, yText, fillPaint)
            } else {
                if (listARPoint[i].petugas.ping) {
                    shapePaint.color = Color.WHITE
                    shapePaint.setShadowLayer(30f, 5f, 5f, Color.BLACK)
//                    canvas.drawCircle(listARPoint[i].x, listARPoint[i].y, 50f, shapePaint)
                    val icon = resources.getDrawable(R.drawable.ic_baseline_warning_50)
                    val x = if (listARPoint[i].x.toInt() <= 0) {
                        listARPoint[i].x.toInt()
                    } else {
                        listARPoint[i].x.toInt() - icon.intrinsicHeight
                    }
                    icon.setBounds(
                        x,
                        listARPoint[i].y.toInt() - icon.intrinsicWidth,
                        x+icon.intrinsicHeight,
                        listARPoint[i].y.toInt()
                    )
                    icon.draw(canvas)
                }
            }

        }
    }

    private fun getTextBackgroundSize(x: Float, y: Float, text: String, paint: Paint): Rect {
        val fontMetrics = paint.fontMetrics
        val halfTextLength = paint.measureText(text)
        return Rect(
            (x).toInt(),
            (y + fontMetrics.top).toInt(),
            (x + halfTextLength).toInt(),
            (y + fontMetrics.bottom).toInt()
        )
    }

    /* override fun onDraw(canvas: Canvas) {
         super.onDraw(canvas)
         if (currentLocation == null) {
             return
         }

         val radius = 30
         paint.style = Paint.Style.FILL
         paint.color = Color.WHITE
         paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
         paint.textSize = 60f

         for (i in petugasList.indices) {
             currentLocation!!.altitude = 0.0 // flatten altitude all 0
             val currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation)
             val pointInECEF = LocationHelper.WSG84toECEF(petugasList[i].getLocation())
             val pointInENU =
                 LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF)

             Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0)

             // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
             // if z > 0, the point will display on the opposite
             if (cameraCoordinateVector[2] < 0) {
                 // before with canvas.width and canvas.height
                 val x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * width
                 val y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * height
                 val distance =
                     currentLocation!!.distanceTo(petugasList[i].getLocation()).roundToInt()
                 canvas.drawCircle(x, y, radius.toFloat(), paint)
                 canvas.drawText(
                     petugasList[i].name + "(" + distance + " m)",
                     x - 30 * petugasList[i].name!!.length / 2,
                     y - 80,
                     paint
                 )
             }
         }
     }*/

}