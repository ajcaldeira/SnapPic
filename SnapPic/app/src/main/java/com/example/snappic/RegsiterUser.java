package com.example.snappic;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RegsiterUser extends AppCompatActivity {


    Button btnRegister;
    EditText txtFullName;
    TextView txtErrorRegister;
    ProgressBar progressBarReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regsiter_user);

        btnRegister = findViewById(R.id.btnRegister);
        txtFullName = findViewById(R.id.txtFullName);
        txtErrorRegister = findViewById(R.id.txtErrorRegister);
        progressBarReg = findViewById(R.id.progressBarReg);
        final String phoneNumber = getIntent().getStringExtra("phoneNumber");


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //IF THE FIELD IS NOT EMPTY
                progressBarReg.setVisibility(View.VISIBLE);
                String userFullName = txtFullName.getText().toString();
                if(userFullName != ""){
                    //CREATE USER OBJECT TO STORE CUSTOM DATA
                    User userReg = new User(userFullName,phoneNumber);
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(userReg)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        //user registered correctly
                                        Intent mainScreen = new Intent(RegsiterUser.this, MainActivity.class);
                                        startActivity(mainScreen);
                                        progressBarReg.setVisibility(View.GONE);
                                        finish();
                                    }
                                }
                            });
                }else{
                    progressBarReg.setVisibility(View.INVISIBLE);
                    txtErrorRegister.setVisibility(View.VISIBLE);
                    txtErrorRegister.setText("Please Enter a valid first name");
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        txtErrorRegister.setVisibility(View.INVISIBLE);

    }
}
