package com.example.snappic;

public class New_contact_item {
    private String txtNewContName;
    private String txtNewContNumber;
    private String newContUID;

    public New_contact_item(String contName, String contNumber, String uid){
        txtNewContName = contName;
        txtNewContNumber = contNumber;
        newContUID = uid;
    }

    public String getNewContactName(){
        return txtNewContName;
    }

    public String getNewContactNumber(){
        return txtNewContNumber;
    }

    public String getUID(){
        return newContUID;
    }
}
