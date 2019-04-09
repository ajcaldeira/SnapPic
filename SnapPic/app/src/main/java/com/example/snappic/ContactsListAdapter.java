package com.example.snappic;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ContactsListAdapter extends ArrayAdapter<Contacts> {

    private Context mContext;
    int mResource;
    //default constructor
    public ContactsListAdapter(Context context, int resource, ArrayList<Contacts> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position,View convertView, ViewGroup parent) {
        //gets view and attaches to list view
        String name = getItem(position).getName();
        String number = getItem(position).getNumber();
        int noMessages = getItem(position).getNoMessages();

        //create contact object with the info
        Contacts contact = new Contacts(name, number, noMessages);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView txtContName = convertView.findViewById(R.id.txtContName);
        TextView txtContNumber = convertView.findViewById(R.id.txtContNumber);
        TextView txtNoNotifications = convertView.findViewById(R.id.txtNoNotifications);

        txtContName.setText(name);
        txtContNumber.setText(number);
        txtNoNotifications.setText(String.valueOf(noMessages));

        return convertView;
    }

}


















