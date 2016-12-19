package com.leavesified.android.wordpressfeeds.feedservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Xml;

import com.leavesified.android.wordpressfeeds.MainActivity;
import com.leavesified.android.wordpressfeeds.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class FeedService extends Service {
    private Handler mRunnableHandler = new Handler();
    private NotificationManager mNotificationManager = null;
    private long lastRequestDate = 0;
    private static final String SERVICE_TAG = "FeedService";
    private static final String PULL_TASK_TAG = "PullTask";
    private static final String RECEIVE_FEED_TASK_TAG = "ReceiveFeed";
    public static final String ULR_FROM_NOTIFICATION_EXTRA = "ULR_FROM_NOTIFICATION";
    SharedPreferences mDefaultSharedPreferences;

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPullTask.run();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mRunnableHandler.removeCallbacks(mPullTask);
        Log.d(SERVICE_TAG, "Service Sopped");
    }

    private final Runnable mPullTask = new Runnable() {
        @Override
        public void run() {
            Log.d(PULL_TASK_TAG, "LastRequestDate: " + lastRequestDate);

            try {
                String response = new ReceiveFeed()
                        .execute(new URL(getString(R.string.feed_url)))
                        .get();
                if (response.length() == 3) {
                    Log.d("HttpResponseCode: ", response);
                    if(response.equals("418"))
                        throw new FeedDownloadFailedException("Feed Download Failed");
                } else {
                    FeedItem feedItem = parseFeed(response);
                    Log.d(PULL_TASK_TAG, feedItem.toString());
                    if(mDefaultSharedPreferences.getBoolean("firstRun", true)){
                        mDefaultSharedPreferences.edit().putBoolean("firstRun", false).apply();
                    }else if(!mDefaultSharedPreferences.getBoolean(feedItem.getLink(), false)){
                        mDefaultSharedPreferences.edit().putBoolean(feedItem.getLink(), true)
                                .apply();
                        showNotification(feedItem);
                    }
                }
            } catch (MalformedURLException e) {
                Log.e(PULL_TASK_TAG, "mPullTask Malformed URL, check your URLs in R.strings\n" +
                        "Example: http://www.example.com/feed");
            } catch (InterruptedException e) {
                Log.e(PULL_TASK_TAG, "ReceiveFeed AsyncTask interrupted, nothing will be done");
            } catch (ExecutionException e) {
                Log.e(PULL_TASK_TAG,
                        "ReceiveFeed AsyncTask failed to execute, nothing will be done");
            } catch (XmlPullParserException | IOException e){
                e.printStackTrace();
            } catch (NullPointerException e){
                // failed to clear notification
                e.printStackTrace();
            } catch(FeedDownloadFailedException e){
                // Exception occured during ReceiveFeed (e.g. no internet) so do nothing but printStack
                e.printStackTrace();
            }
            // TODO change check frequency for new posts (3600000 is 1h)
            mRunnableHandler.postDelayed(mPullTask, 3600000);
        }
    };
    /*
    * Exceptions:
    *   XmlPullParserException, IOException when encountering invalid XML
     */
    private FeedItem parseFeed(String feed) throws XmlPullParserException, IOException{
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(feed));

        FeedItem feedItem = new FeedItem();
        while(parser.next() != XmlPullParser.END_DOCUMENT){
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("item")){
                feedItem = parseItem(parser);
                return feedItem;
            }
        }
        return feedItem;
    }

    /*
    * Exceptions:
    *   XmlPullParserException, IOException when encountering invalid XML
     */
    private FeedItem parseItem(XmlPullParser parser) throws XmlPullParserException, IOException{
        FeedItem feedItem = new FeedItem();
        boolean isLastTagParsed = false;
        while(!isLastTagParsed){
            parser.nextTag();
            String tag = parser.getName();
            if(tag.equals("description")) isLastTagParsed = true;
            parser.next();
            String text = parser.getText();
            feedItem.setAttribute(tag, text);
            parser.nextTag();
        }

        return feedItem;
    }

    private class ReceiveFeed extends AsyncTask<URL, Void, String> {
        @Override
        protected String doInBackground(URL... params) {
            HttpURLConnection httpURLConnection = null;
            InputStream in;
            String response = "";
            try {
                httpURLConnection = (HttpURLConnection) params[0].openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setIfModifiedSince(lastRequestDate);
                httpURLConnection.setUseCaches(false);

                if(httpURLConnection.getResponseCode() == 200){
                    in = new BufferedInputStream(httpURLConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder feed = new StringBuilder();
                    try{
                        while ((response = reader.readLine()) != null){
                            feed.append(response).append('\n');
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    response = feed.toString();
                }else{
                   response = String.valueOf(httpURLConnection.getResponseCode());
                }
                lastRequestDate = httpURLConnection
                        .getHeaderFieldDate("Last-Modified", lastRequestDate);

                Log.d(RECEIVE_FEED_TASK_TAG, "LastRequestDate: " + lastRequestDate);
                Log.d(RECEIVE_FEED_TASK_TAG, "ResponseCode: " + httpURLConnection.getResponseCode());

            }catch(Exception e){
                e.printStackTrace();
                response = "418";
            }finally{
                try{
                    httpURLConnection.disconnect();
                }catch(NullPointerException e){
                    e.printStackTrace();
                    response = "418";
                }
            }
            return response;
        }
    }

    private void showNotification(FeedItem feedItem) {
        Notification notification = null;
        String message = feedItem.getTitle();
        String title = getText(R.string.notify_content_title) + " " + feedItem.getCategory();
        int largeIcon = feedItem.getmLargeImage();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(ULR_FROM_NOTIFICATION_EXTRA, feedItem.getLink());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if(Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 16){
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_notify)
                    .setTicker(message)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(contentIntent)
                    .getNotification();
        }else if(Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 23){
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_notify)
                    .setTicker(message)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(contentIntent)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIcon))
                    .build();
        }else if(Build.VERSION.SDK_INT >= 23){
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_notify)
                    .setTicker(message)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(contentIntent)
                    .setColor(getColor(R.color.colorAccent))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIcon))
                    .build();
        }
        try{
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
        }catch(NullPointerException e){
            // Failed to clear Notification onClick. User needs to swipe-clear manually
            // Log error stack trace
            e.printStackTrace();
        }
        mNotificationManager.notify(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
