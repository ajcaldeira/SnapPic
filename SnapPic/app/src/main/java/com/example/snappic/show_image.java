package com.example.snappic;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    private Uri mImageUri;
    private StorageReference mStorageRef;
    private DatabaseReference mdbRef,dbRetrieveIn,dbRetrieve,dbStoryDate;
    private FirebaseAuth mAuth;
    private StorageTask mUploadTask;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    Button btnSendImage;
    Button btnStory;
    String mFileName;
    private String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        mFileName = fileName;
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
                //show it on image view
                img.setImageBitmap(decodeImg);

                mImageUri = Uri.fromFile(myImg.getAbsoluteFile());
            }
        }

        btnSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //so the user cant spam upload
                if(mUploadTask !=null && mUploadTask.isInProgress()){
                    Toast.makeText(show_image.this,"Please Wait For Your Upload To Finish",Toast.LENGTH_SHORT).show();
                }else{
                    uploadFile();
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
                                    Upload upload = new Upload(mFileName.trim(),
                                            uri.toString());
                                    //creates new db entry with unique id
                                    String uploadID = dbStoryDate.push().getKey();
                                    dbStoryDate.child(uploadID).setValue(upload);
                                    dbStoryDate.child(uploadID).child("timestamp").setValue(""+System.currentTimeMillis());
                                    Toast.makeText(show_image.this, "Set As Story!",Toast.LENGTH_SHORT).show();
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
    public void uploadFile(){
        if(mImageUri != null){
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis() + uid + "." + getFileExtensions(mImageUri));
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
                                    String uploadID = dbRetrieveIn.push().getKey();
                                    dbRetrieveIn.child(uploadID).setValue(upload);
                                    Toast.makeText(show_image.this, "Sent!",Toast.LENGTH_SHORT).show();
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
}
