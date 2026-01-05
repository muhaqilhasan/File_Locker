package id.akaruuu.filelocker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Membuat layar Full Screen (Menghilangkan Status Bar atas)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        // Timer untuk pindah ke MainActivity setelah 3 detik (3000 ms)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Pindah ke MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Hapus SplashActivity dari stack agar tidak bisa di-back
            finish();
        }, 3000); // Ganti angka ini untuk durasi (3000 = 3 detik)
    }
}