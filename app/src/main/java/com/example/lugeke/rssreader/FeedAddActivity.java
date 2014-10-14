package com.example.lugeke.rssreader;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.lugeke.rssreader.provider.FeedContract;


public class FeedAddActivity extends Activity {

    EditText url;
    EditText name;
    Button ok,cancel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_add);
        url = (AutoCompleteTextView) findViewById(R.id.add_url);
        name = (EditText) findViewById(R.id.add_name);
    }

    public void callOk(View v){

        if (TextUtils.isEmpty(url.getText().toString())) {
            Toast.makeText(this, "The url can not be empty!", Toast.LENGTH_SHORT).show();
        } else {
            StringBuilder getUrl = new StringBuilder("http://");
            String urls=url.getText().toString();
            String getName = name.getText().toString();
            if(!urls.startsWith("http://")){
                getUrl.append(urls);
            }else {
                getUrl.append(urls.substring(7));
            }
            ContentValues values = new ContentValues();
            String url=getUrl.toString();
            values.put(FeedContract.FeedColumns.URL, url);
            if(TextUtils.isEmpty(getName)){
                int index=url.indexOf("/",7);
                if(index!=-1)
                    getName=url.substring(0, index);
                else
                    getName=url;
            }
            values.put(FeedContract.FeedColumns.NAME, getName);
            Uri u =getContentResolver().insert(
                    FeedContract.FeedColumns.CONTENT_URI,
                    values);
            finish();
        }
    }
    public void callCancel(View v) {
        finish();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feed_add, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
