package com.example.lugeke.rssreader;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.example.lugeke.rssreader.provider.FeedContract;

public class EntryListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private  long id;
    private   String title;
    private byte image[];
    private SimpleCursorAdapter mAdapter;

    private static final String[] PROJECTION=new String[]{
            FeedContract.EntryColumns._ID,
            FeedContract.EntryColumns.TITLE,
            FeedContract.EntryColumns.DATE,
            FeedContract.EntryColumns.ISREAD,
    };

    private static final int COLUMN_TITLE=1;
    private static final int COLUMN_DATE=2;
    private static final int COLUMN_ISREAD=3;

    private static final String[] FROM_COLUMNS = new String[]{
            FeedContract.EntryColumns.TITLE,
            FeedContract.EntryColumns.DATE,
    };

    private static final int[] TO_FIELDS = new int[]{
            R.id.title,
            R.id.date
    };

    private  ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id=getIntent().getLongExtra(MainActivity.feedID,1);
        title=getIntent().getStringExtra(MainActivity.feedName);
        image=getIntent().getByteArrayExtra(MainActivity.feedICON);
        setTitle(title);



         progressBar=new ProgressBar(this);
        progressBar.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        ViewGroup root=(ViewGroup)findViewById(android.R.id.content);
        root.addView(progressBar);

        mAdapter=new SimpleCursorAdapter(this,R.layout.entrylist_layout,null,FROM_COLUMNS,TO_FIELDS,0);
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                if(i==COLUMN_DATE){
                    Time t=new Time();
                    t.set(cursor.getLong(i));
                    ((TextView)view).setText(t.format("%Y-%m-%d %H:%M"));
                    return true;
                }else if(i==COLUMN_TITLE){
                    int flag = cursor.getInt(COLUMN_ISREAD);
                    if(flag==1){
                        ((TextView)view).setTextColor(getResources().getColor(android.R.color.darker_gray));

                    }
                    ((TextView)view).setText(cursor.getString(COLUMN_TITLE));
                    return true;
                }
                else
                    return false;
            }
        });
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0,null,this);

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

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.entry_list, menu);
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
    public static String entryID="entryID";
    public static String entryTitle="entryTitle";
    public static String entryIcon="entryIcon";
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i=new Intent(this,EntryItemActivity.class);
        Cursor c=(Cursor)mAdapter.getItem(position);
        String title=c.getString(COLUMN_TITLE);
        i.putExtra(entryIcon,image);
        i.putExtra(entryID,id);
        i.putExtra(entryTitle,title);
        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,FeedContract.EntryColumns.CONTENT_URI(id),PROJECTION,null,null,FeedContract.EntryColumns.DATE+" desc ");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
        progressBar.setVisibility(View.GONE);
        TextView t=new TextView(this);
        t.setText(getString(R.string.noentries));
        getListView().setEmptyView(t);
        ViewGroup root=(ViewGroup)findViewById(android.R.id.content);
        root.addView(t);
    }
}
