package id.catatanbelanja.harian

import id.catatanbelanja.harian.backup.Backup
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.parseJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LogikaTest {
    private val tgl = LocalDate.of(2026, 11, 18).toEpochDay()

    private val contoh = listOf(
        Item(tanggal = tgl, nama = "Cabai rawit", jumlah = 0.25, satuan = "kg", harga = 60000, kategori = "Bumbu", toko = "Pasar Pagi", dibuat = 1),
        Item(tanggal = tgl, nama = "Tahu, putih \"super\"", jumlah = 2.0, satuan = "bungkus", harga = 5500, catatan = "baris 1\nbaris 2; titik koma", dibuat = 2),
        Item(tanggal = tgl - 1, nama = "Beras", jumlah = 5.0, satuan = "kg", harga = 13500, dibuat = 3),
    )

    @Test fun formatRupiah() {
        assertEquals("Rp 12.500", rupiah(12500))
        assertEquals("Rp 0", rupiah(0))
        assertEquals("Rp 1.250.000", rupiah(1_250_000))
    }

    @Test fun formatJumlahDanParse() {
        assertEquals("1", formatJumlah(1.0))
        assertEquals("0,5", formatJumlah(0.5))
        assertEquals(0.5, parseJumlah("0,5")!!, 0.0)
        assertEquals(2.0, parseJumlah(" 2 ")!!, 0.0)
    }

    @Test fun tanggalIndonesia() {
        assertEquals("Rabu, 18 November 2026", tanggalLengkap(tgl))
    }

    @Test fun subtotalDibulatkan() {
        assertEquals(15000L, contoh[0].subtotal)
        assertEquals(11000L, contoh[1].subtotal)
        assertEquals(1667L, Item(tanggal = tgl, nama = "x", jumlah = 0.3333, harga = 5000).subtotal)
    }

    @Test fun jsonBolakBalik() {
        val hasil = Backup.dariJson(Backup.keJson(contoh, "Ummu Aisyah"))
        assertEquals("Ummu Aisyah", hasil.namaPencatat)
        assertEquals(contoh.map { it.copy(id = 0) }, hasil.items)
    }

    @Test fun csvBolakBalik() {
        val hasil = Backup.dariCsv(Backup.keCsv(contoh)).items
        assertEquals(contoh.size, hasil.size)
        contoh.zip(hasil).forEach { (a, b) ->
            assertEquals(Backup.kunci(a), Backup.kunci(b))
            assertEquals(a.nama, b.nama)
            assertEquals(a.catatan, b.catatan)
            assertEquals(a.toko, b.toko)
            assertEquals(a.kategori, b.kategori)
        }
    }
}
