package id.catatanbelanja.harian

import android.graphics.Bitmap
import android.graphics.Color
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.data.WarnaTema
import id.catatanbelanja.harian.struk.StrukRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Folder tempat menyimpan gambar hasil uji agar bisa dilihat manusia. */
private val folderHasil = File("build/hasil-uji").apply { mkdirs() }

private fun Bitmap.simpan(nama: String) =
    File(folderHasil, nama).outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }

/** Menguji gambar struk PNG (digambar sungguhan dengan grafis Android asli lewat Robolectric). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StrukDanLayarTest {

    private val tgl = LocalDate.of(2026, 11, 18).toEpochDay()

    @Test
    fun strukPng_lebar1080_dan_tinggiMengikutiJumlahBarang() {
        val sedikit = listOf(
            Item(tanggal = tgl, nama = "Cabai rawit merah", jumlah = 0.25, satuan = "kg", harga = 60000),
            Item(tanggal = tgl, nama = "Tahu putih", jumlah = 2.0, satuan = "bungkus", harga = 5500),
            Item(tanggal = tgl, nama = "Bayam", jumlah = 3.0, satuan = "ikat", harga = 3000),
            Item(tanggal = tgl, nama = "Minyak goreng kemasan 2 liter merek apa saja yang namanya panjang", jumlah = 1.0, satuan = "pcs", harga = 38500),
            Item(tanggal = tgl, nama = "Telur ayam", jumlah = 1.5, satuan = "kg", harga = 28000),
        )
        val kecil = StrukRenderer.render(tgl, sedikit, "Ummu Aisyah", "Uang kembalian Rp 5.000 sudah dimasukkan ke dompet.", namaBuku = "Belanja Kantor")
        kecil.simpan("struk-5-barang.png")
        assertEquals(1080, kecil.width)
        assertEquals(Color.WHITE, kecil.getPixel(2, 2)) // latar putih

        WarnaTema.entries.forEach { w ->
            StrukRenderer.render(tgl, sedikit, "Ummu Aisyah", "", w).simpan("struk-warna-${w.name.lowercase()}.png")
        }

        val banyak = (1..60).map { Item(tanggal = tgl, nama = "Barang ke-$it", jumlah = it.toDouble(), satuan = "pcs", harga = 1000L * it) }
        val besar = StrukRenderer.render(tgl, banyak, "", "")
        besar.simpan("struk-60-barang.png")
        assertEquals(1080, besar.width)
        assertTrue("struk 60 barang harus jauh lebih tinggi", besar.height > kecil.height * 3)
        // bagian paling bawah (footer) harus ikut tergambar, tidak terpotong
        val bawah = (besar.height - 200 until besar.height - 40).any { y ->
            (0 until besar.width step 4).any { x -> besar.getPixel(x, y) != Color.WHITE }
        }
        assertTrue("bagian bawah struk tidak boleh kosong/terpotong", bawah)
    }
}
