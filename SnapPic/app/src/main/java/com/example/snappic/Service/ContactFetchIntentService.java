package com.example.snappic.Service;
//THIS IS A BACKGROUND SERVICE, IT CHECKS THE NUMBER OF CONTACTS, CONTACT REQUESTS, SETS ALL THE SHARED PREFS SO THIS INFORMATION CAN BE CARRIED OUT THROUGH THE APP
//THIS SERVICE CANNOT EFFECT UI AT ALL
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.snappic.User;
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

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        //MAKE SURE WE HAVE THE INSTANCE SO WE KNOW WHICH USER WERE CHECKING
    }

    public ContactFetchIntentService() {
        super("ContactFetchIntentService");
        setIntentRedelivery(true);//ASYNCHRONOUS REQUEST TO RUN THE SERVICE
    }
    //GETTER
    public String getSharedPrefContactVar(){
        return SHARED_PREFS;
    }

    //SAVE SHARED PREFERENCES THAT HOLD UID AND ANOTHER HOLDING USERS NAME
    public void SaveSharedPrefs(String spName, String uid,String spUsersName,String contactSPRef){
        SharedPreferences contactSharedPref = getSharedPreferences(contactSPRef,MODE_PRIVATE);
        SharedPreferences.Editor editor = contactSharedPref.edit();
        editor.putString(spName, uid);//this is a UID for the shared pref, cUID0,cUID1 etc.. used to loop later
        editor.putString(uid, spUsersName); //stores the first name of the user with their uid as key - used to loop through later
        editor.apply();//apply the changes
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        //starts worker thread with request to process
        String uid = mAuth.getUid();
        GetUserContacts(uid,false); //updae the user's contact list - calls SaveSharedPrefs() so info can be used throughout app
        CheckIfUserHasContactRequest(); //check if the user has any contact requests
        checkUserHasMessages(); //check if the user has nay new messages

        //Log.d("ISMYSERVICERUNNING", "RUNNING!");
    }

    //check if the user has nay new messages
    public void checkUserHasMessages(){
        //number of contacts
        SharedPreferences noContactSharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        int noOfContacts = noContactSharedPref.getInt("noContacts",0);

        for(int i = 0; i < noOfContacts; i++){
            //loop through shared pref using the unique id as specified previously
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
                //save the NUMBER of contacts so we know how many to itterate through later
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
        editor.putInt("numOfContactsRequests", noContacts);//number of contact requests
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
