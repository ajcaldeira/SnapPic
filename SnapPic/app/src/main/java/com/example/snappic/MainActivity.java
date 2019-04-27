package com.example.snappic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_TORCH;
import static android.hardware.camera2.CameraMetadata.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_APPROXIMATE;

public class MainActivity extends AppCompatActivity {
    //CAMERA
    private int ALL_PERMISSION_CODE = 1;
    private int FLIP_ORIENTATION;
    private String CAMERA_BACK;
    private String CAMERA_FRONT;
    private String CAMERA_FACE;
    private boolean CAMERA_SWAP;
    private boolean IS_CAM_DC;
    private int DEVICE_ROTATION;
    private String mFileName;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private  int mCaptureState = STATE_PREVIEW;//AS WE START ON PREVIEW ALWAYS
    private int mTotalRotation;

    //defined on main thread, therefore can only work on the main thread
    private Handler ContactHandler = new Handler();

    private int testWidth;
    private int testHeight;

    DatabaseReference dbTokenWriter;
    FrameLayout frameLayout;
    ToggleButton btnFlash;
    TextureView textureView;
    Button btnIsSend;
    //this will be true if the user wants to send a pic to another user FROM the contact screen
    private boolean isToSend = false;
    String toSendUidPassed = "";
    private FirebaseAuth mAuth;


    private float x1, x2,y1,y2;
    static final int MIN_DISTANCE = 100;

