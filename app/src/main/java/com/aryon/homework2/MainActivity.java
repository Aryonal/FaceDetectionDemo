package com.aryon.homework2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    Button bn;
    TextureView txv;
    Camera mCamera = null;
    AlertDialog.Builder dialog;
    AlertDialog ad = null;

    public int FaceNumber = 0;
    public Bitmap READYTOSHOW = null;
    final int PROCESSOVER = 1;
    final int NOFACEDETECTED = 2;
    DrawFace drawFace;

    Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            //
            switch(msg.what){
                case PROCESSOVER: {
                    //close dialog ad
                    Log.d("MainActivity","got msg 1");
                    ad.hide();

                    AlertDialog.Builder imageDialog = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.DialogTheme));
                    imageDialog.setView(R.layout.dialog_image)
                               .setPositiveButton("Back To Preview", new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {
                                       dialog.dismiss();
                                   }
                               })
                               .setTitle(FaceNumber<2?FaceNumber+" Face Found in BMP":FaceNumber+" Faces found in BMP");
                    AlertDialog imagedialog = imageDialog.create();
                    imagedialog.show();
                    ImageView iv = (ImageView)imagedialog.findViewById(R.id.dialog_image);
                    Log.d("MainActivity",iv==null?"iv is null":"iv is not null");

                    iv.setImageBitmap(READYTOSHOW);

                    imagedialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            READYTOSHOW.recycle();
                            mCamera.startPreview();
                            bn.setText(R.string.bottom_button);
                        }
                    });

                    break;
                }
                case NOFACEDETECTED: {
                    Log.d("MainActivity","got face:"+0);
                    Toast.makeText(MainActivity.this,"Oops! No face caught",Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bn = (Button)findViewById(R.id.button);
        txv = (TextureView)findViewById(R.id.textureview);

        dialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.DialogTheme));

        txv.setSurfaceTextureListener(this);
        bn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bn.getText().equals("Take Photo")) {
                    ShootAndDraw();
                    bn.setText(R.string.mask_button);
                }
                else if(bn.getText().equals("add mask")){
                    bn.setText(R.string.top_button);
                    ad = dialog.setView(R.layout.dialog_view).create();
                    ad.show();
                //start processing
                    drawFace.start();
                }else if(bn.getText().equals("Go To PreView")){
                    bn.setText(R.string.bottom_button);
                    if(mCamera != null)
                        mCamera.startPreview();
                }else
                    Toast.makeText(MainActivity.this,"@_@ SAD, ERROR"+bn.getText(),Toast.LENGTH_LONG).show();
            }
        });
    }

    public void SetBMP(Bitmap bmp){
        this.READYTOSHOW = bmp;
    }

    public void ShootAndDraw() {
        //
        Toast.makeText(MainActivity.this,"Surprise!!",Toast.LENGTH_SHORT).show();
        if(mCamera!=null){
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if(success)
                        camera.takePicture(null, null, null,
                            new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data, Camera camera) {
                                    drawFace = new DrawFace(MainActivity.this, data, 10);
                                }
                            });
                    else 
                        Toast.makeText(MainActivity.this,"Focus Error",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("MainActivity","TextureAvailable");

        if(mCamera!=null){
            mCamera.release();
            mCamera = null;
            Log.d("MainActivity","CameraReleased");
        }
        try{
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
        }catch (Exception e){
            e.printStackTrace();
            Log.d("MainActivity","CameraOpenFailed");
        }
        Log.d("MainActivity","SettingCameraParameters");

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
/*
            List<Camera.Size> localSize = parameters.getSupportedPreviewSizes();
            for(Camera.Size sz : localSize){
                Log.d("MainActivity","    SupportedPreviewSize:"+sz.width+"*"+sz.height);
                if (sz.width/sz.height == 4/3 || sz.width/sz.height == 3/4) {
                    size.height = sz.height;
                    size.width = sz.width;
                    //break;
                }
            }*/
            Log.d("MainActivity","CameraParametersSetting,Size:"+width+"*"+height+"\n        PreViewSize:"+size.width+"*"+size.height);

            parameters.setPreviewSize(1600,1200);
            parameters.setPictureSize(1280,960);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

            mCamera.setParameters(parameters);
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                //
                Toast.makeText(MainActivity.this,"Shoot now!",Toast.LENGTH_SHORT).show();
                Log.d("MainActivity","Face detected!");
            }
        });

    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("MainActivity","TextureViewSizeChanged");
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


}
