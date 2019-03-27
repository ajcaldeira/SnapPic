package com.example.snappic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterScreen extends AppCompatActivity {
    private Button btnReg;
    private EditText txtPhone;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        //firebase
        mAuth = FirebaseAuth.getInstance();

        btnReg = findViewById(R.id.btnReg);
        txtPhone = findViewById(R.id.txtPhone);



    }



    private void registerUser(){
        String phoneNumber = txtPhone.getText().toString().trim();
    }
    public void RegClick(View v)
    {
        if(v == btnReg) {
            registerUser();
        }
    }











}
