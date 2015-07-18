package com.aryon.homework2;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by Aryon on 2015/7/18.
 */
public class TextureListner implements TextureView.SurfaceTextureListener {
    static  String          TAG = "TextureListner";

    //Camera mCamera;
    private  MainActivity   mainActivity;

    public TextureListner(MainActivity activity, Camera camera){
        mainActivity = activity;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "TextureAvailable");

        if(mainActivity.mCamera!=null){
            mainActivity.mCamera.release();
            mainActivity.mCamera = null;
            Log.d(TAG, "CameraReleased");
        }
        try{
            mainActivity.mCamera = Camera.open();
            mainActivity.mCamera.setDisplayOrientation(90);
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG,"CameraOpenFailed");
        }
        Log.d(TAG,"SettingCameraParameters");

        try {
            Camera.Parameters parameters = mainActivity.mCamera.getParameters();
            //Camera.Size size = parameters.getPreviewSize();
/*
            List<Camera.Size> localSize = parameters.getSupportedPreviewSizes();
            for(Camera.Size sz : localSize){
                Log.d(TAG,"    SupportedPreviewSize:"+sz.width+"*"+sz.height);
                if (sz.width/sz.height == 4/3 || sz.width/sz.height == 3/4) {
                    size.height = sz.height;
                    size.width = sz.width;
                    //break;
                }
            }
            Log.d(TAG,"CameraParametersSetting,Size:"+width+"*"+height+"\n        PreViewSize:"+size.width+"*"+size.height);*/

            parameters.setPreviewSize(1600,1200);
            parameters.setPictureSize(1280,960);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

            mainActivity.mCamera.setParameters(parameters);
            mainActivity.mCamera.setPreviewTexture(surface);
            mainActivity.mCamera.startPreview();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG,"TextureViewSizeChanged");
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if(mainActivity.mCamera!=null) mainActivity.mCamera.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
