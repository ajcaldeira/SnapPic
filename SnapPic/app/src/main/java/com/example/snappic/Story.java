package com.example.snappic;

public class Story {

    //private String name;
    private String date;
    private String name;
    private String imgUrl;


    public Story(String date,String name,String imgUrl){
        //this.name = name;
        this.date = date;
        this.name = name;
        this.imgUrl = imgUrl;
    }


    public String getDate() {
        return date;
    }

    public String getUrl() {
        return imgUrl;
    }

    public String getName() {
        return name;
    }

    public void setDate(String date) {
        this.date = date;
    }
    public void setUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public void setName(String name) {
        this.name = name;
    }
}
