package com.example.lugeke.rssreader;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.content.Loader;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.example.lugeke.rssreader.provider.FeedContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.view.ViewPager;
import android.support.v4.content.CursorLoader;
public class EntryListActivity extends FragmentActivity  implements ActionBar.TabListener{

    private static String TAG="EntryListActivity";
    private  static long id;
    private   String title;
    private static byte image[];

    ViewPager mViewPager;

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
            R.id.date,
    };




    EntryListPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrylist_viewpager);
        Log.i(TAG, "on create");
        id=getIntent().getLongExtra(MainActivity.feedID, 1);
        title=getIntent().getStringExtra(MainActivity.feedName);
        image=getIntent().getByteArrayExtra(MainActivity.feedICON);
        setTitle(title);

        final ActionBar actionBar=getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setHomeButtonEnabled(true);
        Bitmap icon;
        Drawable d=null;
        if(image!=null) {
            icon = BitmapFactory.decodeByteArray(image, 0, image.length);
            d=new BitmapDrawable(getResources(),icon);
            actionBar.setIcon(d);
        }
        else{
            actionBar.setIcon(R.drawable.feedicon);
        }




        mPagerAdapter=new EntryListPagerAdapter(getSupportFragmentManager());


        mViewPager=(ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for(int i=0;i<mPagerAdapter.getCount();++i){
            actionBar.addTab(actionBar.newTab().setText(mPagerAdapter.getPageTitle(i)).setTabListener(this));
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {


    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }


    public static class EntryListPagerAdapter extends FragmentPagerAdapter {

        public EntryListPagerAdapter(FragmentManager fm){
            super(fm);
        }
        @Override
        public Fragment getItem(int i) {

            Log.i(TAG,"getItem"+i);

            listFragment l=new listFragment();
            Bundle args=new Bundle();
            args.putInt(listFragment.ARG_OBJ,i);
            l.setArguments(args);
            return l;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
           if(position==0)
               return "All";
            else
               return "Star";
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




    public static class listFragment extends ListFragment implements  LoaderManager.LoaderCallbacks<Cursor>{


        private static String ARG_OBJ;
        private  SimpleCursorAdapter mAdapter;

        @Override
        public void onStart() {
            super.onStart();
            Bundle args=getArguments();
            int id=args.getInt(ARG_OBJ);
            if(getLoaderManager().getLoader(id)!=null){
                getLoaderManager().restartLoader(id,null,this);
            }

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAdapter=new SimpleCursorAdapter(getActivity(),R.layout.entrylist_layout,null,FROM_COLUMNS,TO_FIELDS,0);
            mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int i) {
                    if (i == COLUMN_DATE) {
                        Time t = new Time();
                        t.set(cursor.getLong(i));
                        ((TextView) view).setText(t.format("%Y-%m-%d %H:%M"));
                        return true;
                    } else
                        return false;
                }
            });

            Bundle args=getArguments();



            setListAdapter(mAdapter);
            getLoaderManager().initLoader(args.getInt(ARG_OBJ),null,this);
        }





        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            Intent i=new Intent(getActivity(),EntryItemActivity.class);
            Cursor c=(Cursor)mAdapter.getItem(position);
            String title=c.getString(COLUMN_TITLE);
            i.putExtra(entryIcon,image);
            i.putExtra(entryID,id);
            i.putExtra(entryTitle, title);
            ContentValues values=new ContentValues();
            values.put(FeedContract.EntryColumns.ISREAD,1);
            getActivity().getContentResolver().update(FeedContract.EntryColumns.ENTRY_CONTENT_URI(id),values,null,null);
            startActivity(i);
        }


        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            Log.i(TAG,"onCreateLoader"+i);
            if(i==0)
                return new CursorLoader(getActivity(),FeedContract.EntryColumns.CONTENT_URI(id),PROJECTION,null,null,FeedContract.EntryColumns.DATE+" desc ");

            else{
                return new  CursorLoader(getActivity(),FeedContract.EntryColumns.CONTENT_URI(id),PROJECTION,FeedContract.EntryColumns.FAVORITE+" =1 ",null,FeedContract.EntryColumns.DATE+" desc ");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mAdapter.changeCursor(null);
        }
    }
}
