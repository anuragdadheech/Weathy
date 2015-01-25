package com.nlmm01.weathy;

import com.nlmm01.weathy.data.WeatherContract;
import com.nlmm01.weathy.data.WeatherContract.WeatherEntry;
import com.nlmm01.weathy.data.WeatherContract.LocationEntry;
import com.nlmm01.weathy.service.WeathyService;
import com.nlmm01.weathy.sync.WeathySyncAdapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private  String mLocation;
    private static final int FORECAST_LOADER = 0;
    private static final String CLICK_POSITION = "click_position";
    private int mposition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;
    private ListView forecast;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID
    };
    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;


    public ForecastFragment() {
    }

    private ForecastAdapter weatherListAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !Utility.getPreferredLocation(this.getActivity()).equals(mLocation)){
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_geo) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_default_location));
            Intent geoIntent = new Intent (Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + location)); // Prepare intent
            ActivityInfo activityInfo = geoIntent.resolveActivityInfo(getActivity().getPackageManager(), geoIntent.getFlags());
            if (activityInfo.exported) {
                startActivity(geoIntent);
            }

        }

        return super.onOptionsItemSelected(item);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(weatherListAdapter != null){
            weatherListAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }

    private void updateWeather(){
        WeathySyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        weatherListAdapter = new ForecastAdapter(getActivity(),null,0);
        weatherListAdapter.setUseTodayLayout(mUseTodayLayout);
        forecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecast.setAdapter(weatherListAdapter);
        forecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mposition = position;
                ForecastAdapter detailAadapter = (ForecastAdapter) parent.getAdapter();
                Cursor detailCursor = detailAadapter.getCursor();
                if (detailCursor != null && detailCursor.moveToPosition(position)) {
                    String dateString = Utility.formatDate(detailCursor.getString(COL_WEATHER_DATE));
                    String weatherDescription = detailCursor.getString(COL_WEATHER_DESC);

                    boolean isMetric = Utility.isMetric(getActivity());
                    String high = Utility.formatTemperature(getActivity(),
                            detailCursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
                    String low = Utility.formatTemperature(getActivity(),
                            detailCursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric);

                    String detailString = String.format("%s - %s - %s/%s",
                            dateString, weatherDescription, high, low);

                    ((Callback)getActivity()).onItemSelected(detailCursor.getString(COL_WEATHER_DATE));
                }


                //Toast.makeText(getActivity(), (String) weatherListAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            }
        });
        if(savedInstanceState!=null && savedInstanceState.containsKey(CLICK_POSITION)){
            mposition = savedInstanceState.getInt(CLICK_POSITION);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mposition!= ListView.INVALID_POSITION){
            outState.putInt(CLICK_POSITION, mposition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        weatherListAdapter.swapCursor(data);
        if(mposition!=ListView.INVALID_POSITION){
            forecast.setSelection(mposition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        weatherListAdapter.swapCursor(null);
    }
}