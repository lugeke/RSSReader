package com.example.lugeke.rssreader.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.util.Log;


import com.example.lugeke.rssreader.provider.FeedContract;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class fetchImageService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FETCH_IMAGE = "com.example.lugeke.rssreader.service.action.fETCH_IMAGE";
    private static final String TAG="FETCHIMAGESERVICE";
    private static final String EXTRA_URL = "com.example.lugeke.rssreader.service.extra.URL";
    private static final String EXTRA_ID="com.example.lugeke.rssreader.service.extra.ID";
    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchImage(Context context, String urls[],long ids[]) {
        Intent intent = new Intent(context, fetchImageService.class);
        intent.setAction(ACTION_FETCH_IMAGE);
        intent.putExtra(EXTRA_URL, urls);
        intent.putExtra(EXTRA_ID,ids);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */


    public fetchImageService() {
        super("fetchImageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_IMAGE.equals(action)) {
                final String []param1 = intent.getStringArrayExtra(EXTRA_URL);
                final long []param2=intent.getLongArrayExtra(EXTRA_ID);
                handleActionFetchImage(param1,param2);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchImage(String urls[],long ids[]) {
        int len=urls.length;
        for(int i=0;i<len;++i){
            int index=urls[i].indexOf('/',7);
            if(index>7){
               urls[i]= urls[i].substring(0,index);
                Log.i(TAG+"baseUrl:",urls[i]);
            }
        }
        for(int i=0;i<len;++i){
            String iconUrl= null;
            iconUrl = findIconUrl(urls[i]);
            if(iconUrl!=null) {
                Log.i(TAG+"iconUrl",iconUrl);
                updateIcon(iconUrl, ids[i]);
            }
        }
    }
    private String findIconUrl(String urls)  {

        try {
            URL url=new URL(urls);
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(500);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            InputStream stream=connection.getInputStream();
            BufferedReader reader=new BufferedReader(new InputStreamReader(stream));
            String line=null,iconUrl=null;
            Pattern feedIconPattern = Pattern.compile("[.]*<link[^>]* (rel=(\"shortcut icon\"|\"icon\"|icon|\"apple-touch-icon\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* rel=(\"shortcut icon\"|\"icon\"|icon))[^>]*>", Pattern.CASE_INSENSITIVE);
            while((line=reader.readLine())!=null){
                if(line.indexOf("<body")>-1)
                    break;
                else{
                    Matcher matcher=feedIconPattern.matcher(line);
                    if(matcher.find()){
                        String l=matcher.group();
                        Log.i("Baseurl",l);
                        iconUrl=getHref(l,urls);
                        return iconUrl;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void updateIcon(String iconUrl,long id){
        try {
            byte iconBytes[]=null;
            URL url=new URL(iconUrl);
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(500);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            InputStream stream=connection.getInputStream();
            iconBytes=getBytes(stream);
            ContentValues values=new ContentValues();
            values.put(FeedContract.FeedColumns.ICON,iconBytes);
            getContentResolver().update(FeedContract.FeedColumns.CONTENT_URI(id),values,null,null);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private static String getHref(String line, String baseUrl) {
        int posStart = line.indexOf("href=\"");

        if (posStart > -1) {
            String url = line.substring(posStart+6, line.indexOf('"', posStart+10)).replace("&amp;", "&");

            if (url.startsWith("//")) {
                url=new StringBuilder("http:").append(url).toString();
            }else if(url.startsWith("/")){
                url = baseUrl+url;;
            }
            else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = new StringBuilder(baseUrl).append('/').append(url).toString();
            }
            return url;
        } else {
            return null;
        }
    }
    private static byte[] getBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte buffer[]=new byte[4096];
        int n;
        while((n=stream.read(buffer))>0){
            output.write(buffer,0,n);
        }
        byte []result=output.toByteArray();
        output.close();
        stream.close();
        return result;
    }


}
