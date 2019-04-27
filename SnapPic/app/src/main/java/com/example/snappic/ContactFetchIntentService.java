package com.example.snappic;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContactFetchIntentService extends IntentService {

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    DatabaseReference dbContacts;
    DatabaseReference dbContactsSingle;
    DatabaseReference dbCheckForMessages;
    private int counter;
    public static final String SHARED_PREFS = "ContactSP";
    public static final String SHARED_PREFS_REQ_CONTACTS = "ContactREQ";
    public static final String SHARED_PREFS_MESSAGES = "messagesSP";


    // This schedule a runnable task every 2 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();

    }

    public ContactFetchIntentService() {
        super("ContactFetchIntentService");
        setIntentRedelivery(true);
    }
    public String getSharedPrefContactVar(){
        return SHARED_PREFS;
    }
    public void SaveSharedPrefs(String spName, String uid,String spUsersName,String contactSPRef){
        SharedPreferences contactSharedPref = getSharedPreferences(contactSPRef,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactSharedPref.edit();
        editor.putString(spName, uid);
        editor.putString(uid, spUsersName);
        editor.apply();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String uid = mAuth.getUid();

        GetUserContacts(uid,false);
        CheckIfUserHasContactRequest();
        checkUserHasMessages();
        Log.d("ISMYSERVICERUNNING", "RUNNING!");
    }

    public void checkUserHasMessages(){
        //number of contacts
        SharedPreferences noContactSharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        int noOfContacts = noContactSharedPref.getInt("noContacts",0);

        for(int i = 0; i < noOfContacts; i++){
            String currentUID = noContactSharedPref.getString("cUID" + i,"");
            checkUserHasMessagesDB(currentUID);

        }
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
                //ArrayList<Contacts> arrayList = new ArrayList<>();
                counter = 0;
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    User users = userSnapshot.getValue(User.class);
                    String dbNumber = users.number;
                    String dbName = users.name;
                    //gets UID from parent node in DB
                    String cUID = userSnapshot.getKey();
                    //create contact object and pass the data into it
                    //Contacts contact = new Contacts(dbName, dbNumber);

                    String spName = "cUID" + String.valueOf(counter);
                    String contactSPRef = getSharedPrefContactVar();
                    SaveSharedPrefs(spName,cUID,dbName,contactSPRef);
                    counter++;
                }
                SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
                SharedPreferences.Editor editor = contactSharedPref.edit();
                editor.putInt("noContacts", counter);
                editor.apply();

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }

    //update the number of contact requests the user has (top left)
    public void updateNumContactReq(int noContacts){
        SharedPreferences contactSharedPrefReq = getSharedPreferences(SHARED_PREFS_REQ_CONTACTS,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactSharedPrefReq.edit();
        editor.putInt("numOfContactsRequests", noContacts);
        editor.apply();
    }


    //check if the user has a new contact request
    public void CheckIfUserHasContactRequest(){
        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid());
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already
                    if(true){
                        //check if the user already sent a contact request
                        if(dataSnapshot.child("ReceivedContactRequests").exists()){
                            //new requests
                            int noContactReq = (int)dataSnapshot.child("ReceivedContactRequests").getChildrenCount();
                            updateNumContactReq(noContactReq);
                        }else{
                            //nothing new
                            updateNumContactReq(0);
                        }
                    }


            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    //this is called by the below function (checkUserHasMessagesDB) if the user has a new message
    public void updateSharedPrefsIfMessages(int noMessages,String currentContactUID){
        SharedPreferences contactMessagesSP = getSharedPreferences(SHARED_PREFS_MESSAGES,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactMessagesSP.edit();
        editor.putInt(currentContactUID, noMessages);
        editor.apply();
        //this updates the shared preferences with the info collected in checkUserHasMessagesDB
    }

    //checks firebase and check if the user has new messages and get the amount they have
    public void checkUserHasMessagesDB(final String cUID){
        dbCheckForMessages = FirebaseDatabase.getInstance().getReference("Users");
        dbCheckForMessages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int noMessages = (int)dataSnapshot.child(mAuth.getUid()).child("Received").child(cUID).getChildrenCount();
                updateSharedPrefsIfMessages(noMessages,cUID);
                //get the number of new messages and the UID of the user that the message is from
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }

}
