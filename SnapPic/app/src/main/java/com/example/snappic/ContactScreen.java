package com.example.snappic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ContactScreen extends AppCompatActivity  {


    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    ListView listContacts;
    TextView txtTester;
    TextView txtContName;
    TextView txtContNumber;
    ProgressBar progressBarContact;
    DatabaseReference dbRef;
    DatabaseReference dbContacts;
    DatabaseReference dbContactsSingle;
    private FirebaseAuth mAuth;
    private int counter = 0;
    public static final String SHARED_PREFS = "ContactSP";



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_screen);
        txtTester = findViewById(R.id.txtTester);
        txtContName = findViewById(R.id.txtContName);
        txtContNumber = findViewById(R.id.txtContNumber);
        listContacts = findViewById(R.id.listContacts);
        progressBarContact = findViewById(R.id.progressBarContact);
        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getUid();
        txtTester.setText(uid);
        GetUserContacts(uid,false);

        listContacts.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        float deltaX = x2 - x1;
                        if (Math.abs(deltaX) > MIN_DISTANCE)
                        {
                            if(deltaX > 0){
                                // LEFT TO RIGHT

                            }else{
                                //swiped RIGHT to LEFT
                                Intent intent = new Intent(ContactScreen.this, MainActivity.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                            }
                        }
                        else
                        {
                            // consider as something else - a screen tap for example
                        }
                        break;
                }
                return true;
            }
        });



    }

    public String getSharedPrefContactVar(){
        return SHARED_PREFS;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        Intent mainActivity = new Intent(ContactScreen.this,MainActivity.class);
        startActivity(mainActivity);
    }

    public void SaveSharedPrefs(String spName, String uid,String spUsersName,String contactSPRef){
        SharedPreferences contactSharedPref = getSharedPreferences(contactSPRef,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactSharedPref.edit();
        editor.putString(spName, uid);
        editor.putString(uid, spUsersName);
        editor.apply();

        //Toast.makeText(ContactScreen.this, "s", Toast.LENGTH_SHORT).show();
    }

    public String LoadSharedPrefs(String spName,String spKey){
        SharedPreferences contactSharedPref = getSharedPreferences(spName, MODE_PRIVATE);
        String currentSP = contactSharedPref.getString(spKey,"");

        return currentSP;
    }


//TRYING TO GET THE USERS CONTATCS, FIRST GET THE USER WE WANNA GET THE CONTACTS OF THEN TRY ITTERATE THROUGH THE CONTACTS
    public void GetUserContacts(String uid,final boolean isClassCall){
        //GET THE USER'S CONTACTS
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbContacts = dbRef.child(uid);
        dbContactsSingle = dbContacts.child("Contacts");
        dbContactsSingle.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already
                ArrayList<Contacts> arrayList = new ArrayList<>();
                counter = 0;
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    User users = userSnapshot.getValue(User.class);
                    String dbNumber = users.number;
                    String dbName = users.name;
                    //gets UID from parent node in DB
                    String cUID = userSnapshot.getKey();
                    //create contact object and pass the data into it
                    Contacts contact = new Contacts(dbName, dbNumber);

                    String spName = "cUID" + String.valueOf(counter);
                    String contactSPRef = getSharedPrefContactVar();
                    arrayList.add(contact);
                    SaveSharedPrefs(spName,cUID,dbName,contactSPRef);
                    /*
                    if(!isClassCall){

                    }else{


                        MainActivity mainAct = new MainActivity();
                        mainAct.contactHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SaveSharedPrefs(spName,cUID,dbName,contactSPRef);
                            }
                        });

                    }
                        */
                    counter++;
                }
                if(!isClassCall){
                    progressBarContact.setVisibility(View.INVISIBLE);
                    ContactsListAdapter adapter = new ContactsListAdapter(ContactScreen.this, R.layout.custom_layout, arrayList);
                    listContacts.setAdapter(adapter);
                }

                    //ArrayAdapter arrayAdapter = new ArrayAdapter(ContactScreen.this, android.R.layout.simple_list_item_1, arrayList);
                    //listContacts.setAdapter(arrayAdapter);

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }

    public boolean onTouchEvent(MotionEvent event){

        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    if(deltaX > 0){
                        // LEFT TO RIGHT

                    }else{
                        //swiped RIGHT to LEFT
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                    }
                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);

    }

}
