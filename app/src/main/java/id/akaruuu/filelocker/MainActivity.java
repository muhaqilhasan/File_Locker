package id.akaruuu.filelocker;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.OpenableColumns; // Penting untuk ambil nama file asli
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvKeyStatus, tvSelectedFile, tvLog;
    private EditText etServerIp;
    private Button btnGenerateKeys, btnPickFile, btnEncryptUpload, btnScanServer, btnKeyHistory;
    private ImageButton btnCopyLog;
    private RadioGroup rgServerMode;
    private RadioButton rbArtisan, rbXampp, rbOnline;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Uri selectedFileUri;
    private byte[] fileData;

    // VARIABEL BARU: Menyimpan nama file asli (cth: foto.jpg)
    private String originalFileName = "unknown_file";

    private KeyDatabaseHelper dbHelper;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int UDP_PORT = 5001;
    private static final String BROADCAST_MSG = "CARI_SERVER_KIJ";
    private static final String RESPONSE_MSG = "SERVER_KIJ_ADA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        dbHelper = new KeyDatabaseHelper(this);

        // Init View
        tvKeyStatus = findViewById(R.id.tvKeyStatus);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        tvLog = findViewById(R.id.tvLog);
        etServerIp = findViewById(R.id.etServerIp);
        btnGenerateKeys = findViewById(R.id.btnGenerateKeys);
        btnPickFile = findViewById(R.id.btnPickFile);
        btnEncryptUpload = findViewById(R.id.btnEncryptUpload);
        btnScanServer = findViewById(R.id.btnScanServer);
        btnCopyLog = findViewById(R.id.btnCopyLog);
        btnKeyHistory = findViewById(R.id.btnKeyHistory);

        rgServerMode = findViewById(R.id.rgServerMode);
        rbArtisan = findViewById(R.id.rbArtisan);
        rbXampp = findViewById(R.id.rbXampp);
        rbOnline = findViewById(R.id.rbOnline);

        // Logic Radio Button
        rgServerMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOnline) {
                etServerIp.setText("aqildev.diskon.com/api");
                btnScanServer.setEnabled(false);
                btnScanServer.setAlpha(0.5f);
                log("Mode Online: Target Database Server");
            } else {
                etServerIp.setText("192.168.1.X:8000/api");
                btnScanServer.setEnabled(true);
                btnScanServer.setAlpha(1.0f);
            }
        });

        // Listeners
        btnGenerateKeys.setOnClickListener(v -> generateRSAKeys());
        btnKeyHistory.setOnClickListener(v -> showKeyHistoryDialog());
        btnScanServer.setOnClickListener(v -> scanLocalServer());

        btnCopyLog.setOnClickListener(v -> {
            String logText = tvLog.getText().toString();
            if (logText.isEmpty()) return;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("KIJ_Log", logText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Log disalin!", Toast.LENGTH_SHORT).show();
        });

        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        // Ambil nama file asli saat dipilih
                        getFileNameFromUri(selectedFileUri);
                        tvSelectedFile.setText("File: " + originalFileName);
                        readFileContent(selectedFileUri);
                    }
                });

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        btnEncryptUpload.setOnClickListener(v -> {
            if (publicKey == null || fileData == null) {
                Toast.makeText(this, "Key atau File belum siap!", Toast.LENGTH_SHORT).show();
                return;
            }
            performHybridEncryptionAndUpload();
        });
    }

    // --- HELPER BARU: AMBIL NAMA ASLI FILE ---
    private void getFileNameFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    originalFileName = cursor.getString(nameIndex);
                } else {
                    originalFileName = "file_" + System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            originalFileName = "unknown_" + System.currentTimeMillis();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ... (Fungsi History Dialog & Restore Key TETAP SAMA, tidak perlu diubah) ...
    private void showKeyHistoryDialog() {
        ArrayList<HashMap<String, String>> keyList = dbHelper.getAllKeys();
        if (keyList.isEmpty()) { Toast.makeText(this, "Belum ada riwayat.", Toast.LENGTH_SHORT).show(); return; }
        String[] displayItems = new String[keyList.size()];
        for (int i = 0; i < keyList.size(); i++) {
            String shortKey = keyList.get(i).get("pub_key").substring(0, 15) + "...";
            displayItems[i] = "📅 " + keyList.get(i).get("timestamp") + "\n🔑 ID: " + keyList.get(i).get("id") + " | " + shortKey;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Riwayat RSA Keys");
        builder.setItems(displayItems, (dialog, which) -> showActionDialog(keyList.get(which)));
        builder.setNegativeButton("Tutup", null);
        builder.setNeutralButton("Hapus Semua", (dialog, which) -> { dbHelper.clearHistory(); Toast.makeText(this, "Dihapus", Toast.LENGTH_SHORT).show(); });
        builder.show();
    }

    private void showActionDialog(HashMap<String, String> keyData) {
        String[] options = {"Gunakan Key Ini (Restore)", "Salin Private Key (Untuk Web)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Aksi (ID: " + keyData.get("id") + ")");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) restoreKeys(keyData);
            else copyPrivateKey(keyData.get("priv_key"));
        });
        builder.show();
    }

    private void restoreKeys(HashMap<String, String> keyData) {
        try {
            byte[] pubBytes = Base64.decode(keyData.get("pub_key"), Base64.DEFAULT);
            byte[] privBytes = Base64.decode(keyData.get("priv_key"), Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            tvKeyStatus.setText("KEY RESTORED (ID: " + keyData.get("id") + ") ✓");
            tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
            log("Menggunakan Kunci Lama (ID: " + keyData.get("id") + ")");
        } catch (Exception e) { log("Restore Error: " + e.getMessage()); }
    }

    private void copyPrivateKey(String privKey) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("RSA_Private_Key", privKey);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Key disalin! Paste di Web.", Toast.LENGTH_LONG).show();
    }

    private void generateRSAKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            String pubString = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
            String privString = Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
            dbHelper.saveKeyPair(pubString, privString);
            tvKeyStatus.setText("NEW KEYS GENERATED & SAVED ✓");
            tvKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            log("Keys Generated & Saved.");
        } catch (Exception e) { log("Key Error: " + e.getMessage()); }
    }

    private void scanLocalServer() {
        btnScanServer.setEnabled(false); btnScanServer.setText("Scanning..."); log("Scanning UDP...");
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(); socket.setBroadcast(true); socket.setSoTimeout(3000);
                byte[] sendData = BROADCAST_MSG.getBytes();
                socket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), UDP_PORT));
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String ip = receivePacket.getAddress().getHostAddress();
                if (msg.equals(RESPONSE_MSG)) {
                    runOnUiThread(() -> {
                        etServerIp.setText(rbArtisan.isChecked() ? ip + ":8000/api" : ip + "/kij-server/public/api");
                        log("Server Found: " + ip); Toast.makeText(this, "Found!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) { runOnUiThread(() -> log("Scan Timeout/Error.")); }
            finally { if(socket!=null) socket.close(); runOnUiThread(() -> {btnScanServer.setEnabled(true); btnScanServer.setText("AUTO SCAN");}); }
        }).start();
    }

    // --- ENKRIPSI & UPLOAD (UPDATED FILENAME) ---
    private void performHybridEncryptionAndUpload() {
        new Thread(() -> {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES"); keyGen.init(256);
                SecretKey aesKey = keyGen.generateKey();
                Cipher aesCipher = Cipher.getInstance("AES"); aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
                byte[] encryptedFile = aesCipher.doFinal(fileData);
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] encryptedSessionKey = rsaCipher.doFinal(aesKey.getEncoded());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.write(intToByteArray(encryptedSessionKey.length));
                bos.write(encryptedSessionKey);
                bos.write(encryptedFile);
                byte[] finalData = bos.toByteArray();

                File cacheFile = new File(getCacheDir(), "secure_upload.enc");
                FileOutputStream fos = new FileOutputStream(cacheFile);
                fos.write(finalData); fos.close();

                uploadToServer(cacheFile);
            } catch (Exception e) { runOnUiThread(() -> log("Encrypt Fail: " + e.getMessage())); }
        }).start();
    }

    private void uploadToServer(File file) {
        try {
            String ipInput = etServerIp.getText().toString().trim();
            if (ipInput.startsWith("http")) ipInput = ipInput.replace("http://", "").replace("https://", "");
            String url = "http://" + ipInput + "/upload";
            log("Uploading to: " + url);

            OkHttpClient client = new OkHttpClient();

            // LOGIKA PENAMAAN FILE (PENTING UNTUK EKSTENSI)
            // Format: Enc_Timestamp_NamaAsli.jpg.enc
            // Contoh: Enc_1734234_foto.jpg.enc
            String finalUploadName = "Enc_" + System.currentTimeMillis() + "_" + originalFileName + ".enc";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", finalUploadName,
                            RequestBody.create(file, MediaType.parse("application/octet-stream")))
                    .build();

            Request request = new Request.Builder().url(url).post(requestBody).build();
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();

            runOnUiThread(() -> {
                if (response.isSuccessful()) { log("SUKSES: " + resStr); Toast.makeText(this, "Upload Berhasil", Toast.LENGTH_LONG).show(); }
                else log("GAGAL (" + response.code() + "): " + resStr);
            });
        } catch (Exception e) { runOnUiThread(() -> log("Upload Error: " + e.getMessage())); }
    }

    private void readFileContent(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            fileData = bos.toByteArray();
            log("File loaded (" + fileData.length + " bytes)");
        } catch (Exception e) { log("Read Error: " + e.getMessage()); }
    }

    private byte[] intToByteArray(int value) {
        return new byte[] {(byte)(value >> 24), (byte)(value >> 16), (byte)(value >> 8), (byte)value};
    }

    private void log(String msg) {
        runOnUiThread(() -> tvLog.append("\n> " + msg));
        Log.d("KIJ_APP", msg);
    }
}