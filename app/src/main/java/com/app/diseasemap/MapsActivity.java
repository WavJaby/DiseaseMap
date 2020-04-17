package com.app.diseasemap;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.method.SingleLineTransformationMethod;
import android.widget.SeekBar;
import android.widget.TextView;
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
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
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
    private Map<String, List<Integer>> sortedData = new HashMap<>();
    private List<String> day = new ArrayList<>();
    private GeoJsonLayer layer;//市區 區域
    private int[] colorValue = {0, 1, 2, 3};//區域顏色設定
    private Map<Integer, Marker> mkList = new HashMap<>();

    private SeekBar dateSeekBar;
    private TextView dateView;

    private String lastDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        new readSheet("本土病例及境外移入病例(單日新增)").start();//read sheet

        dateView = findViewById(R.id.date_text);
        dateSeekBar = findViewById(R.id.time_line);
        dateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor(day.get(progress), false);
                dateView.setText(day.get(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                int maxVal = day.size() - 1;
                seekBar.setMax(maxVal);
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

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMaxZoomPreference(10);
        mMap.setMyLocationEnabled(true);

        try {
            layer = new GeoJsonLayer(mMap, R.raw.tw_map_sim, getApplicationContext());
            layer.addLayerToMap();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Add a Sydney and move the camera
        LatLng myLocation = layer.getBoundingBox().getCenter();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 7));

        while (sortedData.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd");
        Date date = new Date();//現在
        date.setDate(date.getDate() - 1);//昨天
        updateColor(fmt.format(date), true);//畫顏色
        setUpLegend();//圖例

        dateView.setText(fmt.format(date));
        dateSeekBar.setProgress(day.size());

    }

    private void setUpLegend() {
        ((TextView) findViewById(R.id.low_level)).setText((colorValue[0] + 1) + "~" + colorValue[1]);
        ((TextView) findViewById(R.id.middle_level)).setText((colorValue[1] + 1) + "~" + colorValue[2]);
        ((TextView) findViewById(R.id.high_level)).setText((colorValue[2] + 1) + "~" + colorValue[3]);
        ((TextView) findViewById(R.id.max_level)).setText((colorValue[3] + 1) + ">");
    }

    private void updateColor(String date, boolean first) {
        List<Integer> ctValues = sortedData.get(date);
        List<Integer> lstCtValues = sortedData.get(lastDate);

        for (GeoJsonFeature feature : layer.getFeatures()) {
            GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();//本來的style
            int index = cityList.indexOf(feature.getProperty("名稱"));
            int value = ctValues.get(index);

            LatLng sydney = feature.getBoundingBox().getCenter();

            if (first) {
                mkList.put(index, mMap.addMarker(new MarkerOptions().position(sydney)));
                mkList.get(index).setTitle(feature.getProperty("名稱") + "確診: " + value);
//                mkList.set(index, mMap.addMarker(new MarkerOptions().position(sydney).title(feature.getProperty("名稱") + "確診: " + value)));

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

            } else if (lstCtValues.get(index) != value) {
                mkList.get(index).setTitle(feature.getProperty("名稱") + "確診: " + value);

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
        lastDate = date;
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

                for (int i = 1; i < row.length(); i++) {
                    JSONArray jsonY = row.getJSONArray(i);
                    List<Integer> values = new ArrayList<>();
                    if (jsonY.length() > 1) {//有資料嗎?
                        for (int j = 1; j < jsonY.length(); j++) {//得到每一欄
                            values.add(Integer.parseInt(jsonY.get(j).toString()));
                        }
                        day.add(row.getJSONArray(i).get(0).toString());
                        sortedData.put(row.getJSONArray(i).get(0).toString(), values);
                    }
                }
                System.out.println(day);
//                for (Map.Entry<String, List<Integer>> i : sortedData.entrySet()) {
//                    System.out.println(i);
//                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
