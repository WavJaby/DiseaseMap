package com.app.diseasemap;

import android.annotation.SuppressLint;
import android.os.Build;
import android.widget.SeekBar;
import androidx.annotation.RequiresApi;
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
    final List<String> cityList = new ArrayList<>(Arrays.asList(
            "臺北市", "臺中市", "臺南市", "高雄市", "基隆市", "新竹市", "嘉義市", "新北市", "桃園市", "新竹縣", "宜蘭縣",
            "苗栗縣", "彰化縣", "南投縣", "雲林縣", "嘉義縣", "屏東縣", "澎湖縣", "花蓮縣", "臺東縣", "金門縣", "連江縣"));

    //google map
    private GoogleMap mMap;

    //color layout
    private Map<String, List<Integer>> sortedData;
    private GeoJsonLayer layer;//市區 區域
    private int[] colorValue = {0,1,2,3};//區域顏色設定

    private SeekBar dateSeekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        new readSheet("本土病例及境外移入病例(單日新增)").start();//read sheet

        dateSeekBar = findViewById(R.id.time_line);
        dateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
        mMap.setMyLocationEnabled(true);

        try {
            layer = new GeoJsonLayer(mMap, R.raw.tw_map_sim, getApplicationContext());
            layer.addLayerToMap();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        while (sortedData.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        updateColor(layer);
    }

    private void updateColor(GeoJsonLayer layer) {
        List<Integer> ctValues = sortedData.get("3/17");
        for (GeoJsonFeature feature : layer.getFeatures()) {
            int index = cityList.indexOf(feature.getProperty("名稱"));
            int value = ctValues.get(index);
            GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();//本來的style
            if (value == colorValue[0])
                style.setFillColor(getResources().getColor(R.color.dark_green));//改顏色
            if (value > colorValue[0] && value <= colorValue[1])
                style.setFillColor(getResources().getColor(R.color.green));//改顏色
            if (value > colorValue[1] && value <= colorValue[2])
                style.setFillColor(getResources().getColor(R.color.yellow));//改顏色
            if (value > colorValue[2] && value <= colorValue[3])
                style.setFillColor(getResources().getColor(R.color.orange));//改顏色
            if (value > colorValue[3])
                style.setFillColor(getResources().getColor(R.color.red));//改顏色
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
                JSONObject mapData = new JSONObject(string.toString());//把string變成Json
                JSONArray row = mapData.getJSONArray("values"); //取得JSONArray
                sortedData = new HashMap<>();

                for (int i = 1; i < row.length(); i++) {
                    JSONArray jsonY = row.getJSONArray(i);
                    List<Integer> values = new ArrayList<>();
                    if (jsonY.length() > 1) {//有資料嗎?
                        for (int j = 1; j < jsonY.length(); j++) {//得到每一欄
                            values.add(Integer.parseInt(jsonY.get(j).toString()));
                        }
                        sortedData.put(row.getJSONArray(i).get(0).toString(), values);
                    }
                }

                for (Map.Entry<String, List<Integer>> i : sortedData.entrySet()) {
                    System.out.println(i);
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
