package com.example.snappic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;


public class RightScreen extends AppCompatActivity {


    private float x1,x2,y1,y2;
    static final int MIN_DISTANCE = 150;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat dateFormatTime = new SimpleDateFormat("yyyy-MM-dd @ HH:mm");
    ListView listStory;
    DatabaseReference dbRef,dbStory,dbStoryImage,dbStoryImgDirec;
    FirebaseAuth mAuth;
    ArrayList<Story> arrayList = new ArrayList<>();
    Fragment fragment;
    FrameLayout frameLayoutFragment;
    ImageView fragImgView;
    ImageView imgExitStory;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_right_screen);
        listStory = findViewById(R.id.listStory);
        mAuth = FirebaseAuth.getInstance();
        fragment = new FragmentStory();
        fragImgView = findViewById(R.id.fragImgView);
        imgExitStory = findViewById(R.id.imgExitStory);
        frameLayoutFragment = findViewById(R.id.fragCtr);

        frameLayoutFragment.setVisibility(View.INVISIBLE);


        getUIDContactList();



        imgExitStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameLayoutFragment.setVisibility(View.INVISIBLE);

            }
        });

        listStory.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        float deltaX = x1 - x2;
                        if (Math.abs(deltaX) > MIN_DISTANCE)
                        {
                            if(deltaX > 0){
                                // LEFT TO RIGHT

                            }else{
                                //swiped RIGHT to LEFT
                                //Toast.makeText(RightScreen.this, "RTL", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RightScreen.this, MainActivity.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);

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
        listStory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(RightScreen.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
                //SHOW FRAGMENT HERE
                frameLayoutFragment.setVisibility(View.VISIBLE);
                Story fragImg = arrayList.get(position);
                Picasso.get().load(fragImg.getUrl()).into(fragImgView);
                //fragment.getView().setBackground(fragImg.getUrl();

            }
        });

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        Intent mainActivity = new Intent(RightScreen.this,MainActivity.class);
        startActivity(mainActivity);
    }

    public void getUIDContactList(){
        ContactScreen contactScreen = new ContactScreen();

        String SHARED_PREFS = contactScreen.getSharedPrefContactVar();
        SharedPreferences contactSharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int noContacts = contactSharedPref.getInt("noContacts",0);
        //check contacts are up to date and get number of them

        int no_contacts = noContacts;
        int loop_Incrementer = 0;

        while(loop_Incrementer != no_contacts){
            String spName = "cUID" + String.valueOf(loop_Incrementer);
            String currentSP = contactSharedPref.getString(spName,"");
            String spUsersName = contactSharedPref.getString(currentSP,"");
            GetStory(currentSP,spUsersName);
            loop_Incrementer++;
        }

    }


    public void GetStory(String spID, final String spUsersName){
        Date date = new Date();
        String myUID = spID;
        dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUID).child("Story").child(dateFormat.format(date));
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //iterate through db and check if the number the user just used to sign up exists already

                for(DataSnapshot storySnapshot: dataSnapshot.getChildren()){
                    RetrieveStory retrieveStory = storySnapshot.getValue(RetrieveStory.class);
                    String storyTimeStamp = retrieveStory.timestamp;
                    String storyName = spUsersName;
                    String imgUrl = retrieveStory.mImageUrl;

                    long epoch = Long.parseLong(storyTimeStamp);
                    Date dateEp = new Date(epoch);

                    //create story object and pass the data into it
                    Story story = new Story(dateFormatTime.format(dateEp),storyName,imgUrl);
                    arrayList.add(story);
                }
                StoryListAdapter adapter = new StoryListAdapter(RightScreen.this, R.layout.custom_story_list, arrayList);
                listStory.setAdapter(adapter);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}
