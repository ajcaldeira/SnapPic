package com.example.snappic;

public class Contacts {

    private String name;
    private String number;
    private int noMessages;


    public Contacts(String name, String number, int noMessages){
        this.name = name;
        this.number = number;
        this.noMessages = noMessages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getNoMessages() {
        return noMessages;
    }

    public void getNoMessages(int noMessages) {
        this.noMessages = noMessages;
    }
}
