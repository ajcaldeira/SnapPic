package com.example.snappic;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class show_image extends AppCompatActivity {
    private String TOKEN_TO_SEND_TO = "";
    private int FLIP_ORIENTATION;//use this to flip it back before previewing
    private boolean TOKEN_IS_SENT = false;
    private Uri mImageUri;
    private StorageReference mStorageRef;
    private DatabaseReference mdbRef,dbRetrieveIn,dbRetrieve,dbStoryDate;
    private FirebaseAuth mAuth;
    private StorageTask mUploadTask;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    DatabaseReference dbTokenGetter;
    Button btnSendImage;
    Button btnStory;
    String mFileName;
    String mtoSendUID;
    private String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ORIENTATION FOR BUTTONS START
        OrientationEventListener mOrientationListener = new OrientationEventListener(
                getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                Log.d("WHATSMYORIENTATION", "onOrientationChanged: " + orientation);
                if ((orientation > 235 && orientation < 290)) {
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
                    btnStory.startAnimation(animRotateLogout);

                } else if ((orientation > 65 && orientation < 135)) {
                    AnimationSet animSet2;
                    animSet2 = new AnimationSet(true);
                    animSet2.setInterpolator(new DecelerateInterpolator());
                    animSet2.setFillAfter(true);
                    animSet2.setFillEnabled(true);
                    final RotateAnimation animRotate270 = new RotateAnimation(-90.0f, -90.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.3f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.2f);
                    animRotate270.setDuration(0);
                    animRotate270.setFillAfter(true);
                    animSet2.addAnimation(animRotate270);
                    btnStory.startAnimation(animSet2);

                }else if ((orientation > 0 && orientation < 310 && orientation != 65 && orientation != 290 && orientation != 271)) {
                    AnimationSet animSet2;
                    animSet2 = new AnimationSet(true);
                    animSet2.setInterpolator(new DecelerateInterpolator());
                    animSet2.setFillAfter(true);
                    animSet2.setFillEnabled(true);
                    final RotateAnimation animRotate270 = new RotateAnimation(00.0f, 0.0f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.7f);
                    animRotate270.setDuration(0);
                    animRotate270.setFillAfter(true);
                    animSet2.addAnimation(animRotate270);
                    btnStory.startAnimation(animSet2);

                }
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
        //ORIENTATION FOR BUTTONS END

        setContentView(R.layout.activity_show_image);

        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getUid();

       /*
            *VARIABLES FOR SENDING TO ANOTHER PERSON START*
       */
        //WHERE TO SAVE IN FIREBASE STORAGE
        mStorageRef = FirebaseStorage.getInstance().getReference("PendingSent");
        //WHERE TO SAVE IN DB
        mdbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRetrieve = mdbRef.child(uid);
        dbRetrieveIn = dbRetrieve.child("PendingSent");
        /*
         *VARIABLES FOR SENDING TO ANOTHER PERSON END*
        */

        btnSendImage = findViewById(R.id.btnSendImage);
        btnStory = findViewById(R.id.btnStory);
        //Image must be constructed into a bitmap

        //get  from previous activity:
        Intent intent = getIntent();
        String fileName = intent.getExtras().getString("filename");
        String toSendUID = intent.getExtras().getString("toSendUID");
        int flipOrientation = intent.getExtras().getInt("flipOrientation");
        mFileName = fileName;
        mtoSendUID = toSendUID;
        //make sure still not null
        if(fileName != null){
            //declare image view:
            ImageView img = findViewById(R.id.imageCaptured);

            //get time image in the file location with the passed filename
            File imgDirectory = Environment.getExternalStorageDirectory();
            File myImg = new File(imgDirectory + File.separator + fileName);

            if(myImg.exists()){
                //convert file to bitmap:
                Bitmap decodeImg = BitmapFactory.decodeFile(myImg.getAbsolutePath());

                //flip it for the preview
                switch (flipOrientation){
                    case 0:

                        break;
                    case 90:
                        Matrix matrix = new Matrix();
                        matrix.postRotate(360-90);
                        decodeImg = Bitmap.createBitmap(decodeImg,0,0,decodeImg.getWidth(),decodeImg.getHeight(),matrix,true);
                        break;
                    case 270:
                        Matrix matrix270 = new Matrix();
                        matrix270.postRotate(360-270);
                        decodeImg = Bitmap.createBitmap(decodeImg,0,0,decodeImg.getWidth(),decodeImg.getHeight(),matrix270,true);
                        break;
                }



                //show it on image view
                img.setImageBitmap(decodeImg);
                mImageUri = Uri.fromFile(myImg.getAbsoluteFile());
            }
        }

        //MAKE THE BUTTON INVISIBLE IF THERE IS NO UID TO SEND TO
        if(mtoSendUID.equals("")){
            btnSendImage.setVisibility(View.INVISIBLE);
        }
        btnSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //so the user cant spam upload
                if(mUploadTask !=null && mUploadTask.isInProgress()){
                    Toast.makeText(show_image.this,"Please Wait For Your Upload To Finish",Toast.LENGTH_SHORT).show();
                }else{
                    sendToSpecificUser(mtoSendUID);
                }
            }
        });
        btnStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //so the user cant spam upload
                if(mUploadTask !=null && mUploadTask.isInProgress()){
                    Toast.makeText(show_image.this,"Please Wait For Your Upload To Finish",Toast.LENGTH_SHORT).show();
                }else{
                    uploadFileStory();
                }
            }
        });
    }


    private String getFileExtensions(Uri uri){
        //REFERENCE: https://www.youtube.com/watch?v=lPfQN-Sfnjw 6:28
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    public void uploadFileStory(){
        //where to save in FireBase storage storage
        Date date = new Date();
        mStorageRef = FirebaseStorage.getInstance().getReference("Story").child(mAuth.getUid());
        //WHERE TO SAVE IN DB
        mdbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRetrieve = mdbRef.child(uid);
        dbRetrieveIn = dbRetrieve.child("Story");
        dbStoryDate = dbRetrieveIn.child(dateFormat.format(date));
        if(mImageUri != null){
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis() + uid + ".jpg" /*+ getFileExtensions(mImageUri)*/);
            mUploadTask = fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //reset progress bar and add delay or you will never see it. 12:40 in ref video above
                            //get download url
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {



                                    //check if there is already a story
                                    Upload upload = new Upload(mFileName.trim(),
                                            uri.toString());
                                    //creates new db entry with unique id
                                    dbStoryDate.removeValue();
                                    String uploadID = dbStoryDate.push().getKey();
                                    dbStoryDate.child(uploadID).setValue(upload);
                                    dbStoryDate.child(uploadID).child("timestamp").setValue(""+System.currentTimeMillis());
                                    Toast.makeText(show_image.this, "Set As Story!",Toast.LENGTH_SHORT).show();
                                    Intent mainAct = new Intent(show_image.this,MainActivity.class);
                                    startActivity(mainAct);
                                    finish();
                                }
                            });
                            //Toast.makeText(show_image.this, taskSnapshot.getMetadata().getReference().getDownloadUrl().toString(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(show_image.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //can put progress bar here
                        }
                    });
        }else{
            Toast.makeText(this,"Oops! Something went wrong",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        Intent mainActivity = new Intent(show_image.this,MainActivity.class);
        startActivity(mainActivity);
    }
    public void sendToSpecificUser(String SendUID){
        //where to save in FireBase storage storage
        Date date = new Date();
        final String timestampSend = "" + System.currentTimeMillis();
        mStorageRef = FirebaseStorage.getInstance().getReference("Story").child(mAuth.getUid());
        //WHERE TO SAVE IN DB
        mdbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRetrieve = mdbRef.child(SendUID);
        dbRetrieveIn = dbRetrieve.child("Received").child(uid);
        dbStoryDate = dbRetrieveIn.child(timestampSend);
        if(mImageUri != null){
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis() + uid + ".jpg" /*+ getFileExtensions(mImageUri)*/);
            mUploadTask = fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //reset progress bar and add delay or you will never see it. 12:40 in ref video above
                            //get download url
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Upload upload = new Upload(mFileName.trim(),
                                            uri.toString());
                                    //creates new db entry with unique id
                                    //String uploadID = dbStoryDate.push().getKey();
                                    dbStoryDate.setValue(upload);
                                    dbStoryDate.child("timestamp").setValue(timestampSend);
                                    //Toast.makeText(show_image.this, "Sending..",Toast.LENGTH_SHORT).show();
                                }
                            });
                            //Toast.makeText(show_image.this, taskSnapshot.getMetadata().getReference().getDownloadUrl().toString(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(show_image.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //can put progress bar here
                        }
                    });
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(show_image.this, "Cant send notification! Something Went Wrong! :(", Toast.LENGTH_SHORT).show();
                                return;
                            }


                            String token = getTokenToSendTo(mtoSendUID);//get token to send to
                            Log.d("TOKENFIREBASESEND", token);

                        }
                    });
        }else{
            Toast.makeText(this,"Oops! Something went wrong",Toast.LENGTH_SHORT).show();
        }
    }

    private String getTokenToSendTo(String uidToSend){

        dbTokenGetter = FirebaseDatabase.getInstance().getReference("Users").child(uidToSend);
        dbTokenGetter.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //check if users token is there
                //if not then return
                String tokenToSend = "";
                if(dataSnapshot.child("token").exists()) {
                    tokenToSend = dataSnapshot.child("token").getValue().toString();

                    TOKEN_TO_SEND_TO = tokenToSend;
                    if(!TOKEN_IS_SENT){//so it only sends once
                        //SENDTHE NOTIFICATION
                        new SendNotificationJava(TOKEN_TO_SEND_TO).execute();
                        Toast.makeText(show_image.this, "Sent!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(show_image.this, MainActivity.class);
                        startActivity(intent);
                        TOKEN_IS_SENT = true; //make it true so it doesnt send multiple times
                    }
                }else{
                    if(!TOKEN_IS_SENT) {
                        TOKEN_TO_SEND_TO = "";
                        Toast.makeText(show_image.this, "This user has not finished setting up their account yet!", Toast.LENGTH_LONG).show();
                        TOKEN_IS_SENT = true; //make it true so it doesnt alert multiple times
                        return;
                    }
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });

        dbTokenGetter = null;
        return TOKEN_TO_SEND_TO;
    }
}
