package com.algirm.arling.ar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.view.View;

import com.algirm.arling.data.model.Petugas;
import com.algirm.arling.util.LocationHelperOld;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ntdat on 1/13/17.
 */

public class AROverlayViewOld extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<Petugas> petugasList = new ArrayList<>();


    public AROverlayViewOld(Context context) {
        super(context);

        this.context = context;

    }

    public void setData(List<Petugas> data) {
        this.petugasList = data;
        this.invalidate();
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }

        final int radius = 30;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);

        for (int i = 0; i < petugasList.size(); i++) {
            currentLocation.setAltitude(0); // test
            float[] currentLocationInECEF = LocationHelperOld.WSG84toECEF(currentLocation);
            float[] pointInECEF = LocationHelperOld.WSG84toECEF(petugasList.get(i).getLocation());
            float[] pointInENU = LocationHelperOld.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);

            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0) {
                float x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * canvas.getHeight();

                int distance = Math.round(currentLocation.distanceTo(petugasList.get(i).getLocation()));

                canvas.drawCircle(x, y, radius, paint);
                canvas.drawText(petugasList.get(i).getName() +"(" + distance + " m)", x - (30 * petugasList.get(i).getName().length() / 2), y - 80, paint);
            }
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        Toast.makeText(context, "touched", Toast.LENGTH_SHORT).show();
//        return true;
//    }
}
