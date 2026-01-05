package id.akaruuu.filelocker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class KeyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "StartAppKeyHistory.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_KEYS = "keys";

    public KeyDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Buat tabel: ID, Waktu Dibuat, Public Key, Private Key
        String query = "CREATE TABLE " + TABLE_KEYS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp TEXT, " +
                "pub_key TEXT, " +
                "priv_key TEXT)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
        onCreate(db);
    }

    // Simpan Kunci Baru
    public void saveKeyPair(String pubKey, String privKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        values.put("timestamp", timeStamp);
        values.put("pub_key", pubKey);
        values.put("priv_key", privKey);

        db.insert(TABLE_KEYS, null, values);
        db.close();
    }

    // Ambil Semua Riwayat
    public ArrayList<HashMap<String, String>> getAllKeys() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<HashMap<String, String>> keyList = new ArrayList<>();

        // Urutkan dari yang terbaru (DESC)
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_KEYS + " ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", cursor.getString(0));
                map.put("timestamp", cursor.getString(1));
                map.put("pub_key", cursor.getString(2));
                map.put("priv_key", cursor.getString(3));
                keyList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return keyList;
    }

    // Hapus Riwayat (Opsional)
    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_KEYS);
        db.close();
    }
}