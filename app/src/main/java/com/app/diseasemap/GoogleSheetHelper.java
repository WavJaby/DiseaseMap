//package com.app.diseasemap;
//
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
//import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.client.util.store.FileDataStoreFactory;
//import com.google.api.services.sheets.v4.Sheets;
//import com.google.api.services.sheets.v4.SheetsScopes;
//import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
//import com.google.api.services.sheets.v4.model.ValueRange;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.security.GeneralSecurityException;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//public class GoogleSheetHelper {
//
//    private static String APPLICATION_NAME = "GoogleSheetHelper";
//
//    private static Credential authorize() throws IOException, GeneralSecurityException {
//        InputStream in = GoogleSheetHelper.class.getResourceAsStream("client.json");
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
//                JacksonFactory.getDefaultInstance(), new InputStreamReader(in));
//
//        List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
//                clientSecrets, scopes)
//                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
//                .setAccessType("offline")
//                .build();
//        Credential credential = new AuthorizationCodeInstalledApp(
//                flow, new LocalServerReceiver())
//                .authorize("user");
//        return credential;
//    }
//
//    public static Sheets getSheetsService() {
//        try {
//            Credential credential = authorize();
//            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
//                    .setApplicationName(APPLICATION_NAME)
//                    .build();
//        } catch (GeneralSecurityException | IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//
//    public static List<List<Object>> getData(String sheetID, String range) {
//
//        ValueRange response = null;
//        try {
//            response = getSheetsService().spreadsheets().values()
//                    .get(sheetID, range)
//                    .execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        if (values == null || values.isEmpty())
////            return null;
////        else
//        return response.getValues();
//    }
//
//    public static void writeData(String sheetID, String range, List<Object> data) {
//        List<List<Object>> towDData = Arrays.asList(data);//轉成list of list [y[x]]
//
////        for(String i :data){
////            data1.add(Arrays.asList(i));
////        }
//
//        ValueRange body = new ValueRange()
//                .setValues(towDData);
//
//        try {
//            UpdateValuesResponse response = getSheetsService().spreadsheets().values()
//                    .update(sheetID, range, body)
//                    .setValueInputOption("USER_ENTERED")
//                    .execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
