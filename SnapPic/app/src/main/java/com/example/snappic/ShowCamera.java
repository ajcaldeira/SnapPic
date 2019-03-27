package com.example.snappic;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera ;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

//extends as we need constructor
public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback{

    Camera cam;
    SurfaceHolder holder;


    public ShowCamera(Context context, Camera cam) {
        super(context);
        this.cam = cam;
        holder = getHolder();//holds our view
        holder.addCallback(this);
    }




    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = cam.getParameters();

        //auto focus on
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        //change the orientation of camera


        if(this.getResources().getConfiguration().orientation!= Configuration.ORIENTATION_LANDSCAPE){
            params.set("orientation","portrait");
            cam.setDisplayOrientation(90);
            params.setRotation(90);
        }else{
            params.set("orientation","landscape");
            cam.setDisplayOrientation(0);
            params.setRotation(0);
        }

        cam.setParameters(params);

        //if device doesnt have camera - try catch will stop app from totally crashing
        try{
            cam.setPreviewDisplay(holder);
            cam.startPreview();
        }catch(IOException e){
            e.printStackTrace();
        }


    }
}
