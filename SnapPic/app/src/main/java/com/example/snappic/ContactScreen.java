package com.example.snappic;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
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
    TextView txtContName;
    TextView txtContNumber;
    TextView txtNewContacts;
    ProgressBar progressBarContact;
    DatabaseReference dbRef;
    DatabaseReference dbContacts;
    DatabaseReference dbContactsSingle;
    ArrayList<Contacts> arrayList = new ArrayList<>();
    EditText txtSearchNumber;
    Button btnAddContact;
    ImageButton btnContactAlert;
    //POP UP
    Dialog myDialog;
    Dialog myContactDialog;
    Dialog newContactDialog;
    ImageButton btnPopClose;
    ImageButton btnNewContact;
    TextView txtPopName;
    TextView txtPopPhoneNo;
    private FirebaseAuth mAuth;
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
        GetUserContacts(uid,false);

        //POP UP
        myDialog = new Dialog(this);
        myContactDialog = new Dialog(this);
        newContactDialog = new Dialog(this);
        btnPopClose = findViewById(R.id.btnPopClose);


        //new contact recycler view and items:
        btnContactAlert = findViewById(R.id.btnContactAlert);

        //New_contact_items.add(new New_contact_item("Angelo","+447708521254","D7h7d7dgi4rw86dw3r"));





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
        btnNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myContactDialog.setContentView(R.layout.add_contact_dialog);
                txtSearchNumber = myContactDialog.findViewById(R.id.txtSearchNumber);
                btnAddContact = myContactDialog.findViewById(R.id.btnAddContact);
                myContactDialog.show();
            }
        });

        btnContactAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newContactDialog.setContentView(R.layout.new_contact_recycler);
                newContRecycler = newContactDialog.findViewById(R.id.contactRecyclerView);
                newContRecycler.setHasFixedSize(true);//recycler view will not change size no matter how many items are in it - better performance: https://www.youtube.com/watch?v=17NbUcEts9c 11:50
                setUpRecyclerView();
                newContactDialog.show();

                //first pass 0 as it is the val for drag and drop
                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {


                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                        return false;//drag and drop
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        switch(direction){
                            case 4:
                                //delete item
                                int position = viewHolder.getAdapterPosition();
                                //deleteRecyclerViewItem(0);
                                //New_contact_item currentUser = new New_contact_item();
                                New_contact_item currentUser = New_contact_items.get(position);
                                String uid = currentUser.getUID();


                                setUpRecyclerView();//reset the view so there are no gaps

                                Toast.makeText(ContactScreen.this, "Deleted Request " + uid  + " "+ String.valueOf(position), Toast.LENGTH_LONG).show();
                                break;
                            case 8:
                                setUpRecyclerView();//reset the view so there are no gaps
                                Toast.makeText(ContactScreen.this, "Accepted Request", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        //left is 4


                        //right is 8
                    }
                }).attachToRecyclerView(newContRecycler);

            }
        });
    }
    public void getContactRequest(){
        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("ReceivedContactRequests");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){

                    //get the contacts name and number
                    String currentName = ds.child("Name").getValue().toString();
                    String currentNumber = ds.child("Number").getValue().toString();
                    String uid = ds.getKey();
                    New_contact_items.add(new New_contact_item(currentName,currentNumber,uid));
                    Log.d("MYGETTHEM",currentName + " " + currentNumber + " " + uid);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
    public void deleteContactRequest(){

    }
    public void setUpRecyclerView(){
        newContRecyclerLayoutManager = new LinearLayoutManager(ContactScreen.this);
        newContRecyclerAdapter = new New_contact_recycler_adapter(New_contact_items);
        newContRecycler.setLayoutManager(newContRecyclerLayoutManager);
        newContRecycler.setAdapter(newContRecyclerAdapter);
        btnContactAlert = findViewById(R.id.btnContactAlert);
    }


    public void deleteRecyclerViewItem(int position){

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences contactSharedPrefReq = getSharedPreferences("ContactREQ", MODE_PRIVATE);
        int currentREQ = contactSharedPrefReq.getInt("numOfContactsRequests",0);
        txtNewContacts.setText(String.valueOf(currentREQ));
        New_contact_items.clear();//clear the array so it doesnt duplicate whats in it when the phone is locked and unlocked on the screen
        getContactRequest();
    }

    public void AddNewContact(View v){
        //GET THE USER'S CONTACTS
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
                        break;
                    }

                }


            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
    public void sendContactRequest(){
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already


            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
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
