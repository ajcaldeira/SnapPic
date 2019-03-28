package com.example.snappic;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.squareup.picasso.Picasso;

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
    ArrayList<Contacts> arrayList = new ArrayList<>();

    //POP UP
    Dialog myDialog;
    ImageButton btnPopClose;
    TextView txtPopName;
    TextView txtPopPhoneNo;
    private FirebaseAuth mAuth;
    private int counter = 0;
    public static final String SHARED_PREFS = "ContactSP";
    public static final String SHARED_PREFS_SPECIFIC = "SpecificSP";



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

        //POP UP
        myDialog = new Dialog(this);
        btnPopClose = findViewById(R.id.btnPopClose);

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
                return false;
            }

        });

        listContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Contacts contact = arrayList.get(position);
                //Toast.makeText(ContactScreen.this, contact.getName(), Toast.LENGTH_SHORT).show();
                myDialog.setContentView(R.layout.custom_popup);
                txtPopName = myDialog.findViewById(R.id.txtPopName);
                txtPopName.setText(contact.getName());
                txtPopPhoneNo = myDialog.findViewById(R.id.txtPopPhoneNo);
                txtPopPhoneNo.setText(contact.getNumber());
                myDialog.show();
            }

        });


    }

    public void closePopUp(View v){
        myDialog.hide();
    }
    public void sendSnap(View v){
        //get the first shared prefs with all contacts
        String SHARED_PREFS = getSharedPrefContactVar();
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        //shared prefs with uid linked to phone number
        SharedPreferences contactSharedPrefsSpecific = getSharedPreferences(SHARED_PREFS_SPECIFIC, MODE_PRIVATE);

        int noContacts = contactSharedPref.getInt("noContacts",0);
        int loop_Incrementer = 0;
        while(loop_Incrementer != noContacts){
            String spName = "cUID" + String.valueOf(loop_Incrementer);
            String uid = contactSharedPref.getString(spName,"");
            String userNumber = contactSharedPrefsSpecific.getString(uid,"");
            if(userNumber.equals(txtPopPhoneNo.getText())){
                //if the number the user tapped on is equal to the current shared pref then end the loop
                //and continue to the main screen
                //Toast.makeText(this, userNumber, Toast.LENGTH_SHORT).show();
                loop_Incrementer = noContacts;
            }else {
                loop_Incrementer++;
            }
        }

        Intent sendSnapMain = new Intent(ContactScreen.this, MainActivity.class);
        sendSnapMain.putExtra("isToSend", true);
        sendSnapMain.putExtra("toSendName", true);
        sendSnapMain.putExtra("toSendNumber", true);
        startActivity(sendSnapMain);
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

    public void SaveSharedPrefs(String spName, String uid,String spUsersName,String contactSPRef,String spUsersNumber){
        SharedPreferences contactSharedPref = getSharedPreferences(contactSPRef,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactSharedPref.edit();
        editor.putString(spName, uid);
        editor.putString(uid, spUsersName);
        editor.apply();

        //another shared pref to connect the UIDs to the phone number as opposed to the name above
        SharedPreferences specificSendContact = getSharedPreferences(SHARED_PREFS_SPECIFIC,MODE_PRIVATE);
        SharedPreferences.Editor specificEditor = specificSendContact.edit();
        specificEditor.putString(uid,spUsersNumber);
        specificEditor.apply();
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
                    SaveSharedPrefs(spName,cUID,dbName,contactSPRef,dbNumber);
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


}
