package com.example.android.earthquakes_report_from_USGS;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class EarthquakeActivity extends AppCompatActivity
        implements LoaderCallbacks<List<EarthquakeObject>>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = EarthquakeActivity.class.getName();

    private static final String USGS_REQUEST_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query?";

    private static final String FORMAT = "format";
    private static final String FORMAT_GEOJSON = "geojson";
    private static final String ORDER_BY = "orderby";
    private static final String MINIMUM_MAGNITUDE = "minmag";
    private static final String MAXIMUM_MAGNITUDE = "maxmag";
    private static final String START_TIME = "starttime";
    private static final String END_TIME = "endtime";

    private static final int EARTHQUAKE_LOADER_ID = 1;
    private EarthquakeAdapter mAdapter;
    private TextView mEmptyStateTextView;
    private TextView mTryConnectAgain;
    private ListView mEarthquakeListView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

        mEarthquakeListView = (ListView) findViewById(R.id.list);
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        mTryConnectAgain = (TextView) findViewById(R.id.try_connect_again);
        mProgressBar = (ProgressBar) findViewById(R.id.loading_indicator);

        mAdapter = new EarthquakeAdapter(this, new ArrayList<EarthquakeObject>());
        mEarthquakeListView.setAdapter(mAdapter);

        mEarthquakeListView.setEmptyView(mProgressBar);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        connectToServer();

        mEarthquakeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                EarthquakeObject currentEarthquake = mAdapter.getItem(position);
                Uri earthquakeUri = Uri.parse(currentEarthquake.getUrl());
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, earthquakeUri);
                startActivity(websiteIntent);
            }
        });


        mTryConnectAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmptyStateTextView.setVisibility(View.GONE);
                mTryConnectAgain.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                connectToServer();

            }
        });


    }

    private void connectToServer() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(EARTHQUAKE_LOADER_ID, null, this);
        } else {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mTryConnectAgain.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }

    }

    private void updateUi(List<EarthquakeObject> earthquake) {
        mAdapter.addAll(earthquake);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(getString(R.string.settings_order_by_key)) ||
                key.equals(getString(R.string.settings_min_magnitude_key)) ||
                key.equals(getString(R.string.settings_max_magnitude_key)) ||
                key.equals(getString(R.string.settings_start_date_key)) ||
                key.equals(getString(R.string.settings_end_date_key))) {

            mAdapter.clear();

            mEmptyStateTextView.setVisibility(View.GONE);

            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.VISIBLE);

            getLoaderManager().restartLoader(EARTHQUAKE_LOADER_ID, null, this);

        }
    }

    @Override
    public Loader<List<EarthquakeObject>> onCreateLoader(int i, Bundle bundle) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));


        String minMagnitude = sharedPrefs.getString(
                getString(R.string.settings_min_magnitude_key),
                getString(R.string.settings_min_magnitude_default));

        String maxMagnitude = sharedPrefs.getString(
                getString(R.string.settings_max_magnitude_key),
                getString(R.string.settings_max_magnitude_default));


        String startTime = sharedPrefs.getString(
                getString(R.string.settings_start_date_key),
                getString(R.string.settings_start_date_default));

        String endTime = sharedPrefs.getString(
                getString(R.string.settings_end_date_key),
                getString(R.string.settings_end_date_default));


        Uri baseUri = Uri.parse(USGS_REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        uriBuilder.appendQueryParameter(FORMAT, FORMAT_GEOJSON);
        uriBuilder.appendQueryParameter(ORDER_BY, orderBy);
        uriBuilder.appendQueryParameter(MINIMUM_MAGNITUDE, minMagnitude);
        uriBuilder.appendQueryParameter(MAXIMUM_MAGNITUDE, maxMagnitude);
        uriBuilder.appendQueryParameter(START_TIME, startTime);
        uriBuilder.appendQueryParameter(END_TIME, endTime);

        return new EarthquakeLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<EarthquakeObject>> loader, List<EarthquakeObject> earthquakes) {

        mProgressBar.setVisibility(View.GONE);

        if (earthquakes != null && !earthquakes.isEmpty()) {
            updateUi(earthquakes);
        } else {
            mEmptyStateTextView.setText(R.string.no_earthquakes);
        }



    }

    @Override
    public void onLoaderReset(Loader<List<EarthquakeObject>> loader) {

        mAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
