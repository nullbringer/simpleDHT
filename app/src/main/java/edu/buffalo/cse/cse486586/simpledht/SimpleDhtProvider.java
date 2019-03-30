package edu.buffalo.cse.cse486586.simpledht;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String PREFERENCE_FILE = "groupMsg2PreferenceFile";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.remove(selection);
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

        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(values.getAsString(KEY_FIELD), values.getAsString(VALUE_FIELD));
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

        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);

        String value = sharedPref.getString(selection, null);




        MatrixCursor cursor = new MatrixCursor(
                new String[] {KEY_FIELD, VALUE_FIELD}
        );

        cursor.newRow()
                .add(KEY_FIELD, selection)
                .add(VALUE_FIELD, value);




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
