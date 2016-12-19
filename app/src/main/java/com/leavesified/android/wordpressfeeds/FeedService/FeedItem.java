package com.leavesified.android.wordpressfeeds.feedservice;

import com.leavesified.android.wordpressfeeds.R;

public class FeedItem {
    private String mTitle;
    private String mLink;
    private String mPubDate;
    private String mDescription;
    private String mCategory;

    private int mLargeImage;

    public FeedItem(){
        mTitle = "";
        mLink = "";
        mPubDate = "";
        mDescription = "";
        // TODO set appropriate default image
        mLargeImage = R.mipmap.ic_launcher;
    }

    public void setAttribute(String tag, String text){
        switch (tag) {
            case "title":
                mTitle = decodeEntityRefs(text);
                break;
            case "link":
                mLink = text;
                break;
            case "pubDate":
                mPubDate = text;
                break;
            case "description":
                mDescription = decodeEntityRefs(text);
                break;
            case "category":
                // TODO change icons depending on your category
                mCategory = decodeEntityRefs(text);
                if(mCategory.equals("Einsätze")) mLargeImage = R.mipmap.einsatz;
                else if(mCategory.equals("News")) mLargeImage = R.mipmap.news;
        }
    }

    private String decodeEntityRefs(String in){
        // TODO add new entity replacements when neccesary
        return in.replaceAll("&#252;", "ü")
                .replaceAll("&#220;", "Ü")
                .replaceAll("&#246;", "ö")
                .replaceAll("&#214;", "Ö")
                .replaceAll("&#228;", "ä")
                .replaceAll("&#196;", "Ä")
                .replaceAll("&#223;", "ß")
                .replaceAll("&#8220;", "\"")
                .replaceAll("&#8222;", "\"")
                .replaceAll("&#8230;", "...");
    }

    @Override
    public String toString(){
        String result = "";
        result += mTitle + "\n" + mLink + "\n" + mPubDate + "\n"
                + mDescription  + "\n" + mCategory + "\n\n";
        return result;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getLink() {
        return mLink;
    }

    public String getPubDate() {
        return mPubDate;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getCategory() {
        return mCategory;
    }

    public int getmLargeImage() {
        return mLargeImage;
    }

}
