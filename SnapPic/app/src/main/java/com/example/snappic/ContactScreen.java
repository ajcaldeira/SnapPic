package com.example.snappic;
//DEALS WITH THE CONTACT SCREEN
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.snappic.Service.ContactFetchIntentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ContactScreen extends AppCompatActivity  {
    private static final String SHARED_PREFS_MESSAGES = "messagesSP";//for shared prefs
    private int ARRAY_SIZE; //to keep track of the array size and so firebase doesnt do something unexpected
    private int ORIENTATION_FLIP = 0; //the orientation of the phone - used to flip images when viewing messages
    private boolean DELETING_FLAG; //this is so firebase doesnt delete when it shouldent
    private boolean DELETING_JUST_ADDED = false; //stop the db from auto deleting something if it was previously deleted
    private boolean DELETING_JUST_ADDED_CONT_LIST = true; //stop the db from auto deleting something if it was previously deleted
    private int NO_CONTACTS; //number of contacts user has
    private int CURRENT_MESSAGE; //the current message being displayed
    private String CURRENT_MESSAGE_USERID; //UID of the user's messages being displayed
    private String UID_TO_DELETE; //UID of the contact to delete
    private int USERS_TO_ADD = 1; //always 1 to add new users
    private float x1,x2; //for sliding between screens
    static final int MIN_DISTANCE = 150; //for sliding right between screens

    //Declaring vars
    ListView listContacts;
    TextView txtContName;
    TextView txtContNumber;
    TextView txtNewContacts;
    ProgressBar progressBarContact;
    EditText txtSearchNumber;
    Button btnAddContact;
    ImageButton btnContactAlert;
    ImageView imgShowMessage;
    //Firebase
    private FirebaseAuth mAuth;
    DatabaseReference dbRef;
    DatabaseReference dbRefDeleteContactReq;
    DatabaseReference dbRefAdd;
    DatabaseReference dbRefADelete;
    DatabaseReference dbRefViewMessage;
    DatabaseReference dbRefDeleteMessage;
    DatabaseReference dbContactsSingleForeground;
    //Arrays
    ArrayList<Contacts> arrayList = new ArrayList<>();
    ArrayList<String> newMessagesArrayList = new ArrayList<>();
    ArrayList<String> newMessagesIDArrayList = new ArrayList<>();
    ArrayList<String> newMessagesFileName = new ArrayList<>();
    //POP UP
    Dialog myDialog;
    Dialog myContactDialog;
    Dialog newContactDialog;
    Dialog viewMessageDialog;
    ImageButton btnPopClose;
    ImageButton btnNewContact;
    TextView txtPopName;
    TextView txtPopPhoneNo;

    //shared prefs
    private int counter = 0;
    public static final String SHARED_PREFS = "ContactSP";
    public static final String SHARED_PREFS_SPECIFIC = "SpecificSP";
    //recycler view
    private RecyclerView newContRecycler;
    private RecyclerView.Adapter newContRecyclerAdapter;//provides only as many items as we currently need
    private RecyclerView.LayoutManager newContRecyclerLayoutManager;//provides only as many items as we currently need
    final ArrayList<New_contact_item> New_contact_items = new ArrayList<>();
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ARRAY_SIZE = 1;
        DELETING_FLAG = false;
        setContentView(R.layout.activity_contact_screen);
        txtContName = findViewById(R.id.txtContName);
        txtContNumber = findViewById(R.id.txtContNumber);
        txtNewContacts = findViewById(R.id.txtNewContacts);
        listContacts = findViewById(R.id.listContacts);
        progressBarContact = findViewById(R.id.progressBarContact);
        btnNewContact = findViewById(R.id.btnNewContact);
        btnAddContact = findViewById(R.id.btnAddContact);
        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getUid();

        //POP UP
        myDialog = new Dialog(this);
        myContactDialog = new Dialog(this);
        newContactDialog = new Dialog(this);
        viewMessageDialog = new Dialog(this);
        btnPopClose = findViewById(R.id.btnPopClose);

        //new contact recycler view and items:
        btnContactAlert = findViewById(R.id.btnContactAlert);

        //sliding back to main screen
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
                //when the user clicks on a contact
                Contacts contact = arrayList.get(position); //get the position in the arraylist
                myDialog.setContentView(R.layout.custom_popup); //set up dialog, link it to the layout
                //all vars for the dialog
                txtPopName = myDialog.findViewById(R.id.txtPopName);
                txtPopName.setText(contact.getName());
                txtPopPhoneNo = myDialog.findViewById(R.id.txtPopPhoneNo);
                txtPopPhoneNo.setText(contact.getNumber());
                UID_TO_DELETE = contact.getNumber(); // give this global var the number of the user selected for if we want to delete them
                myDialog.show(); // show pop up
            }

        });
        btnNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set up and open dialog
                myContactDialog.setContentView(R.layout.add_contact_dialog);
                txtSearchNumber = myContactDialog.findViewById(R.id.txtSearchNumber);
                btnAddContact = myContactDialog.findViewById(R.id.btnAddContact);
                myContactDialog.show();
            }
        });

        btnContactAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open the dialog the holds the recycler view that
                newContactDialog.setContentView(R.layout.new_contact_recycler);
                newContRecycler = newContactDialog.findViewById(R.id.contactRecyclerView);
                newContRecycler.setHasFixedSize(true);//recycler view will not change size no matter how many items are in it - better performance: https://www.youtube.com/watch?v=17NbUcEts9c 11:50
                newContactDialog.show();
                getContactRequest();


                //first pass 0 as it is the val for drag and drop
                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                        return false;//drag and drop
                    }
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        New_contact_item currentUser;
                        int position;
                        String uid;
                        switch(direction){
                            //slide left to delete a contact request
                            case 4:
                                //delete item from the requested contact list
                                position = viewHolder.getAdapterPosition();
                                currentUser = New_contact_items.get(position);
                                uid = currentUser.getUID();
                                ARRAY_SIZE--;
                                txtNewContacts.setText(String.valueOf(ARRAY_SIZE));
                                New_contact_items.clear();
                                setUpRecyclerView();
                                deleteContactRequest(uid);
                               // finish();
                               // startActivity(getIntent());
                                break;
                            case 8:
                                //sl;ide right to accept a contact request
                                USERS_TO_ADD = 1;
                                DELETING_JUST_ADDED_CONT_LIST = true;
                                //accept contact request
                                //GET POS and put uid into a variable
                                position = viewHolder.getAdapterPosition();
                                currentUser = New_contact_items.get(position);
                                uid = currentUser.getUID();
                                String name = currentUser.getNewContactName();
                                String number  = currentUser.getNewContactNumber();
                                //1. remove it from the array
                                ARRAY_SIZE--;
                                txtNewContacts.setText(String.valueOf(ARRAY_SIZE));
                                New_contact_items.clear();
                                setUpRecyclerView();

                                //2.add to contacts
                                addUserToContacts(uid,name,number);

                                loadSharedPrefNoContacts();
                                GetUserContacts(mAuth.getUid(),false);
                                Log.d("ARRAYLISTSIZE", "case 8 : " + String.valueOf(arrayList.size()));
                                //3.remove from db
                                deleteContactRequest(uid);

                                break;
                        }

                        //left is 4
                        //right is 8
                    }
                }).attachToRecyclerView(newContRecycler);//attach it to the recycler view

            }
        });
        //make sure the intent service is running to update everything
        Intent serviceIntent = new Intent(ContactScreen.this, ContactFetchIntentService.class);
        startService(serviceIntent);

    }

    @Override
    protected void onResume() {//when activity is hidden from view, this is called when it comes back
        super.onResume();
        //when the screen is resumed, make sure the service is running by running it again just in case it is not
        Intent serviceIntent = new Intent(ContactScreen.this, ContactFetchIntentService.class);
        startService(serviceIntent);
    }

    //get the current user's contact requests from firebase
    public void getContactRequest(){
        New_contact_items.clear();//make sure the array is empty first so there are no doubles
        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("ReceivedContactRequests");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(true){
                    for(DataSnapshot ds: dataSnapshot.getChildren()){
                        String currentName;
                        String currentNumber;
                        //get the contacts name and number
                        if(ds.child("Number").getValue() == null || ds.child("Name").getValue() == null){

                            break;

                        }else{
                            currentName = ds.child("Name").getValue().toString();
                            currentNumber = ds.child("Number").getValue().toString();
                        }


                        String uid = ds.getKey();
                        if(New_contact_items.size() == ARRAY_SIZE){//make sure the array does not get extra contacts added as firebase is asynchronous
                            break;
                        }else{
                            New_contact_items.add(new New_contact_item(currentName,currentNumber,uid));
                        }

                    }
                    setUpRecyclerView(); //set up the recycler view, basically refresh it

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
    //ONCLICK for delete contact button when user selects a contact in their list
    public void deleteContactInContacts(View v){
        DELETING_JUST_ADDED_CONT_LIST = false;//only false when the button is physically clicked
        myDialog.dismiss();//get rid of the dialog box
        String SHARED_PREFS = getSharedPrefContactVar();//load the shared pref id
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);//mode private so only my app can access

        //shared prefs with uid linked to phone number
        SharedPreferences contactSharedPrefsSpecific = getSharedPreferences(SHARED_PREFS_SPECIFIC, MODE_PRIVATE);
        int noContacts = contactSharedPref.getInt("noContacts",0);
        int loop_Incrementer = 0;
        //loop through the contacts shared prefs to match the phone number with the one stored in shared prefs.
        //then delete that specific one.
        while(loop_Incrementer != noContacts){
            String spName = "cUID" + String.valueOf(loop_Incrementer);
            String uid = contactSharedPref.getString(spName,"");
            String userNumber = contactSharedPrefsSpecific.getString(uid,"");
            if(userNumber.equals(UID_TO_DELETE)){
                    //if the number the user tapped on is equal to the current shared pref then end the loop
                    dbRefADelete = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("Contacts").child(uid);
                    dbRefADelete.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //delete contact and children with the uid specified
                            if(!DELETING_JUST_ADDED_CONT_LIST){//so it doesn't delete automatically without being clicked
                                dataSnapshot.getRef().removeValue();

                            }
                            return;
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}

                    });
                dbRefADelete = null;
                break;
            }else {
                loop_Incrementer++;
            }
        }
    }
    //remove the user form the received contacts
    public void deleteContactRequest(String currentUID){
        dbRefDeleteContactReq = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("ReceivedContactRequests").child(currentUID);
        dbRefDeleteContactReq.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //delete contact and children with the uid specified
                if(!DELETING_JUST_ADDED) {//avoid deleting something if it was deleted and added again - firebase problem
                    dataSnapshot.getRef().removeValue();
                }
                return;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });
        DELETING_JUST_ADDED = false;
        dbRefDeleteContactReq = null;
    }
    //once all the users messages have been viewed, clear them from that user
    public void deleteMessages(final String currentMessageID){
        dbRefDeleteMessage = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("Received");
        dbRefDeleteMessage.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //delete contact and children with the uid specified
                dataSnapshot.child(CURRENT_MESSAGE_USERID).child(currentMessageID).getRef().removeValue();
                return;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });

    }
    public void addUserToContacts(final String currentUID, final String currentName, final String currentNumber){
        DELETING_JUST_ADDED = true;
        dbRefAdd = FirebaseDatabase.getInstance().getReference("Users");
        dbRefAdd.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(USERS_TO_ADD <= 1){
                    USERS_TO_ADD++;
                    //get my info
                    String myName = dataSnapshot.child(mAuth.getUid()).child("name").getValue().toString();
                    String myNumber = dataSnapshot.child(mAuth.getUid()).child("number").getValue().toString();

                    //add THEM to MY list
                    dbRefAdd.child(mAuth.getUid()).child("Contacts").child(currentUID).child("name").setValue(currentName);
                    dbRefAdd.child(mAuth.getUid()).child("Contacts").child(currentUID).child("number").setValue(currentNumber);

                    //add ME to THEIR list
                    dbRefAdd.child(currentUID).child("Contacts").child(mAuth.getUid()).child("name").setValue(myName);
                    dbRefAdd.child(currentUID).child("Contacts").child(mAuth.getUid()).child("number").setValue(myNumber);
                    return;
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });
    }

    //set up the recycler view
    public void setUpRecyclerView(){
        if(newContRecycler != null){
            newContRecyclerLayoutManager = new LinearLayoutManager(ContactScreen.this);
            newContRecyclerAdapter = new New_contact_recycler_adapter(New_contact_items);
            newContRecycler.setLayoutManager(newContRecyclerLayoutManager);
            newContRecycler.setAdapter(newContRecyclerAdapter); //link it to the adapter
            btnContactAlert = findViewById(R.id.btnContactAlert);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(ContactScreen.this, ContactFetchIntentService.class);
        GetUserContacts(mAuth.getUid(),false);
        startService(serviceIntent);
        changeNumberOfContactRequestsUI();
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        NO_CONTACTS = contactSharedPref.getInt("noContacts",0);
        if(!isNetworkAvailable()){
            progressBarContact.setVisibility(View.INVISIBLE);
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
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public void changeNumberOfContactRequestsUI(){
        SharedPreferences contactSharedPrefReq = getSharedPreferences("ContactREQ", MODE_PRIVATE);
        int currentREQ = contactSharedPrefReq.getInt("numOfContactsRequests",0);
        ARRAY_SIZE = currentREQ;
        txtNewContacts.setText(String.valueOf(ARRAY_SIZE));
        //Log.d("SPNOREQUESTS", "changeNumberOfContactRequestsUI: DETECT " + currentREQ);
    }

    public void AddNewContact(View v){
        //GET THE USER'S CONTACTS
        DELETING_JUST_ADDED = true;
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already
                boolean isInDb = false;
                String searchedNumber =  txtSearchNumber.getText().toString();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    String currentNo = ds.child("number").getValue(String.class);
                    String uid = ds.getKey();
                    if(searchedNumber.equals(currentNo)){
                        //check if the user already sent a contact request
                        if(ds.child("ReceivedContactRequests").child(mAuth.getUid()).hasChildren()){
                            Log.d("ALREADYIN", "Already in db");
                            Toast.makeText(ContactScreen.this, "Already Sent!", Toast.LENGTH_SHORT).show();
                        }else{
                            String sentName = dataSnapshot.child(mAuth.getUid()).child("name").getValue().toString();
                            String sentNumber = dataSnapshot.child(mAuth.getUid()).child("number").getValue().toString();

                            dbRef.child(uid).child("ReceivedContactRequests").child(mAuth.getUid()).child("Name").setValue(sentName);
                            dbRef.child(uid).child("ReceivedContactRequests").child(mAuth.getUid()).child("Number").setValue(sentNumber);

                            //send notification to ds.child(uid).child(number)
                            Toast.makeText(ContactScreen.this, "Contact Request Sent", Toast.LENGTH_SHORT).show();
                        }
                        searchedNumber = "";
                        txtSearchNumber.setText("");
                        dbRef = null;
                        break;
                    }

                }

                Intent serviceIntent = new Intent(ContactScreen.this, ContactFetchIntentService.class);
                GetUserContacts(mAuth.getUid(),false);
                startService(serviceIntent);
                changeNumberOfContactRequestsUI();
                getContactRequest();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
        Intent serviceIntent = new Intent(ContactScreen.this, ContactFetchIntentService.class);
        startService(serviceIntent);

    }

    public void viewMessages(View v){
        DELETING_FLAG = false;//set it to false on click so it shows the messages
        //the variable above has to be here because of firebase's asynchronous annoyance
        String SHARED_PREFS = getSharedPrefContactVar();
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        myDialog.hide();
        CURRENT_MESSAGE = 0;
        CURRENT_MESSAGE_USERID = "";
        Log.d("VIEWMYMESSAGES", "viewMessages: dwdwdwwd");
        newMessagesArrayList.clear();
        newMessagesIDArrayList.clear();
        newMessagesFileName.clear();
        //shared prefs with uid linked to phone number
        SharedPreferences contactSharedPrefsSpecific = getSharedPreferences(SHARED_PREFS_SPECIFIC, MODE_PRIVATE);
        int noContacts = contactSharedPref.getInt("noContacts",0);
        int loop_Incrementer = 0;
        while(loop_Incrementer != noContacts){
            String spName = "cUID" + String.valueOf(loop_Incrementer);
            String uid = contactSharedPref.getString(spName,"");
            String userNumber = contactSharedPrefsSpecific.getString(uid,"");
            if(userNumber.equals(UID_TO_DELETE)){//use this variable to get the  number and match it to the UID
                //if the number the user tapped on is equal to the current shared pref then end the loop
                CURRENT_MESSAGE_USERID = uid;
                dbRefViewMessage = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("Received").child(uid);
                dbRefViewMessage.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //get the image message from db
                        int counter = 0;
                        for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                            //GET EACH MESSAGE FOR THE SPECIFIC USER
                            //Log.d("VIEWMYMESSAGES", userSnapshot.child("mImageUrl").getValue().toString());
                            String imageURL;
                            imageURL = userSnapshot.child("mImageUrl").getValue().toString().trim();
                            String imgName;
                            imgName = userSnapshot.child("mName").getValue().toString().trim();
                            //Log.d("VIEWMYMESSAGES", "onDataChange: " + imageURL);
                            newMessagesArrayList.add(imageURL.trim());
                            newMessagesIDArrayList.add(userSnapshot.getKey());
                            newMessagesFileName.add(imgName.trim());
                            Log.d("VIEWMYMESSAGES", "im running for no real reason -_-");
                            //newMessagesIDArrayList.add();
                            counter++;
                        }

                        //make sure the array isnt empty and a flag so firebase doesnt delete it
                        if(newMessagesArrayList.size() != 0 && !DELETING_FLAG){
                            Log.d("VIEWMYMESSAGES", "I AM GONNA SHOW THE IMAGES WOO");
                            viewMessageDialog.setContentView(R.layout.show_message);
                            imgShowMessage = viewMessageDialog.findViewById(R.id.imgShowMessage);
                            //get the first letter of the file name to decide if we rotate it.
                            String x = newMessagesFileName.get(0).substring(0,1);
                            if(x.equals("2")){//270 degrees
                                ORIENTATION_FLIP = 270;
                            }else if(x.equals("9")){
                                ORIENTATION_FLIP = 90;
                            }else{
                                ORIENTATION_FLIP = 0;
                            }
                            //rotate it to MINUS ORIENTATION_FLIP
                            Picasso.get().load(newMessagesArrayList.get(0)).rotate(-ORIENTATION_FLIP).into(imgShowMessage);
                            CURRENT_MESSAGE = 0;
                            viewMessageDialog.show();
                        }else{
                            Log.d("VIEWMYMESSAGES", "I GONNA FINISH NOW :D");
                            newMessagesArrayList.clear();
                            newMessagesIDArrayList.clear();
                            newMessagesFileName.clear();
                            viewMessageDialog.hide();
                            Toast.makeText(ContactScreen.this, "No Messages To Show", Toast.LENGTH_SHORT).show();
                        }
                        GetUserContacts(mAuth.getUid(),false);
                        return;
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}

                });

                break;
            }else {
                loop_Incrementer++;
            }
        }

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
                Intent sendSnapMain = new Intent(ContactScreen.this, MainActivity.class);
                sendSnapMain.putExtra("isToSend", true);
                sendSnapMain.putExtra("toSendUID", uid);
                startActivity(sendSnapMain);
                break;
            }else {
                loop_Incrementer++;
            }
        }
    }

    public String getSharedPrefContactVar(){
        return SHARED_PREFS;
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        Intent mainActivity = new Intent(ContactScreen.this,MainActivity.class);
        startActivity(mainActivity);
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
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
    public void loadSharedPrefNoContacts(){
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        NO_CONTACTS = contactSharedPref.getInt("noContacts",0);
        DELETING_JUST_ADDED = true;
    }
//TRYING TO GET THE USERS CONTATCS, FIRST GET THE USER WE WANNA GET THE CONTACTS OF THEN TRY ITTERATE THROUGH THE CONTACTS
    public void GetUserContacts(String uid,final boolean isClassCall){
        //GET THE USER'S CONTACTS
        DELETING_JUST_ADDED = true;

        arrayList.clear();
        //dbCheckForMessages = FirebaseDatabase.getInstance().getReference("Users");
        //dbContactsSingleForeground = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Contacts");
        dbContactsSingleForeground = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Contacts");
        dbContactsSingleForeground.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already
                arrayList.clear();
                counter = 0;
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){
                    if(counter > NO_CONTACTS){
                        break;
                    }else {
                        User users = userSnapshot.getValue(User.class);
                        String dbNumber = users.number;
                        String dbName = users.name;
                        //gets UID from parent node in DB
                        String cUID = userSnapshot.getKey();

                        SharedPreferences contactMessagesSP = getSharedPreferences(SHARED_PREFS_MESSAGES,MODE_PRIVATE);
                        int noMessages = contactMessagesSP.getInt(cUID,0);
                        Log.d("CUIDMESSAGES", String.valueOf(noMessages));

                        //check if this current contact has send any messages to the current logged in user (me)
                                                //create contact object and pass the data into it
                        Contacts contact = new Contacts(dbName, dbNumber, noMessages);

                        String spName = "cUID" + String.valueOf(counter);
                        String contactSPRef = getSharedPrefContactVar();
                        arrayList.add(contact);
                        SaveSharedPrefs(spName, cUID, dbName, contactSPRef, dbNumber);
                        counter++;
                    }
                }
                if(!isClassCall){
                    progressBarContact.setVisibility(View.INVISIBLE);
                    ContactsListAdapter adapter = new ContactsListAdapter(ContactScreen.this, R.layout.custom_layout, arrayList);
                    listContacts.setAdapter(adapter);
                    Log.d("ARRAYLISTSIZE", String.valueOf(arrayList.size()));
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }
    //delete the messages when the =last message is clicked on
    public void testOnClick(View v){

        int sizeOfArray = newMessagesArrayList.size();
        CURRENT_MESSAGE++;
        Log.d("VIEWMYMESSAGES", "currentmessage: " + CURRENT_MESSAGE);
        Log.d("VIEWMYMESSAGES", "arraysize: " + sizeOfArray);
        if(CURRENT_MESSAGE >= sizeOfArray){
            DELETING_FLAG = true;
            Log.d("VIEWMYMESSAGES", "testOnClick: Ending");
            CURRENT_MESSAGE = 0;
            viewMessageDialog.hide();
            for(int i = 0; i < sizeOfArray; i ++){
                deleteMessages(newMessagesIDArrayList.get(i));
            }
        }else{
            viewMessageDialog.setContentView(R.layout.show_message);
            imgShowMessage = viewMessageDialog.findViewById(R.id.imgShowMessage);
            Picasso.get().load(newMessagesArrayList.get(CURRENT_MESSAGE)).into(imgShowMessage);

        }

    }

}
