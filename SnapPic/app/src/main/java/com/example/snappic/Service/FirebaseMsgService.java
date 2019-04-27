package com.example.snappic.Service;
//MESSAGE SERVICE
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.snappic.MainActivity;
import com.example.snappic.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

@SuppressWarnings("ALL")
public class FirebaseMsgService extends FirebaseMessagingService {
    FirebaseAuth mAuth;
    DatabaseReference dbRefADelete;
    DatabaseReference addNewToken;
    public FirebaseMsgService(){

    }

    //when a notification is received
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        showNotification(remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());//get everythign from the json string and compile it in the large function below
        String click_action = remoteMessage.getNotification().getClickAction(); //opens activity when clicked

    }

    //configure attributes of the notification
    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "com.example.snappic.test";
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //intent flag - closes other activities running above this one in the stack
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,intent,0);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){//need oreo
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"Notification",NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableLights(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setContentInfo("Info")
                .setContentIntent(pendingIntent);
        notificationManager.notify(new Random().nextInt(),notificationBuilder.build());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        Log.d("TOKENFIREBASE",s);
        uploadTokenToDb(s);
    }

    public void uploadTokenToDb(final String token){

        mAuth = FirebaseAuth.getInstance();
        addNewToken = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid());
        addNewToken.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //delete contact and children with the uid specified
                addNewToken.child("token").setValue(token);//if a new token is not in the db write it.
                return;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });
    }
}
