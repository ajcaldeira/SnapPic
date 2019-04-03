package com.example.snappic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class LoginScreen extends AppCompatActivity {

    private int ALL_PERMISSION_CODE = 1;

    EditText txtPhoneNumber;
    EditText txtVerCode;
    Button btnGetNumber;
    Button btnWrongNumber;
    TextView txtError;
    TextView txtWrongNumber;
    ProgressBar progressBar;
    String phoneNumber;
    String codeSent;
    Button btnSubmitVerCode;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handlePermissions();
        setContentView(R.layout.activity_login_screen);

        txtPhoneNumber = findViewById(R.id.txtPhoneNumber);
        btnGetNumber = findViewById(R.id.btnGetNumber);
        txtError = findViewById(R.id.txtError);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        btnWrongNumber = findViewById(R.id.btnWrongNumber);
        txtWrongNumber = findViewById(R.id.txtWrongNumber);
        btnSubmitVerCode = findViewById(R.id.btnSubmitVerCode);
        btnGetNumber.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //String rawNumber = getPhoneNumber();
                //rawNumber = rawNumber.replace("00","+");
                //txtPhoneNumber.setText(rawNumber);
                progressBar.setVisibility(View.VISIBLE);
                phoneNumber = txtPhoneNumber.getText().toString();

                mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {

                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {

                    }

                    @Override
                    public void onCodeSent(String verificationId,PhoneAuthProvider.ForceResendingToken token) {



                    }
                };
                sendVerificationCode();
            }
        });


        btnSubmitVerCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCode();
            }
        });


        btnWrongNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtError.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                phoneNumber = txtPhoneNumber.getText().toString();
            }
        });



    }

    public void verifyCode(){

        String code = txtVerCode.getText().toString();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeSent, code);
        signInWithPhoneAuthCredential(credential);
    }

    public String getPhoneNumber(){
        String autoPhoneNumber;
        if (ContextCompat.checkSelfPermission(LoginScreen.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            //already has permissions
            TelephonyManager tMgr = (TelephonyManager)this.getSystemService(this.TELEPHONY_SERVICE);
            autoPhoneNumber = tMgr.getLine1Number();
        }else {
            handlePermissions();
            autoPhoneNumber = "";
        }

        return autoPhoneNumber;
    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            Intent mainScreen = new Intent(LoginScreen.this, MainActivity.class);
            startActivity(mainScreen);
        }

    }

    private void sendVerificationCode(){

        String phone = txtPhoneNumber.getText().toString();
        if(phone.isEmpty()){
            Toast.makeText(this, "ERROR!", Toast.LENGTH_SHORT).show();
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks
        );
    }



//ver
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User successfully verified

                            CheckUserExistsInDatabase();

                        } else {
                            // Sign in failed, display a message and update the UI
                            progressBar.setVisibility(View.INVISIBLE);
                            txtError.setText("Error!!");
                            txtError.setVisibility(View.VISIBLE);
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    public void CheckUserExistsInDatabase(){
        //CHECK IF USER IS REGISTERED IN DB
        final int[] userInDB = {0};
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                txtError.setVisibility(View.VISIBLE);
                txtError.setText("HERE");
                userInDB[0] = 0;
                //iterate through db and check if the number the user just used to sign up exists already
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    User users = userSnapshot.getValue(User.class);
                    String dbNumber = users.number;
                    if(dbNumber.equals(phoneNumber)){
                        //if number exists take user to main screen as they already have an account
                        txtError.setVisibility(View.VISIBLE);
                        GoToMainScreen();
                        txtError.setText("Welcome Back " + users.name);
                        break;
                    }else{
                        GoToRegScreen();
                        finish();
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }

    public void GoToRegScreen(){
        Intent regScreen = new Intent(LoginScreen.this, RegsiterUser.class);
        //send phone number that was successfully verified
        regScreen.putExtra("phoneNumber",phoneNumber);
        startActivity(regScreen);
    }
    public void GoToMainScreen(){
        Intent mainScreen = new Intent(LoginScreen.this, MainActivity.class);
        startActivity(mainScreen);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        handlePermissions();
    }

    private void handlePermissions(){

        //ARRAY OF PERMISSIONS
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.INTERNET
        };

        //ARE THESE PERMISSIONS GIVEN
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[2]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[3]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[4]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[5]) == PackageManager.PERMISSION_GRANTED)
        {
            //IF THEY ARE GRANTED THEN:
        }else{
            //IF NOT GRANTED, ASK FOR THEM:
            ActivityCompat.requestPermissions(LoginScreen.this,permissions,ALL_PERMISSION_CODE);
        }

    }






























}
