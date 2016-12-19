package com.leavesified.android.wordpressfeeds.feedservice;

public class FeedDownloadFailedException extends Exception{
    public FeedDownloadFailedException() {
        super();
    }

    public FeedDownloadFailedException(String message) {
        super(message);
    }

    public FeedDownloadFailedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public FeedDownloadFailedException(Throwable throwable) {
        super(throwable);
    }
}