    //OVERIDE BACK BUTTON PRESS
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        Intent mainActivity = new Intent(MainActivity.this,MainActivity.class);
        startActivity(mainActivity);
    }
    /******************CAMERA2******************/
    private TextureView.SurfaceTextureListener mSurfaceTexListener = new TextureView.SurfaceTextureListener() {
        @SuppressLint("NewApi")
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            ConnectToCamera();
            startPreview();
            Log.d("onDisconnectedCAM", "onSurfaceTextureAvailable: hit");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d("onDisconnectedCAM", "onDisconnected: ");
            //StopBackgroundThread();
            if(!IS_CAM_DC){
                camera.close();
                mCameraDevice = null;
            }else{
                IS_CAM_DC = false;
                textureView = null;



            }

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void closeCamera() {

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }

    private String mCameraID;
    private Size mPreviewSize;

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onPause() {
        //closeCamera();
        //StopBackgroundThread();
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void SetUpCamera(int width, int height) {
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : camManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = camManager.getCameraCharacteristics(cameraID);
                if(CAMERA_SWAP){

                    if(CAMERA_FACE.equals("1")){
                        CAMERA_FACE = "0";
                        cameraCharacteristics = camManager.getCameraCharacteristics(CAMERA_FACE);
                    }else{
                        CAMERA_FACE = "1";
                        cameraCharacteristics = camManager.getCameraCharacteristics(CAMERA_FACE);
                    }
                }else{
                    //for its first run ONLY so it starts with front cam always
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        CAMERA_BACK = cameraID;
                        CAMERA_FACE = CAMERA_BACK;
                        continue; //restart loop
                    }else{
                        CAMERA_FRONT = cameraID;
                        CAMERA_FACE = CAMERA_FRONT;
                    }
                }

                //Toast.makeText(MainActivity.this,debug + CAMERA_FACE,Toast.LENGTH_SHORT).show();
                //gets a list of available resolutions
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                //Orientation. portrait or not?
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = senseorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                Log.d("ROTATIONTAG", String.valueOf(totalRotation));
                mTotalRotation = totalRotation;
                //swap rotation if totRotation = 90 or 270. makes bool true else false
                boolean doSwapRotation = totalRotation == 90 || totalRotation == 270;
                Log.d("ROTATIONTAG", String.valueOf(doSwapRotation));
                //getting this width from frame layout (passed when this function is called
                int rotatedWidth = width;
                int rotaredHeight = height;
                if (doSwapRotation) {
                    //swap height and width as phone will be sideways and they will essentially be swapped
                    rotatedWidth = height;
                    rotaredHeight = width;
                }
                //map.getoutputsize get a list of the preview resolutions and then we pick the optimal with the frameLayouts w and h
                //so we may not get the exact resolution, but we get the closest match
                mPreviewSize = pickBestSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotaredHeight);
                //HANDLE 2K AND 4K
                mImageSize = pickBestSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotaredHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(),mImageSize.getHeight(), ImageFormat.JPEG,1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                if(CAMERA_SWAP){
                    mCameraID = CAMERA_FACE;
                }else {
                    mCameraID = cameraID;
                }
                //CAMERA_SWAP = false;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //FIXING ORIENTATION
    //3 is rotating right
    //1 is left
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void fixOrientation(int width, int height){
        if(mPreviewSize == null || textureView == null){
            return;
        }
        Log.d("ROTATIONTAG2", "w: " + width + " height: " + height);
        Log.d("ROTATIONTAG2", "mw: " + width + " mheight: " + height);
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0,0,width,height);
        RectF previewRectF = new RectF(0,0,height,width);
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            Log.d("ROTATIONTAG2", "fixOrientation: " + rotation);
            previewRectF.offset(centerX ,centerY );
            matrix.setRectToRect(textureRectF,previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max(1,1);
            matrix.postScale(scale,scale,centerX,centerY);
            matrix.postRotate(90 * (rotation - 2), centerX,centerY);
            if(rotation == Surface.ROTATION_270 ){
                matrix.postTranslate(Math.round(((float)width - (float)height)),Math.round(((float)height - (float)width) * 0.5f));
            }else{
                matrix.postTranslate(-Math.round(((float)width - (float)height)),-Math.round(((float)height - (float)width) * 0.5f));
            }

        }
        textureView.setTransform(matrix);

    }


    //BACKGROUND THREAD - so it doesn't run on the UI thread and effect ui behaviour
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private void StartBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void StopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();//doesnt let other apps use thread
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //DEVICE ORIENTATION
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int senseorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        //mod 360 to keep in in the 360 range
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Size pickBestSize(Size[] choices, int width, int height) {
        //make a list to put all acceptable values into
        //FOV from sensor is big enough
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            //aspect ratio check
            if (option.getHeight() == option.getWidth() * height / width &&
                    //make sure heights match:
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);//add to list
            }
        }
        //make sure we found a size to put in our list
        if (bigEnough.size() > 0) {
            //will return the minimum value in the list
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            //return a default
            return choices[0];
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void ConnectToCamera() {
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission Error",Toast.LENGTH_SHORT).show();
                return;
            }
            camManager.openCamera(mCameraID, mCameraDeviceStateCallback, mBackgroundHandler);
            //Toast.makeText(MainActivity.this, "Cam Connected",Toast.LENGTH_SHORT).show();


        }catch(CameraAccessException e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "no",Toast.LENGTH_SHORT).show();
        }
    }
    private CameraCaptureSession mPreviewCapSession;

    //using this callback for capturing while in preview mode
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraCaptureSession.CaptureCallback mPreviewCapCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult capResult){
            //check which state were in
            switch (mCaptureState){
                case STATE_PREVIEW:
                    break;//do nothing coz this is what we want
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW; //do this so it doesnt keep taking pictures
                    Integer afState = capResult.get(CaptureResult.CONTROL_AF_STATE);
                    startStillCaptureRequest();
                   // if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){

                   // }
                    break;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);

        }
    };

    private CaptureRequest.Builder mCapReqBuilder;
    //Got to have a request builder to put all info into our request
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPreview() {

        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();

        // Toast.makeText(MainActivity.this,surfaceTexture.toString(),Toast.LENGTH_SHORT).show();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            if(mCameraDevice == null){
                if(CAMERA_SWAP){
                    if(CAMERA_BACK.equals(CAMERA_FACE)){
                        CAMERA_FACE = CAMERA_FRONT;
                    }else{
                        CAMERA_FACE = CAMERA_BACK;
                    }
                }
                SetUpCamera(testWidth,testHeight);
            }
            mCapReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mCapReqBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    //session is configures and set up
                    //next thing to do is set up our request to session
                    mPreviewCapSession = session;
                    try {
                        mPreviewCapSession.setRepeatingRequest(mCapReqBuilder.build(),null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();

                }
            },null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startStillCaptureRequest(){

        if(!isNetworkAvailable()){
            networkRule();
        }else{
            try {
                mCapReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                mCapReqBuilder.addTarget(mImageReader.getSurface());
                mCapReqBuilder.set(CaptureRequest.JPEG_ORIENTATION,mTotalRotation);

                //we need a custom capture callback
                CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                        String rotOrientation = String.valueOf(FLIP_ORIENTATION);
                        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
                        //if its the back camera add a prefix of 'b' to the name
                        if(CAMERA_FACE.equals(CAMERA_BACK)){
                            mFileName = "b_" + rotOrientation + "_SnapPic_" + timeStamp + ".jpg";
                        }else{
                            mFileName = rotOrientation + "_SnapPic_" + timeStamp + ".jpg";
                        }


                    }
                };
                mPreviewCapSession.capture(mCapReqBuilder.build(),stillCaptureCallback, null);//null coz we already in the background thread
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //TAKING PHOTOS
    private Size mImageSize;
    private ImageReader mImageReader;
    //will let us know when an image is captured
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireNextImage();
            mBackgroundHandler.post(new ImageSave(img));
        }
    };

    private class ImageSave implements Runnable{

        private final Image mImage;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public ImageSave(Image image){
            mImage = image;
        }
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
            Bitmap decodeImg;
            decodeImg = BitmapFactory.decodeByteArray(data, 0, data.length);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            //flip the image depending on the orientation it was saved
            int flipOrientation = FLIP_ORIENTATION;
            if(CAMERA_FACE.equals(CAMERA_BACK)){
                flipOrientation = 0;
            }
            switch (flipOrientation){
                case 90:
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);
                    decodeImg = Bitmap.createBitmap(decodeImg,0,0,decodeImg.getWidth(),decodeImg.getHeight(),matrix,true);

                    break;
                case 270:
                    Matrix matrix270 = new Matrix();
                    matrix270.postRotate(-270);
                    decodeImg = Bitmap.createBitmap(decodeImg,0,0,decodeImg.getWidth(),decodeImg.getHeight(),matrix270,true);
                    break;
            }

            //flip mirror image from front cam
            if(CAMERA_FACE.equals(CAMERA_FRONT)){
                Float cx = decodeImg.getWidth()/2f;
                Float cy = decodeImg.getHeight()/2f;

                Matrix matrixMirror = new Matrix();
                matrixMirror.postScale(-1, 1, cx, cy);

                Bitmap flippedImg;
                flippedImg = Bitmap.createBitmap(decodeImg,0,0,decodeImg.getWidth(),decodeImg.getHeight(),matrixMirror,true);
                flippedImg.compress(Bitmap.CompressFormat.JPEG, 15, bytes);
            }else{
                decodeImg.compress(Bitmap.CompressFormat.JPEG, 15, bytes);
            }

            File ExternalStorageDirectory = Environment.getExternalStorageDirectory();
            File file = new File(ExternalStorageDirectory + File.separator + mFileName);

            FileOutputStream fileOutputStream = null;
            try {
                file.createNewFile();
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes.toByteArray());
                fileOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                closeCamera();
            }
            //Send the filename and call the other intent
            Intent intent  = new Intent(MainActivity.this, show_image.class);
            intent.putExtra("filename", mFileName);
            intent.putExtra("toSendUID",  toSendUidPassed);
            intent.putExtra("flipOrientation",  flipOrientation);
            startActivity(intent);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void LockCameraFocus(){
        mCaptureState = STATE_WAIT_LOCK;
        mCapReqBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);

        //TurnOnFlash();
        try {
            mPreviewCapSession.capture(mCapReqBuilder.build(),mPreviewCapCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void TurnOnFlash(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraManager.setTorchMode(CAMERA_BACK,true);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    private void TurnOffFlash(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraManager.setTorchMode(CAMERA_BACK,true);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /******************CAMERA2 END*****************/

    public void LogOut(View v){
        mAuth.signOut();
        BackToLogin();
    }
    public void BackToLogin(){
        Intent loginScreen = new Intent(MainActivity.this,LoginScreen.class);
        startActivity(loginScreen);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onStart() {
        super.onStart();
        networkRule();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(textureView.isAvailable()){

        }else{
            textureView.setSurfaceTextureListener(mSurfaceTexListener);
        }
        //CHECK IF THE USER IS LOGGED IN, THIS WILL USUALLY RUN ON THE FIRST
        //RUN OF THE APP OR IF THEY LOG OUT
        if(currentUser == null){
            //user not logged in
           BackToLogin();
        }

        //write notification token of the user to db
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {

                String token = task.getResult().getToken();//gets my token
                //Log.d("TOKENFIREBASE", token);
                writeTokenToDB(token);
                dbTokenWriter = null;


            }
        });

        StartBackgroundThread();
        Intent serviceIntent = new Intent(MainActivity.this, ContactFetchIntentService.class);
        startService(serviceIntent);

    }
    private void writeTokenToDB(final String token){
        dbTokenWriter = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid());
        dbTokenWriter.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //check if my token is there
                //if not then write it
                if(dataSnapshot.child("token").exists()){
                    if(!dataSnapshot.child("token").getValue().toString().equals(token)){
                        //token doesnt match the one in the db
                        //update it
                        dataSnapshot.child("token").getRef().setValue(token);
                    }
                }else{
                    //the user does not have a token
                    dataSnapshot.child("token").getRef().setValue(token);
                }
                return;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });
        dbTokenWriter = null;


    }

    ImageView squirrelImg;
    Button btnSwapCam;
    Button btnLogout;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OrientationEventListener mOrientationListener = new OrientationEventListener(
                getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                btnLogout = findViewById(R.id.btnLogOut);
                squirrelImg = findViewById(R.id.imageView3);
                Log.d("WHATSMYORIENTATION", "onOrientationChanged: " + orientation);
                if ((orientation > 235 && orientation < 290)) {
                    FLIP_ORIENTATION = 270;
                    AnimationSet animSet ;
                    animSet = new AnimationSet(true);
                    animSet.setInterpolator(new DecelerateInterpolator());
                    animSet.setFillAfter(true);
                    animSet.setFillEnabled(true);
                    final RotateAnimation animRotate90 = new RotateAnimation(90.0f, -270.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.6f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.6f);
                    animRotate90.setDuration(0);
                    animRotate90.setFillAfter(true);
                    animSet.addAnimation(animRotate90);
                    squirrelImg.startAnimation(animSet);
                    btnSwapCam.startAnimation(animSet);

                    AnimationSet animSetLogout ;
                    animSetLogout = new AnimationSet(true);
                    animSetLogout.setInterpolator(new DecelerateInterpolator());
                    animSetLogout.setFillAfter(true);
                    animSetLogout.setFillEnabled(true);
                    final RotateAnimation animRotateLogout = new RotateAnimation(90.0f, -270.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.6f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.05f);
                    animRotateLogout.setDuration(0);
                    animRotateLogout.setFillAfter(true);
                    animSetLogout.addAnimation(animRotateLogout);
                    btnLogout.startAnimation(animSetLogout);
                    if(isToSend){
                        AnimationSet animSetIsToSend;
                        animSetIsToSend = new AnimationSet(true);
                        animSetIsToSend.setInterpolator(new DecelerateInterpolator());
                        animSetIsToSend.setFillAfter(true);
                        animSetIsToSend.setFillEnabled(true);
                        final RotateAnimation animRotateSend = new RotateAnimation(90.0f, -270.0f,
                                RotateAnimation.RELATIVE_TO_SELF, 0.8f,
                                RotateAnimation.RELATIVE_TO_SELF, 0.6f);
                        animRotateSend.setDuration(0);
                        animRotateSend.setFillAfter(true);
                        animSetIsToSend.addAnimation(animRotateSend);
                        btnIsSend.startAnimation(animSetIsToSend);
                    }
                } else if ((orientation > 65 && orientation < 135)) {
                    FLIP_ORIENTATION = 90;
                    AnimationSet animSet2;
                    animSet2 = new AnimationSet(true);
                    animSet2.setInterpolator(new DecelerateInterpolator());
                    animSet2.setFillAfter(true);
                    animSet2.setFillEnabled(true);
                    final RotateAnimation animRotate270 = new RotateAnimation(-90.0f, -90.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                    animRotate270.setDuration(0);
                    animRotate270.setFillAfter(true);
                    animSet2.addAnimation(animRotate270);
                    squirrelImg.startAnimation(animSet2);
                    btnSwapCam.startAnimation(animSet2);
                    btnLogout.startAnimation(animSet2);
                    if(isToSend){
                        AnimationSet animSetIsToSend;
                        animSetIsToSend = new AnimationSet(true);
                        animSetIsToSend.setInterpolator(new DecelerateInterpolator());
                        animSetIsToSend.setFillAfter(true);
                        animSetIsToSend.setFillEnabled(true);
                        final RotateAnimation animRotateSend = new RotateAnimation(-90.0f, -90.0f,
                                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                                RotateAnimation.RELATIVE_TO_SELF, 0.1f);
                        animRotateSend.setDuration(0);
                        animRotateSend.setFillAfter(true);
                        animSetIsToSend.addAnimation(animRotateSend);
                        btnIsSend.startAnimation(animSetIsToSend);
                    }
                }else if ((orientation > 0 && orientation < 310 && orientation != 65 && orientation != 290 && orientation != 271)) {
                    //normal portrait
                    FLIP_ORIENTATION = 0;
                    AnimationSet animSet2;
                    animSet2 = new AnimationSet(true);
                    animSet2.setInterpolator(new DecelerateInterpolator());
                    animSet2.setFillAfter(true);
                    animSet2.setFillEnabled(true);
                    final RotateAnimation animRotate270 = new RotateAnimation(00.0f, 0.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                    animRotate270.setDuration(0);
                    animRotate270.setFillAfter(true);
                    animSet2.addAnimation(animRotate270);
                    squirrelImg.startAnimation(animSet2);
                    btnSwapCam.startAnimation(animSet2);
                    btnLogout.startAnimation(animSet2);

                    if(isToSend){
                        btnIsSend.startAnimation(animSet2);
                    }



                }
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);


        Log.d("DOIHAVEINTERNET", "onCreate: " + isNetworkAvailable());

        //FireBase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        //Get info from contact intent
        btnIsSend = findViewById(R.id.btnIsSend);
        try{
            Intent intent = getIntent();
            isToSend = intent.getExtras().getBoolean("isToSend");
            toSendUidPassed = intent.getExtras().getString("toSendUID");

        }catch(Exception e){
            e.getStackTrace();
        }

        //IF THE USER IS tryign to send from the contact activity this will be true
        //else the cancel button will not show as there is nothing to cancel
        if(isToSend){
            btnIsSend.setVisibility(View.VISIBLE);
        }else{
            btnIsSend.setVisibility(View.INVISIBLE);

        }
        btnIsSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        //swap camera
        btnSwapCam = findViewById(R.id.btnSwapCam);
        btnSwapCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CAMERA_SWAP = true;
                closeCamera();
                SetUpCamera(testWidth,testHeight);
                ConnectToCamera();
                startPreview();
            }
        });

        //prepare the frame layout

        textureView = findViewById(R.id.textureView);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            /*******************NEW CAMERA *****************/
            //have to use this as the framlayout w and h waere being queried before the layout was drawn:
            //https://stackoverflow.com/a/21926714
            ViewTreeObserver vto = textureView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        MainActivity.this.textureView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        MainActivity.this.textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    int width  = textureView.getMeasuredWidth();
                    int height = textureView.getMeasuredHeight();
                    testWidth = width;
                    testHeight = height;
                    SetUpCamera(testWidth,testHeight);
                    //make sure orientation is correct
                    DEVICE_ROTATION = getWindowManager().getDefaultDisplay().getRotation();
                    if(DEVICE_ROTATION == 1 || DEVICE_ROTATION == 3){
                        //0 normal
                        //3 right
                        //1 left
                        Log.d("ONCREATEDEVICEROTATION", "onCreate: " + DEVICE_ROTATION);
                        Log.d("ONCREATEDEVICEROTATION", "width: " + testWidth);
                        Log.d("ONCREATEDEVICEROTATION", "height: " + testHeight);
                        fixOrientation(testWidth,testHeight);
                        //fixOrientation(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                    }
                }
            });

        }else {
            handlePermissions();
        }
    }

    //CHECK IF THERE IS AN INTERNET CONNECTION
    //https://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void networkRule(){
        if(!isNetworkAvailable()){
            new AlertDialog.Builder(this)
                    .setTitle("Internet Needed")
                    .setMessage("Please connect to the internet before using this app!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create().show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(!CAMERA_SWAP){
            if(textureView.isAvailable()){

            }else{
                textureView.setSurfaceTextureListener(mSurfaceTexListener);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean onTouchEvent(MotionEvent event){

        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;
                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    if(deltaX > 0){
                        // LEFT TO RIGHT

                        closeCamera();
                        IS_CAM_DC = true;
                        mSurfaceTexListener = null;
                        textureView.setSurfaceTextureListener(mSurfaceTexListener);
                        //mCameraDeviceStateCallback.onDisconnected(mCameraDevice);
                        Intent intent = new Intent(MainActivity.this, ContactScreen.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                        finish();


                    }else{
                        //swiped RIGHT to LEFT
                        Intent intent = new Intent(this, RightScreen.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                    }
                }
                else if(Math.abs(deltaY) > MIN_DISTANCE)
                {
                    //swipe down

                }
                break;
        }
        return super.onTouchEvent(event);

    }

    //THE FUNCTION CALLED ON BUTTON CLICK TO TAKE THE PHOTO

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void CaptureImage(View v){
        LockCameraFocus();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        handlePermissions();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handlePermissions(){

        //ARRAY OF PERMISSIONS
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };

        //ARE THESE PERMISSIONS GIVEN
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[2]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[3]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[4]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[5]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[6]) == PackageManager.PERMISSION_GRANTED
        )
        {
            //IF THEY ARE GRANTED THEN:

        }else{
            //IF NOT GRANTED, ASK FOR THEM:
            ActivityCompat.requestPermissions(MainActivity.this,permissions,ALL_PERMISSION_CODE);
        }

    }
}
