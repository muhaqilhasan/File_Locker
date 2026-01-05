# Secure File Locker (Android Client)

**Secure File Locker** adalah aplikasi Android yang dirancang untuk mengamankan file (Gambar, PDF, Dokumen) menggunakan metode **Kriptografi Hibrid** sebelum dikirim ke server. Aplikasi ini menerapkan *Client-Side Encryption* untuk memastikan privasi dan integritas data pengguna.

---

## 🚀 Fitur Utama

### 1. Kriptografi Hibrid (Hybrid Encryption)
Aplikasi ini menggabungkan kecepatan dan keamanan dari dua algoritma:
* **AES-256 (Simetris):** Digunakan untuk mengenkripsi konten file utama agar proses cepat.
* **RSA-2048 (Asimetris):** Digunakan untuk mengenkripsi Kunci AES (Session Key). Hasil enkripsi kunci ini dikirim bersama file sebagai *Enveloped Data*.

### 2. Manajemen Kunci (Key Management)
* **Generate Keypair:** Membangkitkan pasangan kunci Public & Private RSA secara lokal di perangkat.
* **Local History:** Riwayat kunci disimpan dalam database lokal (**SQLite**) sehingga kunci tidak hilang saat aplikasi ditutup.
* **Copy Private Key:** Fitur untuk menyalin Private Key guna keperluan dekripsi di sisi Server/Backend.

### 3. Secure Upload
* Mendukung pengiriman file terenkripsi ke server lokal (XAMPP/Artisan) maupun server online (Hosting/CWP).
* Menggunakan protokol HTTP/HTTPS POST.

### 4. Modern UI
* Dibangun dengan **Material Design**.
* Antarmuka bersih, responsif, dan dilengkapi Splash Screen.

---

## 🛠️ Teknologi yang Digunakan

* **Bahasa:** Java (Native Android)
* **Minimum SDK:** Android 7.0 (API 24)
* **Target SDK:** Android 14 (API 34)
* **Library Utama:**
    * `OkHttp3` (Networking/Upload)
    * `Java.Security` & `Javax.Crypto` (Core Encryption)
    * Material Design Components

---

## 🔐 Alur Logika Enkripsi

Berikut adalah langkah-langkah sistem saat tombol **"Encrypt & Upload"** ditekan:

1.  **Generate AES Key:** Aplikasi membuat kunci acak AES 256-bit (*Session Key*).
2.  **Encrypt File:** File asli dibaca menjadi *byte array* dan dienkripsi menggunakan AES Key tersebut.
3.  **Encrypt Key:** AES Key tadi dienkripsi menggunakan **RSA Public Key** tujuan.
4.  **Packaging:** Aplikasi menggabungkan hasil enkripsi ke dalam satu paket *byte array* dengan format:
    ```
    [4 Byte Panjang Header] + [Encrypted AES Key] + [Encrypted File Body]
    ```
5.  **Upload:** Paket data biner tersebut dikirim ke API Server.

---

## 📸 Screenshots

| Splash Screen | Dashboard | Key History |
|:---:|:---:|:---:|
| <img src="path/to/screenshot1.png" width="200"> | <img src="path/to/screenshot2.png" width="200"> | <img src="path/to/screenshot3.png" width="200"> |

*> Catatan: Ganti `path/to/screenshot` dengan lokasi gambar yang sebenarnya di folder repositori Anda.*

---

## 💻 Cara Menjalankan (Installation)

### Prasyarat
* Android Studio Ladybug / Koala (atau terbaru)
* JDK 17 atau lebih baru
* Koneksi Internet (untuk Gradle Sync)

### Langkah-langkah
1.  **Clone Repository**
    ```bash
    git clone [https://github.com/username-anda/nama-repo-android.git](https://github.com/username-anda/nama-repo-android.git)
    ```
2.  **Buka di Android Studio**
    * Pilih `File > Open`.
    * Arahkan ke folder hasil clone.
3.  **Sync Gradle**
    * Tunggu hingga Android Studio selesai mengunduh semua *dependency*.
4.  **Konfigurasi Server**
    * Buka `MainActivity.java` (atau file konfigurasi API Anda).
    * Sesuaikan URL endpoint server (Localhost/Live) jika diperlukan.
5.  **Run**
    * Sambungkan HP Android (USB Debugging On) atau gunakan Emulator.
    * Klik tombol **Run (Play)**.

---

## ⚠️ Disclaimer

Aplikasi ini dibuat sebagai bagian dari **Tugas Akhir / UAS Mata Kuliah Keamanan Informasi dan Jaringan**.
* Aplikasi menggunakan standar industri (RSA/AES), namun implementasi ini ditujukan utama untuk **tujuan edukasi**.
* **Penting:** Simpan Private Key Anda dengan aman. Tanpa Private Key, file yang sudah dienkripsi tidak dapat dikembalikan/didekripsi.

---

## 👨‍💻 Author

* **Nama:** [Nama Lengkap Anda]
* **NIM:** [NIM Anda]
* **Kampus:** [Nama Kampus Anda]

Dibuat dengan ❤️ dan Kopi.
