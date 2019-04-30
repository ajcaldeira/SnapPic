package com.example.snappic;
//CLASS TO SAVE THE DATA OF STORIES
public class Story {

    //private String name;
    private String date;
    private String name;
    private String imgUrl;
    private String imgName;


    public Story(String date,String name,String imgUrl,String imgName){
        //this.name = name;
        this.date = date;
        this.name = name;
        this.imgUrl = imgUrl;
        this.imgName = imgName;
    }


    public String getDate() {
        return date;
    }
    public String getImgName() {
        return imgName;
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
    public void setImgName(String imgName) {
        this.imgName = imgName;
    }
    public void setName(String name) {
        this.name = name;
    }
}
