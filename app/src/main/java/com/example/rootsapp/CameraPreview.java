package com.example.rootsapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraPreview extends TextureView implements TextureView.SurfaceTextureListener {

    private Camera camera;
    private Camera.PreviewCallback previewCallback;

    public interface CameraFrameListener {
        void onFrame(Bitmap bitmap);
    }

    private CameraFrameListener listener;

    public CameraPreview(Context context, CameraFrameListener listener) {
        super(context);
        this.listener = listener;
        setSurfaceTextureListener(this);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            camera.setPreviewTexture(surface);
            camera.setDisplayOrientation(90);

            camera.setPreviewCallback((data, cam) -> {
                Camera.Size previewSize = cam.getParameters().getPreviewSize();
                // convert byte[] NV21 to bitmap
                Bitmap bmp = CameraUtils.getBitmapFromNV21(data, previewSize.width, previewSize.height);
                if (bmp != null && listener != null) listener.onFrame(bmp);
            });

            camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    public void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                Log.d("CameraPreview", "Camera released safely");
            } catch (Exception e) {
                Log.e("CameraPreview", "Error releasing camera", e);
            }
        }
    }

}

