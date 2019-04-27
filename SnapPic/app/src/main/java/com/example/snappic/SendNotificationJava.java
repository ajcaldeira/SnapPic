package com.example.snappic;
//THIS IS USED TO SEND THE NOTIFICATION - https://firebase.google.com/docs/cloud-messaging/http-server-ref
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendNotificationJava extends AsyncTask<String,String,String> {

    private String TOKEN_TO_SEND;


    //this is called when we want to send a notification, passing the token from the DB
    public SendNotificationJava(String token){
        super();
        TOKEN_TO_SEND = token;

    }

    @Override
    protected String doInBackground(String... token) {
        try {
            //https://www.youtube.com/watch?v=G6XK37-x__A&fbclid=IwAR3IstK8U-IMKjpCbosMLfi_8BKnHLNbw6uS3tf1P4dQ-cZmz449n4Zj_WM
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "key=AIzaSyANboaMSEMytilBT_0v-tROB3fqkW7cguU");//key from firebase
            conn.setRequestProperty("Content-Type", "application/json");

            //parse the json
            JSONObject json = new JSONObject();
            json.put("to", TOKEN_TO_SEND);//token for who to send to


            JSONObject info = new JSONObject();
            info.put("title", "SnapPic");   // Notification title
            info.put("body", "New Snap Received!"); // Notification body
            info.put("click_action", "MAINACTIVITYY"); // activity to open when the notification is clicked
            json.put("notification", info);

            //compile the json string
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(json.toString());
            wr.flush();
            conn.getInputStream();

        }
        catch (Exception e)
        {
            Log.d("sendnotificationerror",""+e);
        }
        return null;
    }
}