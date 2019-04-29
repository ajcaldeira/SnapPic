package com.example.snappic;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactUIDListAdapter extends ArrayAdapter<ContactUIDListFill> {

    private Context mContext;
    int mResource;
    //default constructor
    public ContactUIDListAdapter(Context context, int resource, ArrayList<ContactUIDListFill> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //gets view and attaches to list view
        String uid = getItem(position).uid;

        //create contact object with the info
        UIDList uidList = new UIDList(uid);

        //sets the xml layout to the view
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        //name of person
        TextView txtStoryName = convertView.findViewById(R.id.txtStoryName);
        txtStoryName.setText(uid);

        return convertView;
    }

}
