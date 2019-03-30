package edu.buffalo.cse.cse486586.simpledht;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if(selection.equals(Constants.GLOBAL_INDICATOR)){

            //TODO: delete DHT data

        } else if(selection.equals(Constants.LOCAL_INDICATOR)){
            editor.clear();

        } else{
            editor.remove(selection);
        }

        editor.commit();

        Log.v("removed", selection);

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(values.getAsString(Constants.KEY_FIELD), values.getAsString(Constants.VALUE_FIELD));
        editor.commit();

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        Log.v("query", selection);

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);

        HashMap<String,String> hm = new HashMap<String,String>();

        if(selection.equals(Constants.GLOBAL_INDICATOR)){

            //TODO: get DHT data

        } else if(selection.equals(Constants.LOCAL_INDICATOR)){

            Map<String,?> keys = sharedPref.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){

                hm.put(entry.getKey(),entry.getValue().toString());

                //Log.d("map values",entry.getKey() + ": " + entry.getValue().toString());
            }

        } else {

            hm.put(selection, sharedPref.getString(selection, null));


        }

        MatrixCursor cursor = new MatrixCursor(
                new String[] {Constants.KEY_FIELD, Constants.VALUE_FIELD}
        );

        for (Map.Entry<String, String> entry : hm.entrySet()) {

            cursor.newRow()
                    .add(Constants.KEY_FIELD, entry.getKey())
                    .add(Constants.VALUE_FIELD, entry.getValue());
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
