package com.example.snappic;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class New_contact_recycler_adapter extends RecyclerView.Adapter<New_contact_recycler_adapter.contViewHolder> {
private ArrayList<New_contact_item> mContactArrayList;
    public static class contViewHolder extends RecyclerView.ViewHolder{
        public TextView txtName;
        public TextView txtNumber;
        public contViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtNewContName);
            txtNumber = itemView.findViewById(R.id.txtNewContNumber);
        }
    }

    public New_contact_recycler_adapter(ArrayList<New_contact_item> contactArrayList){
        mContactArrayList = contactArrayList;
    }

    @NonNull
    @Override
    public contViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contact_recyclerview_item,viewGroup,false);
        contViewHolder contViewHolder = new contViewHolder(v);
        return contViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull contViewHolder contViewHolder, int position) {
        New_contact_item currentItem = mContactArrayList.get(position);

        contViewHolder.txtName.setText(currentItem.getNewContactName());
        contViewHolder.txtNumber.setText(currentItem.getNewContactNumber());
    }

    @Override
    public int getItemCount() {
        return mContactArrayList.size();//return no of items in the array list
    }

}
