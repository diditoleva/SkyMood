package com.example.owner.skymood.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.skymood.R;
import com.example.owner.skymood.SwipeViewActivity;
import com.example.owner.skymood.asyncTasks.APIDataGetterAsyncTask;
import com.example.owner.skymood.asyncTasks.AutoCompleteStringFillerAsyncTask;
import com.example.owner.skymood.asyncTasks.FindLocationAsyncTask;
import com.example.owner.skymood.listeners.KeyboardListener;
import com.example.owner.skymood.listeners.LocationSearchButtonListener;
import com.example.owner.skymood.listeners.MyLocationsSpinnerListener;
import com.example.owner.skymood.listeners.SearchButtonListener;
import com.example.owner.skymood.listeners.SyncButtonListener;
import com.example.owner.skymood.listeners.TextChangedListener;
import com.example.owner.skymood.model.LocationPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class CurrentWeatherFragment extends Fragment implements Swideable {

    private static final String DEFAULT_CITY = "Sofia";
    private static final String DEFAULT_COUNTRY_CODE = "BG";
    private static final String DEFAULT_COUNTRY = "Bulgaria";

    private ProgressBar progressBar;
    private TextView chosenCityTextView;
    private Spinner spinner;
    private ImageView syncButton;
    private ImageView locationSearchButton;
    private ImageView citySearchButton;
    private AutoCompleteTextView writeCityEditText;
    private TextView temperature;
    private TextView condition;
    private TextView feelsLike;
    private TextView lastUpdate;
    private TextView countryTextView;
    private TextView minTempTextView;
    private TextView maxTempTextView;
    private ImageView weatherImage;

    private String city;
    private String country;
    private String countryCode;
    private String cityToDisplay;
    private String minTemp;
    private String maxTemp;
    private String dateAndTime;
    private HashMap<String, String> cities;
    private ArrayList<String> autoCopleteNames;
    private ArrayAdapter adapterAutoComplete;

    private InputMethodManager keyboard;
    private Context context;
    private LocationPreference locPref;

    public CurrentWeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_current_weather, container, false);

        syncButton = (ImageView) rootView.findViewById(R.id.synchronize);
        locationSearchButton = (ImageView) rootView.findViewById(R.id.gpsSearch);
        citySearchButton = (ImageView) rootView.findViewById(R.id.citySearch);
        writeCityEditText = (AutoCompleteTextView) rootView.findViewById(R.id.writeCityEditText);
        writeCityEditText.setThreshold(3);
        temperature = (TextView) rootView.findViewById(R.id.temperatureTextView);
        countryTextView = (TextView) rootView.findViewById(R.id.country);
        condition = (TextView) rootView.findViewById(R.id.conditionTextView);
        minTempTextView = (TextView) rootView.findViewById(R.id.minTemp);
        maxTempTextView = (TextView) rootView.findViewById(R.id.maxTemp);
        feelsLike = (TextView) rootView.findViewById(R.id.feelsLikeTextView);
        lastUpdate = (TextView) rootView.findViewById(R.id.lastUpdateTextView);
        weatherImage = (ImageView) rootView.findViewById(R.id.weatherImageView);
        chosenCityTextView = (TextView) rootView.findViewById(R.id.chosenCity);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        spinner = (Spinner) rootView.findViewById(R.id.locationSpinner);

        //TODO filling the spinner from myLocations or from DB
        ArrayList<String> citiesSpinner =  new ArrayList<>();
        citiesSpinner.add("My Locations");
        citiesSpinner.add("Plovdiv, Bulgaria");
        ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, citiesSpinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new MyLocationsSpinnerListener(this, weatherImage));

        citySearchButton.setOnClickListener(new SearchButtonListener(context, this, writeCityEditText, keyboard));

        TextView.OnEditorActionListener searchButtonPressed = new KeyboardListener(this, writeCityEditText, keyboard, cities);
        writeCityEditText.setOnEditorActionListener(searchButtonPressed);

        writeCityEditText.addTextChangedListener(new TextChangedListener(this, writeCityEditText));

        syncButton.setOnClickListener(new SyncButtonListener(this, weatherImage, countryCode, city, country));

        locationSearchButton.setOnClickListener(new LocationSearchButtonListener(this));

        //shared prefs
        locPref = LocationPreference.getInstance(context);

        //TODO remove, for now hard coded for demo
        //locPref.setPreferredLocation("Burgas", "Bulgaria", "BG", "clear", "19.9", "15", "21", "Clear", "Feels like: 20", "Last update: 05.04.2016, 18:00");

        if(isOnline()){
            APIDataGetterAsyncTask task = new APIDataGetterAsyncTask(this, context, weatherImage);
            //first: check shared prefs
            if(locPref.isSetLocation()){
                setCity(locPref.getCity());
                countryCode = locPref.getCountryCode();
                country = locPref.getCountry();
                task.execute(countryCode, city, country);
            } else {
                //API autoIP
                findLocation();
            }
            if(city == null) {
                setCity(DEFAULT_CITY);
                countryCode = DEFAULT_COUNTRY_CODE;
                country = DEFAULT_COUNTRY;
                task.execute(countryCode, city, country);
            }
        } else {
            if(locPref.isSetLocation()){
                Toast.makeText(context, "NO INTERNET CONNECTION\nFor up to date info connect to Internet", Toast.LENGTH_LONG).show();
                setCity(locPref.getCity());
                country = locPref.getCountry();
                countryCode = locPref.getCountryCode();
                getWeatherInfoFromSharedPref();
            } else {
                feelsLike.setText("Please connect to Internet");
            }
        }
        return rootView;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    public void getWeatherInfoFromSharedPref(){
        chosenCityTextView.setVisibility(View.VISIBLE);
        chosenCityTextView.setText(locPref.getCity());
        countryTextView.setText(country);
        temperature.setText(locPref.getTemperature() + "°");
        minTempTextView.setText("⬇" + locPref.getMinTemp() + "°");
        maxTempTextView.setText("⬆" + locPref.getMaxTemp() + "°");
        condition.setText(locPref.getCondition());
        feelsLike.setText(locPref.getFeelsLike());
        lastUpdate.setText(locPref.getLastUpdate());

        Context con = weatherImage.getContext();
        weatherImage.setImageResource(context.getResources().getIdentifier(locPref.getIcon(), "drawable", con.getPackageName()));
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void setCity(String city){
        this.city = city.replace(" ", "_");
        this.city.toLowerCase();
        this.cityToDisplay = city.toUpperCase();
    }

    public void getWeatherInfoByCity(String city){
        if(city != null && !city.isEmpty()) {
            setCity(city);
            writeCityEditText.setText("");
            writeCityEditText.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
            syncButton.setVisibility(View.VISIBLE);
            locationSearchButton.setVisibility(View.VISIBLE);
            APIDataGetterAsyncTask task = new APIDataGetterAsyncTask(this, context, weatherImage);
            task.execute(countryCode, city, country);
        }
    }

    public void apiDataGetterAsyncTaskOnPreExecute(){
        chosenCityTextView.setVisibility(View.GONE);
        countryTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void apiDataGetterAsyncTaskOnPostExecute(String temp, String condition, String feelsLike,
                                    String minTemp, String maxTemp, String dateAndTime, String lastUpdate){
        this.progressBar.setVisibility(View.GONE);
        this.chosenCityTextView.setVisibility(View.VISIBLE);
        this.chosenCityTextView.setText(cityToDisplay);
        this.countryTextView.setVisibility(View.VISIBLE);
        this.countryTextView.setText(country);
        if(temp != null) {
            this.temperature.setText(temp + "°");
            this.condition.setText(condition);
            this.feelsLike.setText(feelsLike);
            this.minTempTextView.setText("⬇" + minTemp + "°");
            this.minTemp = minTemp;
            this.maxTempTextView.setText("⬆" + maxTemp + "°");
            this.maxTemp = maxTemp;
            this.lastUpdate.setText(lastUpdate);
            this.dateAndTime = dateAndTime;


        } else {
            this.temperature.setText("");
            this.condition.setText("");
            this.lastUpdate.setText("");
            this.maxTempTextView.setText("");
            this.minTempTextView.setText("");
            this.feelsLike.setText("Sorry, there is no \ninformation for that location.");
        }
    }

    public void autoCompleteStringFillerAsyncTaskOnPostExecute(ArrayAdapter adapterAutoComplete, HashMap<String, String> cities){
        this.writeCityEditText.setAdapter(adapterAutoComplete);
        this.adapterAutoComplete = adapterAutoComplete;
        this.cities = cities;
    }

    public void findLocation(){
        FindLocationAsyncTask findLocation = new FindLocationAsyncTask(this, context, weatherImage);
        findLocation.execute();
    }

    public void changeVisibility(int visibility){
        spinner.setVisibility(visibility);
        syncButton.setVisibility(visibility);
        locationSearchButton.setVisibility(visibility);
        weatherImage.setAdjustViewBounds(true);
    }

    public void setLocation(String city, String country, String countryCode){
        this.city = city;
        this.country = country;
        this.countryCode = countryCode;
    }
}

