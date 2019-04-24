package com.example.snappic;

public class RetrieveStory {

    public String timestamp,mImageUrl,mName,imgName;

    public RetrieveStory(){

    }

    public RetrieveStory(String timeStamp, String mImageUrl,String mName,String imgName) {
        this.timestamp = timeStamp;
        this.mImageUrl = mImageUrl;
        this.mName = mName;
        this.imgName = imgName;
    }
}
