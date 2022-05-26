package ugr.gbv.cognimobile.database;

import static ugr.gbv.cognimobile.database.Provider.CONTENT_URI_TESTS;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import ugr.gbv.cognimobile.callbacks.LoginCallback;
import ugr.gbv.cognimobile.dto.TestDTO;
import ugr.gbv.cognimobile.payload.request.LoginRequest;
import ugr.gbv.cognimobile.utilities.ErrorHandler;

public class ContentProvider implements Serializable {

    private static volatile ContentProvider instance;


    private ContentProvider(){

        if (instance != null){
            throw new RuntimeException("Use .getInstance() to invoke ContentProvider");
        }
    }

    public static ContentProvider getInstance() {
        if (instance == null) {
            synchronized (ContentProvider.class) {
                if (instance == null) instance = new ContentProvider();
            }
        }

        return instance;
    }

//    public Cursor getRopa(ClothsFragment clothsFragment) {
//        ContentResolver contentResolver = clothsFragment.getContext().getContentResolver();
//        Uri articleQueryUri = RopaContract.RopaEntry.CONTENT_URI;
//        Cursor cursor = contentResolver.query(
//                articleQueryUri,
//                null,
//                null,
//                null,
//                null);
//        cursor.setNotificationUri(contentResolver, articleQueryUri);
//        return cursor;
//    }

    public void doLogin(Context context, LoginRequest credentials, LoginCallback loginCallback){
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                CognimobilePreferences.getServerUrl(context) + "/api/auth/signin",
                response -> {
                    try {
                         CognimobilePreferences.setLogin(context,response);
                         loginCallback.loginStored();
                    } catch (Exception e) {
                        ErrorHandler.displayError("Something happened when loading the tests into the database");
                    }
                },
                error -> {
                    //displaying the error in toast if occur
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return credentials == null ? null : objectMapper.writeValueAsBytes(credentials);

                } catch (JsonProcessingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", credentials, "utf-8");
                    return null;
                }

            }
        };

        //creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        //adding the string request to request queue
        requestQueue.add(stringRequest);

    }

    public void getTests(Context context) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                CognimobilePreferences.getServerUrl(context),
                response -> {
                    try {
                        //getting the whole json object from the response
                        JSONArray obj = new JSONArray(response);
                        ObjectMapper mapper = new ObjectMapper();
                        ContentValues[] contentValues = new ContentValues[obj.length()];
                        for(int i = 0; i < obj.length(); ++i){
                            TestDTO test = mapper.readValue(obj.get(i).toString(),TestDTO.class);
                            ContentValues ropaValue = new ContentValues();
                            ropaValue.put(Provider.Cognimobile_Data._ID, test.getId());
                            ropaValue.put(Provider.Cognimobile_Data.DATA, test.toString());
                            ropaValue.put(Provider.Cognimobile_Data.NAME, test.getName());
                            contentValues[i] = ropaValue;
                        }

                        ContentResolver contentResolver = context.getContentResolver();

                        contentResolver.delete(
                                CONTENT_URI_TESTS,
                                null,
                                null
                        );

                        contentResolver.bulkInsert(
                                CONTENT_URI_TESTS,
                                contentValues
                        );

                    } catch (Exception e) {
                        ErrorHandler.displayError("Something happened when loading the tests into the database");
                    }
                },
                error -> {
                    //displaying the error in toast if occur
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        //creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        //adding the string request to request queue
        requestQueue.add(stringRequest);
    }

}

