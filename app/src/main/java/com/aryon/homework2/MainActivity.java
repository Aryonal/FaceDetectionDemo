package com.aryon.homework2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    static  String              TAG = "MainActivity";

    public  Button              bn;
    public  TextureView         txv;
    public  ImageView           fcv;
    public  Camera              mCamera = null;
    public  AlertDialog.Builder dialog;
    public  AlertDialog         ad = null;

    public  Bitmap              blankBMP;
    public  float               x;
    public  float               y;

    private int                 SHOOTOVER = 0;

    public int                  FaceNumber = 0;
    public Bitmap               READYTOSHOW = null;
    final int                   PROCESSOVER = 1;
    final int                   NOFACEDETECTED = 2;
    public  DrawFace            drawFace;

    Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            //
            switch(msg.what){
                case PROCESSOVER: {
                    //close dialog ad
                    Log.d(TAG,"got msg 1");
                    ad.dismiss();

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
                    imagedialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
                    imagedialog.show();
                    ImageView iv = (ImageView)imagedialog.findViewById(R.id.dialog_image);
                    Log.d(TAG,iv==null?"iv is null":"iv is not null");

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
                    Log.d(TAG,"got face:"+0);
                    Toast.makeText(MainActivity.this,"Oops! No face caught",Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.front_page);
        //delay 1s
        final TextView front = (TextView)findViewById(R.id.front_text);
        final AnimationSet animset = new AnimationSet(true);
        final Animation translateAnimation = new TranslateAnimation(0,0,0,1191);
        final Animation scaleAnimation = new ScaleAnimation(1,0.606f,1,0.606f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        translateAnimation.setDuration(1000);
        scaleAnimation.setDuration(1000);
        animset.addAnimation(translateAnimation);
        animset.addAnimation(scaleAnimation);

//
        animset.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
//OnCreate
            createView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                front.startAnimation(animset);
            }
        }, 1000/* 1sec delay */);
    }

    public void createView(){
        setContentView(R.layout.activity_main);

        bn = (Button)findViewById(R.id.button);
        txv = (TextureView)findViewById(R.id.textureview);
        fcv = (ImageView)findViewById(R.id.focusview);

        dialog = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.DialogTheme));

        txv.setSurfaceTextureListener(new TextureListner(this,mCamera));
        fcv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FocusOnArea(event);
                return false;
            }
        });
        bn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bn.getText().equals("Take Photo")) {
                    ShootAndDraw();
                    bn.setText(R.string.mask_button);
                }
                else if(bn.getText().equals("add mask")){
                    if(drawFace!=null && SHOOTOVER == 1) {
                        SHOOTOVER = 0;
                        bn.setText(R.string.top_button);
                        ad = dialog.setView(R.layout.dialog_view).create();
                        ad.show();
                        //start processing
                        drawFace.start();
                    }
                }else if(bn.getText().equals("Go To PreView")){
                    bn.setText(R.string.bottom_button);
                    if(mCamera != null)
                        mCamera.startPreview();
                }else
                    Toast.makeText(MainActivity.this,"@_@ SAD, ERROR"+bn.getText(),Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPause(){
        Log.d(TAG,"Activity onPause");
        super.onPause();
        if(mCamera!=null) {
            mCamera.release();
            mCamera = null;
        }

    }

    public void SetBMP(Bitmap bmp){
        this.READYTOSHOW = bmp;
    }

    public void FocusOnArea(MotionEvent event){
        x = event.getX();
        y = event.getY();
        Log.d(TAG,"touch area:"+x+"*"+y);
        Paint paint = new Paint();
        paint.setARGB(0x99, 0x3f, 0x51, 0xb5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        DrawRect(paint, true);

        if(mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            try{
                ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                focusAreas.add(new Camera.Area(new Rect(
                        (int)(((x-75)/txv.getWidth())*1831-915),
                        (int)(((y-75)/txv.getHeight())*1831-915),
                        (int)(((x+75)/txv.getWidth())*1831-915),
                        (int)(((y+75)/txv.getHeight())*1831-915)
                ),750));
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.cancelAutoFocus();
                parameters.setFocusAreas(focusAreas);

                mCamera.setParameters(parameters);
            }catch (Exception e){
                e.printStackTrace();
            }
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if(success) {
                        Log.d(TAG, "Focus!");
                        Paint paint = new Paint();
                        paint.setARGB(0xff,0x4c,0xaf,0x50);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        DrawRect(paint,false);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Focus ERROR", Toast.LENGTH_SHORT).show();
                        Paint paint = new Paint();
                        paint.setARGB(0xff,0xf4,0x43,0x36);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        DrawRect(paint,false);
                    }
                }
            });
        }
    }

    public void DrawRect(final Paint paint, Boolean isAnim){
        final Paint paint1 = paint;
        final int wid = 960, hei = 1280;
        if(isAnim) {
            if (blankBMP != null) blankBMP.recycle();
            blankBMP = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(blankBMP);
            canvas.drawRect(new Rect((int) (x - 150) * wid / 1080, (int) (y - 150) * hei / 1440, (int) (x + 150) * wid / 1080, (int) (y + 150) * hei / 1440), paint);
            fcv.setImageBitmap(blankBMP);

            //focus rect animation and hide
            final ScaleAnimation anim = new ScaleAnimation(1, 0.5f, 1, 0.5f, Animation.RELATIVE_TO_SELF, x / 1080, Animation.RELATIVE_TO_SELF, y / 1440);
            anim.setDuration(500);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    blankBMP = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(blankBMP);
                    paint1.setStrokeWidth(paint1.getStrokeWidth()/2);
                    canvas.drawRect(new Rect((int) (x - 75) * wid / 1080, (int) (y - 75) * hei / 1440, (int) (x + 75) * wid / 1080, (int) (y + 75) * hei / 1440), paint1);
                    fcv.setImageBitmap(blankBMP);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            Log.d(TAG, "Canvas should have been drawn");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fcv.startAnimation(anim);
                }
            }, 0);
        }else{
            //
            blankBMP = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(blankBMP);
            canvas.drawRect(new Rect((int) (x - 75) * wid / 1080, (int) (y - 75) * hei / 1440, (int) (x + 75) * wid / 1080, (int) (y + 75) * hei / 1440), paint1);
            fcv.setImageBitmap(blankBMP);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    blankBMP = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(blankBMP);
                    //canvas.drawRect(new Rect((int) (x - 75) * wid / 1080, (int) (y - 75) * hei / 1440, (int) (x + 75) * wid / 1080, (int) (y + 75) * hei / 1440), paint1);
                    fcv.setImageBitmap(blankBMP);
                }
            }, 500);
        }
    }

    public void ShootAndDraw() {
        //
        if(mCamera!=null){
            //
                        mCamera.takePicture(null, null, null,
                            new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data, Camera camera) {
                                    Toast.makeText(MainActivity.this,"Surprise!!",Toast.LENGTH_SHORT).show();
                                    drawFace = new DrawFace(MainActivity.this, data, 10);
                                    SHOOTOVER = 1;
                                }
                            });
        }
    }

}
