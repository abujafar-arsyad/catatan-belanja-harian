# Catatan Belanja Harian

Aplikasi Android sederhana untuk mencatat belanja harian ibu rumah tangga: nama barang, jumlah, harga,
total otomatis, lalu **membagikan struk belanja sebagai gambar PNG** (misalnya ke WhatsApp suami).

- Bahasa Indonesia, mata uang Rupiah (format `Rp 12.500`)
- Offline, tanpa login, data tersimpan di HP (Room database)
- Kotlin + Jetpack Compose + Material 3, Android 8.0 (API 26) ke atas

**Unduh APK:** lihat halaman [Releases](../../releases).

## Fitur

| Halaman | Isi |
|---|---|
| **Buku Belanja** | Pisahkan catatan agar tidak tercampur, misalnya *Belanja Rumah*, *Belanja Kantor*, *Belanja Ibu Ana*, *Belanja Pak Yanto*. Pilih buku lewat tombol "Ganti" di Hari Ini / Riwayat. Total, riwayat, ringkasan, dan struk dihitung per buku; nama buku tercetak di struk. Kelola (tambah, ubah, hapus) di Pengaturan. |
| **Uang ditalangi** | Buku bisa ditandai *"Uangnya ditalangi dulu"* (belanja kantor / titipan orang). Setiap hari belanja punya status **Sudah diganti / Belum diganti**; Riwayat menampilkan total yang belum diganti; status ikut tercetak di struk dan laporan. |
| **Hari Ini** | Tanggal hari ini, tombol besar *Tambah Barang*, daftar barang (nama, jumlah + satuan, harga satuan, subtotal), TOTAL besar di bawah, tombol *Bagikan Struk*. Ketuk barang = ubah; tekan lama = pilihan Ubah/Hapus; ikon tempat sampah = hapus (selalu dengan konfirmasi). |
| **Form Barang** | Tanggal belanja bisa diubah (untuk mencatat belanja kemarin yang terlupa; kalender berbahasa Indonesia, tanggal yang akan datang tidak bisa dipilih). Nama (wajib) dengan saran otomatis + harga terakhir, Jumlah (boleh desimal `0,5`), Satuan, Harga satuan, Kategori & Toko (opsional). Subtotal otomatis. Tombol *Simpan + Lagi* untuk mencatat beruntun. |
| **Riwayat** | Total minggu ini & bulan ini, daftar per hari / per minggu / per bulan. Ketuk tanggal → detail (bisa diubah & dibagikan). Pencarian harga barang (terakhir, termurah, termahal). |
| **Laporan PDF** | Dari tab *Per Minggu* / *Per Bulan*: laporan A4 berisi ringkasan, total per hari, rincian setiap barang (opsional), status sudah/belum diganti, dan TOTAL. Halaman otomatis bertambah bila barangnya banyak. Bisa dibagikan (WhatsApp, dll.) atau disimpan ke folder Download — cocok untuk diserahkan ke kantor. |
| **Bagikan Struk** | Gambar PNG lebar 1080 px, tinggi menyesuaikan jumlah barang. Isi: judul, nama buku, hari & tanggal, nama pencatat, tabel No/Nama/Jumlah/Harga/Subtotal, TOTAL tebal, jumlah item, status (untuk buku talangan), catatan opsional. Bisa dibagikan atau *Simpan ke Galeri*. |
| **Pengaturan** | Nama pencatat, kelola buku belanja, warna tema (Ungu — utama, Hijau, Biru), terang / gelap / ikut HP, **Kirim Backup** (WhatsApp/Drive) atau **Simpan Backup ke Download**, Ekspor CSV (Excel), Impor JSON/CSV (gabungkan atau ganti semua). |

## Pindah dari versi 1.0 ke 1.1

Mulai versi 1.1 aplikasi ditandatangani dengan kunci rilis resmi, jadi **v1.1 tidak bisa dipasang langsung
di atas v1.0**. Agar data tidak hilang:

