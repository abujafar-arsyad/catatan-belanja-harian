package id.catatanbelanja.harian

import id.catatanbelanja.harian.backup.Backup
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.parseJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogikaTest {
    private val tgl = LocalDate.of(2026, 11, 18).toEpochDay()

    private val contoh = listOf(
        Item(tanggal = tgl, nama = "Cabai rawit", jumlah = 0.25, satuan = "kg", harga = 60000, kategori = "Bumbu", toko = "Pasar Pagi", dibuat = 1),
        Item(tanggal = tgl, nama = "Tahu, putih \"super\"", jumlah = 2.0, satuan = "bungkus", harga = 5500, catatan = "baris 1\nbaris 2; titik koma", dibuat = 2),
        Item(tanggal = tgl - 1, nama = "Beras", jumlah = 5.0, satuan = "kg", harga = 13500, dibuat = 3, bukuId = 2, lunas = true),
    )
    private val buku = listOf(Buku(1, "Belanja Rumah", 0), Buku(2, "Belanja Kantor", 1, talangan = true))

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

    @Test fun jsonBolakBalik_bukuIkutTersimpan() {
        val hasil = Backup.dariJson(Backup.keJson(contoh, buku, "Ummu Aisyah"))
        assertEquals("Ummu Aisyah", hasil.namaPencatat)
        assertEquals(listOf("Belanja Rumah", "Belanja Kantor"), hasil.buku)
        assertEquals(listOf("Belanja Rumah", "Belanja Rumah", "Belanja Kantor"), hasil.baris.map { it.buku })
        assertEquals(contoh.map { it.copy(id = 0, bukuId = 1) }, hasil.items)
        assertEquals(setOf("belanja kantor"), hasil.bukuTalangan)
        assertEquals(listOf(false, false, true), hasil.items.map { it.lunas })
    }

    @Test fun csvBolakBalik_bukuIkutTersimpan() {
        val hasil = Backup.dariCsv(Backup.keCsv(contoh, buku))
        assertEquals(listOf("Belanja Rumah", "Belanja Kantor"), hasil.buku)
        assertEquals(contoh.size, hasil.baris.size)
        contoh.zip(hasil.baris).forEach { (a, b) ->
            assertEquals(Backup.kunci(a.copy(bukuId = 1)), Backup.kunci(b.item))
            assertEquals(a.nama, b.item.nama)
            assertEquals(a.catatan, b.item.catatan)
            assertEquals(a.toko, b.item.toko)
            assertEquals(a.kategori, b.item.kategori)
            assertEquals(buku.first { it.id == a.bukuId }.nama, b.buku)
            assertEquals(a.lunas, b.item.lunas)
        }
    }

    @Test fun backupLamaTanpaBuku_tetapBisaDibaca() {
        val lama = """{"format":"CatatanBelanjaHarian","versi":1,"namaPencatat":"",
            "items":[{"tanggal":"2026-11-18","nama":"Cabai","jumlah":1,"satuan":"kg","harga":60000}]}"""
        val hasil = Backup.dariJson(lama)
        assertEquals(1, hasil.baris.size)
        assertEquals(null, hasil.baris[0].buku)
        assertTrue(hasil.buku.isEmpty())
    }

    @Test fun backupVersi2_bukuBerupaTeks_tetapBisaDibaca() {
        val v2 = """{"format":"CatatanBelanjaHarian","versi":2,"buku":["Belanja Rumah","Belanja Kantor"],
            "items":[{"tanggal":"2026-11-18","buku":"Belanja Kantor","nama":"Kertas","harga":55000}]}"""
        val hasil = Backup.dariJson(v2)
        assertEquals(listOf("Belanja Rumah", "Belanja Kantor"), hasil.buku)
        assertTrue(hasil.bukuTalangan.isEmpty())
        assertEquals(false, hasil.items[0].lunas)
    }
}
