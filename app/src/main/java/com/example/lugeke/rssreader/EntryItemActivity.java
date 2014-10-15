package com.example.lugeke.rssreader;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.example.lugeke.rssreader.provider.FeedContract;


public class EntryItemActivity extends Activity {

    private long id;
    private String title;
    private WebView webView;
    private byte[] image;
    private int fav;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_entry_item);
        webView=(WebView)findViewById(R.id.entry_item);
        id=getIntent().getLongExtra(EntryListActivity.entryID, 1);
        title=getIntent().getStringExtra(EntryListActivity.entryTitle);
        image=getIntent().getByteArrayExtra(EntryListActivity.entryIcon);
        setTitle(title);
        Cursor c = getContentResolver().query(
                FeedContract.EntryColumns.ENTRY_CONTENT_URI(id),
                new String[] {
                        FeedContract.EntryColumns.ABSTRACT }, null, null,
                null);
        String content=null;
        if(c.moveToFirst()){
            content=c.getString(0);
        }c.close();



        Bitmap icon;
        Drawable d=null;
        if(image!=null) {
            icon = BitmapFactory.decodeByteArray(image, 0, image.length);
            d=new BitmapDrawable(getResources(),icon);
            getActionBar().setIcon(d);
        }
        else{
            getActionBar().setIcon(R.drawable.feedicon);
        }

        webView.loadDataWithBaseURL(null,content,"text/html","utf-8",null);
        
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry_item, menu);
        updateFavorite(menu);
        return true;
    }

    private void updateFavorite(Menu menu){
        MenuItem item=menu.getItem(0);
        Cursor c=getContentResolver().query(
                FeedContract.EntryColumns.ENTRY_CONTENT_URI(id),
                new String[] {
                        FeedContract.EntryColumns.FAVORITE }, null, null,
                null);
        if(c.moveToFirst()){
            fav=c.getInt(0);
        }c.close();
        if(fav==0){
            item.setIcon(R.drawable.grey_heart);
        }else{
            item.setIcon(R.drawable.red_heart);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_favorite:
                ContentValues values=new ContentValues();
                if(fav==1){
                    item.setIcon(R.drawable.grey_heart);
                    values.put(FeedContract.EntryColumns.FAVORITE,0);
                    fav=0;
                }else{
                    item.setIcon(R.drawable.red_heart);
                    values.put(FeedContract.EntryColumns.FAVORITE,1);
                    fav=1;
                }
                getContentResolver().update(FeedContract.EntryColumns.ENTRY_CONTENT_URI(id),values,null,null);
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
