package com.example.snappic;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendNotificationJava extends AsyncTask<String,String,String> {

    private String TOKEN_TO_SEND;


    public SendNotificationJava(String token){
        super();
        TOKEN_TO_SEND = token;

    }

    @Override
    protected String doInBackground(String... token) {
        try {

            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "key=AIzaSyANboaMSEMytilBT_0v-tROB3fqkW7cguU");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            json.put("to", TOKEN_TO_SEND);


            JSONObject info = new JSONObject();
            info.put("title", "Angelo");   // Notification title
            info.put("body", "Hello Test notification"); // Notification body

            json.put("notification", info);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(json.toString());
            wr.flush();
            conn.getInputStream();
            Log.d("sendnotificationerror","I didnt fail..." + TOKEN_TO_SEND);
        }
        catch (Exception e)
        {
            Log.d("sendnotificationerror",""+e);
        }
        return null;
    }
}