1. Di v1.0: **Pengaturan → Backup Data (JSON)**. Pada layar penyimpanan, pilih penyimpanan internal
   (nama HP) → folder **Documents**, lalu *Simpan*. (Hindari pintasan "Downloads" di layar itu — di
   sebagian HP file backup jadi kosong.)
2. Hapus (uninstall) aplikasi versi 1.0.
3. Pasang v1.1.
4. Di v1.1: **Pengaturan → Impor Data** → pilih file backup tadi → **Ganti Semua**.

Update berikutnya (1.1 → 1.2 dst.) bisa langsung dipasang tanpa langkah di atas.

## Struktur kode

```
app/src/main/java/id/catatanbelanja/harian/
├── BelanjaApp.kt            # Application: menyiapkan database & pengaturan
├── MainActivity.kt          # Navigasi + menu bawah (Hari Ini, Riwayat, Pengaturan)
├── data/
│   ├── Item.kt              # Tabel barang (tanggal, bukuId, nama, jumlah, satuan, harga, lunas, ...) + tabel Buku
│   ├── ItemDao.kt           # Perintah database (simpan, hapus, total per hari, saran, cari, laporan)
│   ├── AppDatabase.kt       # Room database "belanja.db" + migrasi v1→v2→v3
│   └── Settings.kt          # Nama pencatat, tema, warna, buku aktif (SharedPreferences)
├── struk/
│   ├── StrukRenderer.kt     # Menggambar struk ke Canvas → Bitmap PNG
│   ├── LaporanPdf.kt        # Laporan mingguan/bulanan → PDF A4 (PdfDocument), banyak halaman
│   └── StrukShare.kt        # FileProvider + Intent.ACTION_SEND, simpan ke Galeri / Download (MediaStore)
├── backup/Backup.kt         # Ekspor/impor JSON & CSV
├── ui/theme/Theme.kt        # Tema Ungu (utama), Hijau, Biru; huruf besar; mode gelap
├── ui/screens/              # Semua layar Compose
└── util/Format.kt           # Format Rupiah & tanggal Indonesia
```

Skema database tiap versi tersimpan di `app/schemas/` dan diuji oleh `MigrasiTest`
(data versi 1 harus tetap utuh setelah di-update ke versi terbaru).

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

### APK rilis & kunci tanda tangan
`gradlew assembleRelease` menghasilkan APK kecil di `app/build/outputs/apk/release/app-release.apk`.

APK rilis resmi ditandatangani dengan kunci milik pembuat aplikasi. Kunci itu **tidak** ada di repository ini.
Untuk menandatangani dengan kunci sendiri, salin `keystore.properties.example` menjadi `keystore.properties`
lalu isi lokasi file `.jks` dan password-nya. Tanpa file itu, APK rilis memakai kunci debug (tetap bisa dipasang,
tapi tidak bisa meng-update APK rilis resmi).

> Simpan file kunci (`.jks`) dan password-nya baik-baik (misalnya juga di flashdisk / Google Drive pribadi).
> Jika hilang, aplikasi yang sudah terpasang di HP orang lain tidak bisa di-update lagi.

### Menjalankan test
```bash
gradlew.bat testDebugUnitTest
```
Menguji format Rupiah & tanggal, backup JSON/CSV (termasuk file backup versi lama), gambar struk PNG,
dan migrasi database.

## Format backup JSON (versi 3)

```json
{
  "format": "CatatanBelanjaHarian",
  "versi": 3,
  "namaPencatat": "Ummu ...",
  "buku": [
    { "nama": "Belanja Rumah", "talangan": false },
    { "nama": "Belanja Kantor", "talangan": true }
  ],
  "items": [
    { "tanggal": "2026-11-18", "buku": "Belanja Kantor", "nama": "Kertas HVS A4", "jumlah": 1, "satuan": "pcs",
      "harga": 55000, "kategori": null, "toko": null, "catatan": null, "lunas": true }
  ]
}
```
Backup versi 1 dan 2 tetap bisa diimpor.
