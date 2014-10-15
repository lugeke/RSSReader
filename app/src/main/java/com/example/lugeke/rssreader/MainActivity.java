package com.example.lugeke.rssreader;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lugeke.rssreader.provider.FeedContract;
import com.example.lugeke.rssreader.service.mService;

import org.apache.http.protocol.HTTP;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>{


    private static final String TAG="MainActivity";
    private SimpleCursorAdapter mAdapter;


    private static final String[] PROJECTION=new String[]{
            FeedContract.FeedColumns._ID,
            FeedContract.FeedColumns.NAME,
            FeedContract.FeedColumns.LASTUPDATE,
            FeedContract.FeedColumns.ICON,
    };



    private static final int COLUMN_ID=0;
    private static final int COLUMN_NAME=1;
    private static final int COLUMN_LASTUPDATE=2;
    private static final int COLUMN_ICON=3;

    private static final String[] FROM_COLUMNS = new String[]{
            FeedContract.FeedColumns.ICON,
            FeedContract.FeedColumns.NAME,
            FeedContract.FeedColumns.LASTUPDATE
    };

    private static final int[] TO_FIELDS = new int[]{
            R.id.icon,
            R.id.name,
            R.id.lastupdate
    };
    private static final String PREF_SETUP_COMPLETE="setup";

    private ListView listView;
    ProgressBar progressBar;

    static EditText url;
    static EditText name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        init();
        updateImage();
        deleteOldFeed();
        listView=getListView();
         progressBar=new ProgressBar(this);
        progressBar.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        ViewGroup root=(ViewGroup)findViewById(android.R.id.content);
        root.addView(progressBar);

        mAdapter=new SimpleCursorAdapter(this,R.layout.feeds_layout,null,FROM_COLUMNS,TO_FIELDS,0);
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                if (i == COLUMN_LASTUPDATE) {
                    Time t = new Time();
                    long time = cursor.getLong(i);
                    t.set(time);
                    if (time == 0)
                        ((TextView) view).setText("Never");
                    else
                        ((TextView) view).setText(t.format("%Y-%m-%d %H:%M"));
                    return true;
                } else if (i == COLUMN_ICON) {
                    byte icon[] = cursor.getBlob(i);
                    if (icon != null) {
                        Bitmap bm = BitmapFactory.decodeByteArray(icon, 0, icon.length);
                        ((ImageView) view).setImageBitmap(bm);
                    } else {
                        ((ImageView) view).setImageResource(R.drawable.feedicon);
                    }
                    return true;
                } else
                    return false;
            }

        });
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
        SyncUtils.CreateSyncAccount(getApplicationContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
       // listView.setBackgroundResource(R.drawable.list);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            List<Long> id = new ArrayList<Long>();

            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {

                if (b) {
                    Toast.makeText(getApplicationContext(), "select at" + l, Toast.LENGTH_SHORT).show();
                    id.add(l);
                    //listView.getChildAt(i).setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                    // listView.getSelectedView().setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                    //((View)listView.getItemAtPosition(i)).setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                } else {
                    Toast.makeText(getApplicationContext(), "cancel at" + l, Toast.LENGTH_SHORT).show();
                    // ((View)listView.getItemAtPosition(i)).setBackgroundColor(getResources().getColor(android.R.color.background_light));
                    id.remove(l);
                    // listView.setItemChecked(i,false);
                }

                Log.i(TAG, id.toString());
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.feeds_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                id.clear();
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_share:
                        shareCurrentItem(id);
                        actionMode.finish();
                        return true;
                    case R.id.menu_delete:
                        deleteCurrentItem(id);
                        actionMode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });
    }

    private void shareCurrentItem(List<Long> l){
            String title=getResources().getString(R.string.choose_title);
            Intent sendIntent=new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            StringBuilder content=new StringBuilder("add_feed url:\n");
            Cursor c=getContentResolver().query(FeedContract.FeedColumns.CONTENT_URI,new String[]{FeedContract.FeedColumns.URL},null,null,null);
            int count=c.getCount();
            if(count==0) {
                c.close();
                Toast.makeText(this, "No feeds", Toast.LENGTH_SHORT).show();
            }
            else{
                for(int i=0;i<count;++i) {
                    c.moveToPosition(i);
                    content.append(c.getString(0)).append("\n");
                }
                c.close();
                sendIntent.putExtra(Intent.EXTRA_TEXT,content.toString());
                sendIntent.setType(HTTP.PLAIN_TEXT_TYPE);
                startActivity(Intent.createChooser(sendIntent,title));

            }

    }
    private void deleteCurrentItem(List<Long> l){
        for(long i:l){
            getContentResolver().delete(FeedContract.FeedColumns.CONTENT_URI(i),null,null);
        }
        getLoaderManager().restartLoader(0,null,this);
    }




    private void init(){
        boolean isFirst= PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_SETUP_COMPLETE,true);
        if(isFirst){

            ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
            String urls[]={"http://zhihu.com/rss","http://www.guokr.com/rss/","http://zh.lucida.me/atom.xml","http://blog.sina.com.cn/rss/2640115247.xml",
                    "http://blog.jobbole.com/add_feed/"
            };
            String names[]={"知乎每日精选","果壳网","lucida","计算机2011级工作博客",
                    "博客 - 伯乐在线",

            };

            Uri uri=FeedContract.FeedColumns.CONTENT_URI;
            for(int i=0;i<urls.length;++i){
                batch.add(ContentProviderOperation.newInsert(uri).withValue(FeedContract.FeedColumns.URL, urls[i])
                        .withValue(FeedContract.FeedColumns.NAME, names[i]).build());
            }
            try {
                getContentResolver().applyBatch(FeedContract.AUTHORITY,batch );
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREF_SETUP_COMPLETE,false).commit();
        }
    }
    private void updateImage(){


        ConnectivityManager connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        if(networkInfo!=null&&networkInfo.isConnected()) {
            Cursor c;
            c = getContentResolver().query(FeedContract.FeedColumns.CONTENT_URI, new String[]{FeedContract.FeedColumns._ID, FeedContract.FeedColumns.URL}, null, null, null);
            int count = c.getCount();
            String[] urls = new String[count];
            long[] ids = new long[count];
            if (count > 0) {
                for (int i = 0; i < count; ++i) {
                    c.moveToPosition(i);
                    ids[i] = c.getLong(0);
                    urls[i] = new String(c.getString(1));
                }
                mService.startActionFetchImage(getApplicationContext(), urls, ids);
            }
        }else{
            Toast.makeText(this,getString(R.string.nonetwork),Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteOldFeed(){

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    public static String feedName="feedName";
    public static String feedID="feedID";
    public static String feedICON="feedIcon";
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent entrylist=new Intent(this,EntryListActivity.class);
        Cursor c=(Cursor)mAdapter.getItem(position);
        String name=c.getString(COLUMN_NAME);
        byte b[]=c.getBlob(COLUMN_ICON);
        entrylist.putExtra(feedName,name);
        entrylist.putExtra(feedID,id);
        entrylist.putExtra(feedICON,b);
        startActivity(entrylist);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_add:
                openAdd();
                return true;
            case R.id.action_refresh:
                openRefresh();
                return true;
            case R.id.action_setting:
                openSetting();
                return true;
            /*case R.id.action_share:
                openShare();
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }

    }



    public void openSetting(){}
    public void openRefresh(){
        SyncUtils.TriggerRefresh();
    }
    public void openAdd(){
        /*Intent addIntent=new Intent(this,FeedAddActivity.class);
        startActivity(addIntent);*/
        DialogFragment f=new addFragment();
        f.setCancelable(false);

        f.show(getFragmentManager(), "add");

    }
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Log.i(TAG,"onCreateLoader");
        return new CursorLoader(this,FeedContract.FeedColumns.CONTENT_URI,PROJECTION,null,null,null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.i(TAG,"onLoaderReset");
        mAdapter.changeCursor(null);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
        progressBar.setVisibility(View.GONE);
        TextView t=new TextView(this);
        t.setText(getString(R.string.nofeeds));
        getListView().setEmptyView(t);
        ViewGroup root=(ViewGroup)findViewById(android.R.id.content);
        root.addView(t);
        Log.i(TAG,"onLoadFinished");
    }

    public  static  class addFragment extends DialogFragment {


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
            LayoutInflater inflater=getActivity().getLayoutInflater();
            final View  v=inflater.inflate(R.layout.add_dialog, null);
            builder.setIcon(R.drawable.add_feed).setTitle("Add new feed").setView(v).setPositiveButton(R.string.addok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {


                    url = (EditText) v.findViewById(R.id.add_url);
                    name = (EditText) v.findViewById(R.id.add_name);

                    if (TextUtils.isEmpty(url.getText().toString())) {
                        Toast.makeText(getActivity(), "The url can not be empty!", Toast.LENGTH_SHORT).show();

                    } else {
                        StringBuilder getUrl = new StringBuilder("http://");
                        String urls = url.getText().toString();
                        String getName = name.getText().toString();
                        if (!urls.startsWith("http://")) {
                            getUrl.append(urls);
                        } else {
                            getUrl.append(urls.substring(7));
                        }
                        ContentValues values = new ContentValues();
                        String url = getUrl.toString();
                        values.put(FeedContract.FeedColumns.URL, url);
                        if (TextUtils.isEmpty(getName)) {
                            int index = url.indexOf("/", 7);
                            if (index != -1)
                                getName = url.substring(0, index);
                            else
                                getName = url;
                        }
                        values.put(FeedContract.FeedColumns.NAME, getName);
                        Uri u = getActivity().getContentResolver().insert(
                                FeedContract.FeedColumns.CONTENT_URI,
                                values);
                    }


                }

            }).setNegativeButton(R.string.addcancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //dismiss();
                }
            });
            return builder.create();
        }
    }
}
