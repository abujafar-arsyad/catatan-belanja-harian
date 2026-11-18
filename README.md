# Catatan Belanja Harian

Aplikasi Android sederhana untuk mencatat belanja harian ibu rumah tangga: nama barang, jumlah, harga,
total otomatis, lalu **membagikan struk belanja sebagai gambar PNG** (misalnya ke WhatsApp suami).

- Bahasa Indonesia, mata uang Rupiah (format `Rp 12.500`)
- Offline, tanpa login, data tersimpan di HP (Room database)
- Kotlin + Jetpack Compose + Material 3, Android 8.0 (API 26) ke atas

## Fitur

| Halaman | Isi |
|---|---|
| **Hari Ini** | Tanggal hari ini, tombol besar *Tambah Barang*, daftar barang (nama, jumlah + satuan, harga satuan, subtotal), TOTAL besar di bawah, tombol *Bagikan Struk*. Ketuk barang = ubah; tekan lama = pilihan Ubah/Hapus; ikon tempat sampah = hapus (selalu dengan konfirmasi). |
| **Form Barang** | Nama (wajib) dengan saran otomatis dari barang yang pernah dibeli + harga terakhirnya, Jumlah (bawaan 1, boleh desimal `0,5`), Satuan (pcs, kg, gram, liter, bungkus, ikat, buah, lainnya), Harga satuan (wajib, keyboard angka), Kategori & Toko (opsional). Subtotal dihitung otomatis. Tombol *Simpan + Lagi* untuk mencatat beruntun. |
| **Riwayat** | Total minggu ini & bulan ini, daftar per hari / per minggu / per bulan. Ketuk tanggal → detail (bisa diubah & dibagikan). Pencarian harga barang (terakhir, termurah, termahal). |
| **Bagikan Struk** | Gambar PNG lebar 1080 px, tinggi menyesuaikan jumlah barang. Isi: judul, hari & tanggal, nama pencatat, tabel No/Nama/Jumlah/Harga/Subtotal, TOTAL tebal, jumlah item, catatan opsional. Bisa dibagikan (WhatsApp, Telegram, dll.) atau *Simpan ke Galeri* (folder `Pictures/Catatan Belanja`). |
| **Pengaturan** | Nama pencatat, tema (ikut HP / terang / gelap), Backup JSON, Ekspor CSV (Excel), Impor JSON/CSV (gabungkan atau ganti semua). |

## Struktur kode

```
app/src/main/java/id/catatanbelanja/harian/
├── BelanjaApp.kt            # Application: menyiapkan database & pengaturan
├── MainActivity.kt          # Navigasi + menu bawah (Hari Ini, Riwayat, Pengaturan)
├── data/
│   ├── Item.kt              # Tabel barang: id, tanggal, nama, jumlah, satuan, harga, kategori, toko, catatan
│   ├── ItemDao.kt           # Perintah database (simpan, hapus, total per hari, saran, cari)
│   ├── AppDatabase.kt       # Room database "belanja.db"
│   └── Settings.kt          # Nama pencatat & tema (SharedPreferences)
├── struk/
│   ├── StrukRenderer.kt     # Menggambar struk ke Canvas → Bitmap PNG
│   └── StrukShare.kt        # FileProvider + Intent.ACTION_SEND, simpan ke Galeri (MediaStore)
├── backup/Backup.kt         # Ekspor/impor JSON & CSV
├── ui/theme/Theme.kt        # Warna hijau lembut + oranye, huruf besar, mode gelap
├── ui/screens/              # Semua layar Compose
└── util/Format.kt           # Format Rupiah & tanggal Indonesia
```

## Cara build APK

### Cara 1 — Android Studio (paling mudah)
1. Buka Android Studio → **File › Open** → pilih folder `CatatanBelanjaHarian`.
2. Tunggu proses *Gradle Sync* selesai (pertama kali butuh internet untuk mengunduh library).
3. Menu **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
4. Klik *locate* pada notifikasi → file ada di `app/build/outputs/apk/debug/app-debug.apk`.
5. Kirim file APK ke HP (WhatsApp/kabel USB), buka, izinkan "instal dari sumber tidak dikenal".

### Cara 2 — Command line
Butuh JDK 17+ dan Android SDK (file `local.properties` berisi `sdk.dir=...`, otomatis dibuat Android Studio).

```bash
# Windows
gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

Untuk APK yang lebih kecil: `gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
(saat ini ditandatangani dengan kunci debug agar langsung bisa dipasang; untuk Play Store, buat keystore sendiri).

## Format backup JSON

```json
{
  "format": "CatatanBelanjaHarian",
  "versi": 1,
  "namaPencatat": "Ummu ...",
  "items": [
    { "tanggal": "2026-11-18", "nama": "Cabai rawit", "jumlah": 0.25, "satuan": "kg",
      "harga": 60000, "kategori": "Bumbu", "toko": "Pasar Pagi", "catatan": null }
  ]
}
```
