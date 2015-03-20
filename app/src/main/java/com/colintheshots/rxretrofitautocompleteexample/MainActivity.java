package com.colintheshots.rxretrofitautocompleteexample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;
import rx.android.observables.ViewObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action1;
import rx.functions.Func1;


public class MainActivity extends Activity {

    private interface GooglePlacesClient {

        @GET("/maps/api/place/autocomplete/json")
        Observable<PlacesResult> autocomplete(
            @Query("key") String key,
            @Query("input") String input);
    }

    private class PlacesResult {
        @Expose
        List<MainActivity.Prediction> predictions;
        @Expose
        String status;
    }

    private class Prediction {
        @Expose
        String description;
    }

    private static final String LOG_TAG = "RxRetrofitAutoComplete";
    private static final String GOOGLE_API_BASE_URL = "https://maps.googleapis.com";
    private static final int DELAY = 500;

    GooglePlacesClient mGooglePlacesClient;

    @InjectView(R.id.editText1)
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        if (Secrets.API_KEY.length()<10) {
            Toast.makeText(this, "API KEY is unset!", Toast.LENGTH_LONG).show();
            return;
        }

        if (mGooglePlacesClient == null) {
            mGooglePlacesClient = new RestAdapter.Builder()
                    .setConverter(new GsonConverter(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()))
                    .setEndpoint(GOOGLE_API_BASE_URL)
                    .setLogLevel(RestAdapter.LogLevel.FULL).build()
                    .create(GooglePlacesClient.class);
        }

        Observable<EditText> searchTextObservable = ViewObservable.text(editText);
        searchTextObservable.debounce(DELAY, TimeUnit.MILLISECONDS)
                .map(editText1 -> editText1.getText().toString())
                .flatMap(searchTerm -> {
                    Observable<PlacesResult> placesResult = null;
                    try {
                        placesResult = mGooglePlacesClient.autocomplete(Secrets.API_KEY, URLEncoder.encode(searchTerm, "utf8"));
                    } catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                    return placesResult;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(placesResult ->
                    {
                        List<String> strings = new ArrayList<String>();
                        for (MainActivity.Prediction p : placesResult.predictions) {
                            strings.add(p.description);
                        }
                        ListView listView = (ListView) findViewById(R.id.listView1);
                        if (listView != null) {
                            listView.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, strings));
                        }
                    }, Throwable::printStackTrace);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
