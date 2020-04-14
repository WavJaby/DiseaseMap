package com.app.diseasemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.renderscript.Allocation;
import androidx.annotation.DrawableRes;
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
import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Random;

import static com.app.diseasemap.GoogleSheetHelper.getData;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //googleSheet
    private static String SPREADSHEET_ID = "1haTOjjWN8vg-6W5CWFn8f3nTEBH581CS1zIk4KfJ9QE";

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        for (List<Object> i : getData(SPREADSHEET_ID, "本土病例及境外移入病例" + "(總)" + "!A2:A")) {
            System.out.println(i.get(0).toString().replace("01~", ""));
        }
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


//        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.tw_map, getApplicationContext());

            for (GeoJsonFeature feature : layer.getFeatures()) {
                GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();//本來的style
                style.setFillColor(Color.argb(80, 0, new Random().nextInt(255), new Random().nextInt(255)));//改顏色
                style.setPolygonStrokeWidth(4);//界線寬度
                feature.setPolygonStyle(style);
            }
            layer.addLayerToMap();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(bitmapDescriptorFromVector(this, R.mipmap.ic_launcher))
                .anchor(0.5f, 0.5f)
                .position(sydney, 1000, 1000)
                .transparency(0.5f));
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
