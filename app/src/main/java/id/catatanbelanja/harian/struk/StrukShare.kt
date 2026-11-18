package id.catatanbelanja.harian.struk

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object StrukShare {

    private fun namaFile(tanggal: Long, namaBuku: String): String {
        val buku = namaBuku.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "Belanja" }
        return "${buku}_${LocalDate.ofEpochDay(tanggal)}.png"
    }

    /** Simpan PNG ke cache aplikasi lalu buka menu Share Android (WhatsApp, Telegram, dll). */
    fun bagikan(context: Context, bitmap: Bitmap, tanggal: Long, namaBuku: String, teks: String) {
        val folder = File(context.cacheDir, "struk").apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() } // bersihkan struk lama
        val file = File(folder, namaFile(tanggal, namaBuku))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val kirim = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, teks)
            clipData = ClipData.newRawUri("struk", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(kirim, "Bagikan struk lewat…")
        if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /** Android 8–9 butuh izin tulis penyimpanan; Android 10+ tidak. */
    val perluIzinTulis: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Simpan PNG ke Galeri (folder Pictures/Catatan Belanja). */
    fun simpanKeGaleri(context: Context, bitmap: Bitmap, tanggal: Long, namaBuku: String): Boolean = runCatching {
        val nama = namaFile(tanggal, namaBuku).removeSuffix(".png") + "_" + System.currentTimeMillis() % 100000 + ".png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, nama)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Catatan Belanja")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Gagal membuat file")
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: error("Gagal menulis file")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Catatan Belanja")
            folder.mkdirs()
            val file = File(folder, nama)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        }
        true
    }.getOrDefault(false)
}
