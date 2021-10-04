package com.app.diseasemap;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SheetReader.SheetEvent {

    //googleSheet
    final String API_KEY = "AIzaSyDE231ta5ozKj75Y-EW1cR7Us07HrHRtng";
    final String SPREADSHEET_ID = "1haTOjjWN8vg-6W5CWFn8f3nTEBH581CS1zIk4KfJ9QE";
    final List<String> cityList = new ArrayList<>(Arrays.asList(
            "臺北市", "臺中市", "臺南市", "高雄市", "基隆市", "新竹市", "嘉義市", "新北市", "桃園市", "新竹縣", "宜蘭縣",
            "苗栗縣", "彰化縣", "南投縣", "雲林縣", "嘉義縣", "屏東縣", "澎湖縣", "花蓮縣", "臺東縣", "金門縣", "連江縣"));
    SheetReader sheet;

    //google map
    private GoogleMap mMap;

    //color layout
    private GeoJsonLayer layer;//市區 區域
    private int[] colorValue = {0, 1, 2, 3};//區域顏色設定
    private Map<Integer, Marker> mkList = new HashMap<>();

    //element
    private SeekBar dateSeekBar;
    private TextView dateView;

    //data
    private LinkedHashMap<String, List<Integer>> sortedData;
    private String[] days;
    private String lastDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sheet = new SheetReader(SPREADSHEET_ID, API_KEY, "本土病例及境外移入病例(單日新增)");

        sheet.setListener(this);

        //取得資料
        sheet.startRead();

        dateView = findViewById(R.id.date_text);
        dateSeekBar = findViewById(R.id.time_line);
    }


    private CountDownLatch mapReady = new CountDownLatch(1);

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

        mapReady.countDown();
    }

    @Override
    public void onSheetRead(LinkedHashMap<String, List<Integer>> sortedData) {
        this.sortedData = sortedData;
        days = sortedData.keySet().toArray(new String[0]);
        try {
            mapReady.await();

            runOnUiThread(() -> {
                setUpLegend();//圖例
                updateColor(days[days.length - 1], true);//畫顏色

                //時間軸
                dateSeekBar.setMax(days.length - 1);
                dateSeekBar.setProgress(days.length - 1);
                dateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        updateColor(days[progress], false);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        dateView.setText(date);

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

                style.setPolygonStrokeWidth(2);//界線寬度
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

                style.setPolygonStrokeWidth(2);//界線寬度
                feature.setPolygonStyle(style);
            }
        }
        lastDate = date;
    }
}
