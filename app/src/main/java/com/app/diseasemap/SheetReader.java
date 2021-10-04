package com.app.diseasemap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class SheetReader extends Thread {
    private final String SPREADSHEET_ID;
    private final String API_KEY;
    private final String page;
    private SheetEvent event;

    private final LinkedHashMap<String, List<Integer>> sortedData = new LinkedHashMap<>();

    public SheetReader(String spreadsheetID, String apiKey, String page) {
        this.SPREADSHEET_ID = spreadsheetID;
        this.API_KEY = apiKey;
        this.page = page;
    }

    public void startRead() {
        new Thread(this).start();
    }

    public void setListener(SheetEvent event) {
        this.event = event;
    }

    public void run() {
        String url = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + page + "!A:X?key=" + API_KEY;
        try {
            InputStream in = new URL(url).openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int length;
            byte[] buff = new byte[1024];
            while ((length = in.read(buff)) > 0) {
                out.write(buff, 0, length);
            }
            JSONObject mapData = new JSONObject(out.toString("UTF-8"));//把string變成Json
            JSONArray row = mapData.getJSONArray("values"); //取得JSONArray

            for (int i = 1; i < row.length(); i++) {
                JSONArray jsonY = row.getJSONArray(i);
                List<Integer> values = new ArrayList<>();
                if (jsonY.length() > 1) {//有資料嗎?
                    for (int j = 1; j < jsonY.length(); j++) {//得到每一欄
                        values.add(Integer.parseInt(jsonY.get(j).toString()));
                    }
                    //date, data
                    sortedData.put(jsonY.get(0).toString(), values);
                }
            }
            event.onSheetRead(sortedData);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public interface SheetEvent {
        void onSheetRead(LinkedHashMap<String, List<Integer>> sortedData);
    }
}