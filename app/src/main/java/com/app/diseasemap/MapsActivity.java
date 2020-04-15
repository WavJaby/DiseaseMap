package com.app.diseasemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.JsonReader;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //googleSheet
    final String API_KEY = "AIzaSyDE231ta5ozKj75Y-EW1cR7Us07HrHRtng";
    final String SPREADSHEET_ID = "1haTOjjWN8vg-6W5CWFn8f3nTEBH581CS1zIk4KfJ9QE";

    private GoogleMap mMap;

    private JSONObject mapData;
    private GeoJsonLayer layer;//市區 區域


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        new readSheet("本土病例及境外移入病例(單日新增)").start();//read sheet

//                updateColor(layer);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("ResourceType")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(25.05, 121.54);
        mMap.addMarker(new MarkerOptions().position(sydney).title("this"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        try {
            layer = new GeoJsonLayer(mMap, R.raw.tw_map_sim, getApplicationContext());
            layer.addLayerToMap();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    private void updateColor(GeoJsonLayer layer) {
        for (GeoJsonFeature feature : layer.getFeatures()) {

            GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();//本來的style
            style.setFillColor(Color.argb(80, 0, new Random().nextInt(255), new Random().nextInt(255)));//改顏色
            style.setPolygonStrokeWidth(4);//界線寬度
            feature.setPolygonStyle(style);
        }
    }

    class readSheet extends Thread {

        private String page;

        public readSheet(String page) {
            this.page = page;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void run() {
            String url = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + page + "!A:X?key=" + API_KEY;
            try {
                InputStream openUrl = new URL(url).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(openUrl, Charset.forName("UTF-8")));

                StringBuilder string = new StringBuilder();//可以append的string(比+=快速的方法)
                int text;//一個一個讀取text
                while ((text = reader.read()) != -1) {
                    string.append((char) text);
                }
                mapData = new JSONObject(string.toString());
                JSONArray ja = mapData.getJSONArray("values"); // get the JSONArray
                List<String> keys = new ArrayList<>();

                for (int i = 0; i < ja.length(); i++) {
//                    JSONArray ja2 = new JSONArray(ja.get(i));
                    System.out.printf("%d,%s\n",i,ja.get(i));
//                    System.out.println(ja2.get(0));
                }

                System.out.println(mapData);
                System.out.println(keys);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
