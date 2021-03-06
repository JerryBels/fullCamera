package br.com.ferreiraz.fullcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.*;

import java.io.IOException;
import java.util.List;

// http://developer.android.com/guide/topics/media/camera.html#preview-layout
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String TAG = "FULL_CAM_PREVIEW";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error stoping camera preview: " + e.getMessage());
        }

        Camera.Parameters parameters = mCamera.getParameters();

/*        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            parameters.setPreviewSize(h, w);
            mCamera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            parameters.setPreviewSize(w, h);
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            parameters.setPreviewSize(h, w);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            parameters.setPreviewSize(w, h);
            mCamera.setDisplayOrientation(180);
        }

        mCamera.setParameters(parameters);*/

        boolean isPortrait = false;
        final int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                mCamera.setDisplayOrientation(0);
                parameters.setRotation(0);
                break;
            case Surface.ROTATION_270:
                mCamera.setDisplayOrientation(180);
                parameters.setRotation(180);
                break;
            default:
                isPortrait = true;
                mCamera.setDisplayOrientation(90);
                parameters.setRotation(90);
                int t = w;
                w = h;
                h = t;
                break;
        }

        if (parameters.getSupportedPictureSizes() != null) {
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            Camera.Size largest = sizes.get(0);
            for (Camera.Size size : sizes) {
                if(size.width > w && size.height > h) {
                    if (size.width >= largest.width && size.height >= largest.height) {
                        largest = size;
                    }
                }
            }
            float relationHeight = (float) largest.height / h;
            float relationWidth  = (float) largest.width  / w;
            float relation       = (relationHeight > relationWidth) ? relationHeight : relationWidth;
            int previewHeight = (int) Math.floor(largest.height / relation);
            int previewWidth  = (int) Math.floor(largest.width  / relation);

            if(isPortrait)
                parameters.setPreviewSize(previewHeight, previewWidth);
            else
                parameters.setPreviewSize(previewWidth, previewHeight);
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}