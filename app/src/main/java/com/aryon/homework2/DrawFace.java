package com.aryon.homework2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.util.Log;

/**
 * Created by Aryon on 2015/7/14.
 */
public class DrawFace extends Thread implements Runnable {
    MainActivity mainActivity;
    byte[] img;
    int number;

    public DrawFace(MainActivity ac, byte[] data, int number){
        //
        mainActivity = ac;
        this.img = data;
        this.number = number;
    }

    public void run(){
        process(img,number);
    }

    public void process(byte[] data, int number){
        //
        Bitmap getbmp = BitmapFactory.decodeByteArray(data,0,data.length);

        Log.d("DrawFace", "FaceDetection working");
        Bitmap tempbmp;
        tempbmp = getbmp.copy(Bitmap.Config.RGB_565, true);//change into RGB565 format
        getbmp.recycle();
//rotate bmp
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap bmp = Bitmap.createBitmap(tempbmp, 0, 0, tempbmp.getWidth(), tempbmp.getHeight(), matrix, true);
        tempbmp.recycle();

        int FaceNumber = 1000;

//face detection process
        FaceDetector.Face[] faces = new FaceDetector.Face[number];
        FaceDetector fd = new FaceDetector(bmp.getWidth(),bmp.getHeight(),number);
        try {
            FaceNumber = fd.findFaces(bmp, faces);
            Log.d("MainActivity","FaceDetection findFaces over");
        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }

        Log.d("MainActivity","got face:"+FaceNumber);
        if(FaceNumber == 0)
            mainActivity.mhandler.sendEmptyMessage(mainActivity.NOFACEDETECTED);
        mainActivity.FaceNumber = FaceNumber;

//Draw bmp
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setARGB(0xaa,0x3F,0x51,0xB5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        PointF point = new PointF();
        FaceDetector.Face fs;
        for(int i = 0; i < FaceNumber; i++){
            fs = faces[i];
            if(fs.confidence()>0.4) {
                fs.getMidPoint(point);
                float dis = fs.eyesDistance();
                canvas.drawCircle(point.x - dis / 2, point.y, dis / 3, paint);
                canvas.drawCircle(point.x + dis / 2, point.y, dis / 3, paint);
                canvas.drawLine(point.x - dis / 6, point.y, point.x + dis / 6, point.y, paint);
            }
        }

        mainActivity.SetBMP(bmp);
        mainActivity.mhandler.sendEmptyMessage(mainActivity.PROCESSOVER);//process over, mainactivity ready to show the bmp
    }


}
