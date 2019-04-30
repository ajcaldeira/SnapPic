package com.example.snappic;
//CLASS FOR THE USERS NAME AN DNUMBER WHEN PULLING FROM FIREBASE
public class User {

    public String name, number;


    public User(){

    }

    public User(String name, String number) {
        this.name = name;
        this.number = number;
    }
}
