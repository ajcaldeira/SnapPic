package com.example.snappic;

public class RetrieveStory {

    public String timestamp,mImageUrl,mName;

    public RetrieveStory(){

    }

    public RetrieveStory(String timeStamp, String mImageUrl,String mName) {
        this.timestamp = timeStamp;
        this.mImageUrl = mImageUrl;
        this.mName = mName;
    }
}
