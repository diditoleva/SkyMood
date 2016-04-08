package com.example.owner.skymood.asyncTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.example.owner.skymood.fragments.CurrentWeatherFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Golemanovaa on 8.4.2016 г..
 */
public class FindLocationAsyncTask extends AsyncTask<Void, Void, Void> {

    private CurrentWeatherFragment fragment;
    private Context context;
    private ImageView weatherImage;
    private String city;
    private String countryCode;
    private String country;

    public FindLocationAsyncTask(CurrentWeatherFragment fragment, Context context, ImageView weatherImage){
        this.fragment = fragment;
        this.context = context;
        this.weatherImage = weatherImage;
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {
            URL url = new URL("http://api.wunderground.com/api/b4d0925e0429238f/geolookup/q/autoip.json");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();

            Scanner sc = new Scanner(con.getInputStream());
            StringBuilder body = new StringBuilder();
            while(sc.hasNextLine()){
                body.append(sc.nextLine());
            }
            String info = body.toString();

            JSONObject data = new JSONObject(info);
            JSONObject location = data.getJSONObject("location");
            countryCode = location.getString("country_iso3166");
            country = location.getString("country_name");
            city = location.getString("city");

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
    protected void onPostExecute(Void aVoid) {
        fragment.setCity(city);
        APIDataGetterAsyncTask task = new APIDataGetterAsyncTask(fragment, context, weatherImage);
        task.execute(countryCode, city, country);
    }

}