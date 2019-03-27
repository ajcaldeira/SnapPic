package com.example.snappic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class StoryListAdapter extends ArrayAdapter<Story> {

    private Context mContext;
    int mResource;
    //default constructor
    public StoryListAdapter(Context context, int resource, ArrayList<Story> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //gets view and attaches to list view
        String date = getItem(position).getDate();
        String name = getItem(position).getName();
        String imgUrl = getItem(position).getUrl();

        //create contact object with the info
        Story story = new Story(date,name,imgUrl);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        //TextView txtStoryName = convertView.findViewById(R.id.txtStoryName);
        TextView txtStoryDate = convertView.findViewById(R.id.txtStoryDate);
        TextView txtStoryName = convertView.findViewById(R.id.txtStoryName);
        ImageView storyImg  = convertView.findViewById(R.id.storyImg);
        txtStoryDate.setText(date);
        txtStoryName.setText(name);
        new DownloadImageTask(storyImg).execute(imgUrl);
        return convertView;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {

                e.printStackTrace();
            }
            return bmp;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);

        }
    }




}



























