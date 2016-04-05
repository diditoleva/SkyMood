package com.example.owner.skymood.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.owner.skymood.R;
import com.example.owner.skymood.location.NetworkLocationListener;
import com.example.owner.skymood.model.LocationPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;


public class CurrentWeatherFragment extends Fragment implements NetworkLocationListener.LocationReceiver, Swideable {

    private ProgressBar progressBar;
    private TextView chosenCity;
    private Spinner spinner;
    private ImageView sync;
    private ImageView gpsSearch;
    private ImageView citySearch;
    private EditText writeCityEditText;
    private TextView temperature;
    private TextView condition;
    private TextView feelsLike;
    private TextView lastUpdate;
    private ImageView weatherImage;

    private Location location;
    private LocationManager locationManager;
    private NetworkLocationListener listener;
    private double latitude;
    private double longtitude;
    String city;
    String cityToDisplay;

    InputMethodManager keyboard;
    Context context;

    public CurrentWeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_current_weather, container, false);

        sync = (ImageView) rootView.findViewById(R.id.synchronize);
        gpsSearch = (ImageView) rootView.findViewById(R.id.gpsSearch);
        citySearch = (ImageView) rootView.findViewById(R.id.citySearch);
        writeCityEditText = (EditText) rootView.findViewById(R.id.writeCityEditText);
        temperature = (TextView) rootView.findViewById(R.id.temperatureTextView);
        condition = (TextView) rootView.findViewById(R.id.conditionTextView);
        feelsLike = (TextView) rootView.findViewById(R.id.feelsLikeTextView);
        lastUpdate = (TextView) rootView.findViewById(R.id.lastUpdateTextView);
        weatherImage = (ImageView) rootView.findViewById(R.id.weatherImageView);
        chosenCity = (TextView) rootView.findViewById(R.id.chosenCity);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        spinner = (Spinner) rootView.findViewById(R.id.locationSpinner);

        ArrayList<String> cities =  new ArrayList<>();
        cities.add("My Locations");
        cities.add("Sofia");
        cities.add("Burgas");
        cities.add("Plovdiv");
        ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, cities);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnMyLocationsSpinnerItemListener());

        citySearch.setOnClickListener(new OnCitySearchClickListener());

        View.OnKeyListener onSearchPressed = new OnSearchPressedListener();
        writeCityEditText.setOnKeyListener(onSearchPressed);

        sync.setOnClickListener(new OnSyncListener());

        //TODO gpsSearch.setOnClickListener();


        //shared prefs
        LocationPreference locPref = LocationPreference.getInstance(context);

        //for now hard coded for demo
        locPref.setPreferredLocation("Burgas", "19.9°", "Clear", "Feels like: 19.9℃", "05.04.2016, 18:00");

        if(locPref.isSetLocation()){
            setCity(locPref.getLocation());
        }

        //network location
        //getNetworkLocation();

        //TODO: gps


        MyTask task = new MyTask();
        task.execute();

        return rootView;
    }

    public void setCity(String city){
        this.city = city.replace(" ", "_");
        this.city.toLowerCase();
        this.cityToDisplay = city.toUpperCase();
    }

    public void getWeatherInfoByCity(String city){
        setCity(city);
        writeCityEditText.setText("");
        writeCityEditText.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        sync.setVisibility(View.VISIBLE);
        gpsSearch.setVisibility(View.VISIBLE);
        MyTask task = new MyTask();
        task.execute();
    }

   public void getNetworkLocation() {
        listener = new NetworkLocationListener(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
    }

    @Override
    public void receiveLocation(Location location) {
        this.location = location;
        this.latitude = location.getLatitude();
        this.longtitude = location.getLongitude();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        locationManager.removeUpdates(listener);
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }


    class MyTask extends AsyncTask<Void, Void, Void> {

        String location;
        String conditionn;
        String icon;
        String temp;
        String feelsLikee;

        @Override
        protected Void doInBackground(Void... params) {

            try {

                URL url = new URL("http://api.wunderground.com/api/b4d0925e0429238f/conditions/q/" + city + ".json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                Scanner sc = new Scanner(connection.getInputStream());
                StringBuilder body = new StringBuilder();
                while(sc.hasNextLine()){
                    body.append(sc.nextLine());
                }
                String info = body.toString();

                JSONObject jsonData = new JSONObject(info);
                JSONObject observation = (JSONObject) jsonData.get("current_observation");
                JSONObject locationObject = (JSONObject) observation.get("display_location");
                location = locationObject.getString("full");
                conditionn = observation.getString("weather");
                temp = observation.getString("temp_c") + "°";
                feelsLikee = "Feels like: " + observation.getString("feelslike_c") + "℃";
                icon = observation.getString("icon");

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            chosenCity.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressBar.setVisibility(View.GONE);
            chosenCity.setVisibility(View.VISIBLE);
            chosenCity.setText(location);
            temperature.setText(temp);
            condition.setText(conditionn);
            feelsLike.setText(feelsLikee);

            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            String update = "Last update: " + hour + ":" + c.get(Calendar.MINUTE) + " " +
                    c.get(Calendar.DATE) + "." + c.get(Calendar.MONTH) + "." + c.get(Calendar.YEAR);
            lastUpdate.setText(update);


            Context con = weatherImage.getContext();
            int id = 0;
            if(hour >= 6 && hour <= 19){
               id = context.getResources().getIdentifier(icon, "drawable", con.getPackageName());
            } else {
                id = context.getResources().getIdentifier(icon + "_night", "drawable", con.getPackageName());
            }
            weatherImage.setImageResource(id);
        }
    }

    private class OnSearchPressedListener implements View.OnKeyListener{
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if(keyCode == KeyEvent.KEYCODE_ENTER){
                getWeatherInfoByCity(writeCityEditText.getText().toString());
                keyboard.hideSoftInputFromWindow(writeCityEditText.getWindowToken(), 0);
                return true;
            }
            return false;
        }
    }

    private class OnCitySearchClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (writeCityEditText.getVisibility() == View.GONE) {
                spinner.setVisibility(View.GONE);
                sync.setVisibility(View.GONE);
                gpsSearch.setVisibility(View.GONE);
                //TODO: change animation
                Animation slide = new AnimationUtils().loadAnimation(getContext(), android.R.anim.fade_in);
                slide.setDuration(1000);
                writeCityEditText.startAnimation(slide);
                writeCityEditText.setVisibility(View.VISIBLE);
                writeCityEditText.setFocusable(true);
                writeCityEditText.requestFocus();
                keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(writeCityEditText, 0);
            } else {
                writeCityEditText.setVisibility(View.GONE);
                keyboard.hideSoftInputFromWindow(writeCityEditText.getWindowToken(), 0);
                spinner.setVisibility(View.VISIBLE);
                sync.setVisibility(View.VISIBLE);
                gpsSearch.setVisibility(View.VISIBLE);
            }
        }
    }

    private class OnMyLocationsSpinnerItemListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(!((String)parent.getItemAtPosition(position)).equals("My Locations")){
                setCity((String)parent.getItemAtPosition(position));
                MyTask task = new MyTask();
                task.execute();
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class OnSyncListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            MyTask task = new MyTask();
            task.execute();
        }
    }

}
