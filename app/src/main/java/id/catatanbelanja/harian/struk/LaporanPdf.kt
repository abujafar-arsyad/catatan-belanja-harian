package id.catatanbelanja.harian.struk

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.data.WarnaTema
import id.catatanbelanja.harian.util.angka
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import id.catatanbelanja.harian.util.tanggalSaja
import java.io.OutputStream
import java.time.LocalDate

/**
 * Laporan belanja satu periode (minggu / bulan) sebagai PDF ukuran A4.
 * Halaman baru dibuat otomatis bila isinya panjang, jadi aman untuk ratusan barang.
 */
object LaporanPdf {
    data class Isi(
        val namaBuku: String,
        val judulPeriode: String,
        val dari: LocalDate,
        val sampai: LocalDate,
        val items: List<Item>,
        val namaPencatat: String,
        val talangan: Boolean,
        val rincian: Boolean,
        val warna: WarnaTema,
    )

    // A4 dalam satuan point (1/72 inci)
    private const val LEBAR = 595
    private const val TINGGI = 842
    private const val TEPI = 40f
    private const val JARAK = 8f

    private val HITAM = Color.rgb(0x1A, 0x1A, 0x1A)
    private val ABU = Color.rgb(0x6B, 0x6B, 0x6B)
    private val MERAH = Color.rgb(0xC6, 0x28, 0x28)

    private fun paint(size: Float, bold: Boolean = false, color: Int = HITAM) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun tulis(isi: Isi, out: OutputStream) {
        val doc = PdfDocument()
        try {
            Penulis(doc, isi).jalankan()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private class Penulis(val doc: PdfDocument, val isi: Isi) {
        val utama = isi.warna.warnaStruk
        val zebra = isi.warna.zebraStruk
        val lebarIsi = LEBAR - 2 * TEPI

        val pJudul = paint(20f, bold = true, color = utama)
        val pBuku = paint(14f, bold = true, color = utama)
        val pBiasa = paint(10.5f)
        val pTebal = paint(10.5f, bold = true)
        val pKecil = paint(8.5f, color = ABU)
        val pHari = paint(11f, bold = true)
        val pTotal = paint(16f, bold = true, color = utama)

        // Kolom rincian barang: No | Nama | Jumlah | Harga | Subtotal
        val wNo = 24f
        val wJumlah = 80f
        val wHarga = 80f
        val wSub = 90f
        val wNama = lebarIsi - wNo - wJumlah - wHarga - wSub - 4 * JARAK
        val xNo = TEPI
        val xNama = xNo + wNo + JARAK
        val xJumlah = xNama + wNama + JARAK
        val xHarga = xJumlah + wJumlah + JARAK
        val xSub = xHarga + wHarga + JARAK

        lateinit var halaman: PdfDocument.Page
        lateinit var c: Canvas
        var nomorHalaman = 0
        var y = 0f

        fun halamanBaru() {
            if (nomorHalaman > 0) tutupHalaman()
            nomorHalaman++
            halaman = doc.startPage(PdfDocument.PageInfo.Builder(LEBAR, TINGGI, nomorHalaman).create())
            c = halaman.canvas
            y = TEPI
            if (nomorHalaman > 1) {
                teks("Laporan Belanja — ${isi.namaBuku} — ${isi.judulPeriode} (lanjutan)", pKecil)
                y += 6f
            }
        }

        fun tutupHalaman() {
            val p = paint(8.5f, color = ABU)
            gambar("Halaman $nomorHalaman", p, TEPI, TINGGI - TEPI + 10f, lebarIsi, Layout.Alignment.ALIGN_OPPOSITE)
            gambar("Catatan Belanja Harian", p, TEPI, TINGGI - TEPI + 10f, lebarIsi, Layout.Alignment.ALIGN_NORMAL)
            doc.finishPage(halaman)
        }

        /** Pindah halaman bila sisa ruang kurang dari [tinggi]. */
        fun pastikanMuat(tinggi: Float) {
            if (y + tinggi > TINGGI - TEPI - 6f) halamanBaru()
        }

        fun jalankan() {
            halamanBaru()
            val items = isi.items
            val total = items.sumOf { it.subtotal }

            // Kepala laporan
            teks("Laporan Belanja", pJudul)
            y += 2f
            teks(isi.namaBuku, pBuku)
            y += 4f
            teks("Periode: ${tanggalSaja(isi.dari)} – ${tanggalSaja(isi.sampai)} (${isi.judulPeriode})", pBiasa)
            if (isi.namaPencatat.isNotBlank()) teks("Dicatat oleh: ${isi.namaPencatat.trim()}", pBiasa)
            teks("Dibuat: ${tanggalLengkap(LocalDate.now().toEpochDay())}", pKecil)
            y += 10f

            // Kotak ringkasan
            val hariBelanja = items.map { it.tanggal }.distinct().size
            val baris = mutableListOf(
                "Total belanja" to rupiah(total),
                "Jumlah hari belanja" to "$hariBelanja hari",
                "Jumlah barang" to "${items.size} barang",
            )
            if (isi.talangan) {
                val belum = items.filter { !it.lunas }.sumOf { it.subtotal }
                baris += "Sudah diganti" to rupiah(total - belum)
                baris += "Belum diganti" to rupiah(belum)
            }
            val tinggiBaris = 17f
            val tinggiKotak = baris.size * tinggiBaris + 14f
            c.drawRect(TEPI, y, LEBAR - TEPI, y + tinggiKotak, Paint().apply { color = zebra })
            var yy = y + 8f
            baris.forEach { (k, v) ->
                gambar(k, pBiasa, TEPI + 10f, yy, lebarIsi / 2, Layout.Alignment.ALIGN_NORMAL)
                val pv = if (k == "Belum diganti" && v != rupiah(0)) paint(10.5f, bold = true, color = MERAH) else pTebal
                gambar(v, pv, TEPI, yy, lebarIsi - 10f, Layout.Alignment.ALIGN_OPPOSITE)
                yy += tinggiBaris
            }
            y += tinggiKotak + 14f

            // Rincian per hari
            items.groupBy { it.tanggal }.toSortedMap().forEach { (tgl, list) ->
                val totalHari = list.sumOf { it.subtotal }
                val status = when {
                    !isi.talangan -> ""
                    list.all { it.lunas } -> "  ✓ sudah diganti"
                    else -> "  ● belum diganti"
                }
                pastikanMuat(if (isi.rincian) 60f else 22f)
                val tinggiHari = 18f
                c.drawRect(TEPI, y, LEBAR - TEPI, y + tinggiHari, Paint().apply { color = zebra })
                gambar(tanggalLengkap(tgl) + status, pHari, TEPI + 6f, y + 3f, lebarIsi - 100f, Layout.Alignment.ALIGN_NORMAL)
                gambar(rupiah(totalHari), pHari, TEPI, y + 3f, lebarIsi - 6f, Layout.Alignment.ALIGN_OPPOSITE)
                y += tinggiHari + 4f

                if (isi.rincian) {
                    barisTabel(listOf("No", "Nama Barang", "Jumlah", "Harga", "Subtotal"), pTebal)
                    list.forEachIndexed { i, item ->
                        barisTabel(
                            listOf(
                                "${i + 1}", item.nama, "${formatJumlah(item.jumlah)} ${item.satuan}",
                                angka(item.harga), angka(item.subtotal),
                            ),
                            pBiasa,
                        )
                    }
                    y += 6f
                }
            }

            // Total akhir
            pastikanMuat(40f)
            y += 4f
            c.drawLine(TEPI, y, LEBAR - TEPI, y, Paint().apply { color = HITAM; strokeWidth = 1.2f })
            y += 8f
            gambar("TOTAL", paint(14f, bold = true), TEPI, y + 2f, lebarIsi, Layout.Alignment.ALIGN_NORMAL)
            gambar(rupiah(total), pTotal, TEPI, y, lebarIsi, Layout.Alignment.ALIGN_OPPOSITE)
            y += 24f
            tutupHalaman()
        }

        fun barisTabel(sel: List<String>, p: TextPaint) {
            val kolom = listOf(
                Triple(xNo, wNo, Layout.Alignment.ALIGN_NORMAL),
                Triple(xNama, wNama, Layout.Alignment.ALIGN_NORMAL),
                Triple(xJumlah, wJumlah, Layout.Alignment.ALIGN_NORMAL),
                Triple(xHarga, wHarga, Layout.Alignment.ALIGN_OPPOSITE),
                Triple(xSub, wSub, Layout.Alignment.ALIGN_OPPOSITE),
            )
            val layouts = sel.mapIndexed { i, s -> buatLayout(s, p, kolom[i].second, kolom[i].third) }
            val h = layouts.maxOf { it.height }.toFloat()
            pastikanMuat(h + 4f)
            layouts.forEachIndexed { i, l -> gambarLayout(l, kolom[i].first, y) }
            y += h + 4f
        }

        fun teks(s: String, p: TextPaint) {
            val l = buatLayout(s, p, lebarIsi, Layout.Alignment.ALIGN_NORMAL)
            gambarLayout(l, TEPI, y)
            y += l.height
        }

        fun gambar(s: String, p: TextPaint, x: Float, top: Float, w: Float, align: Layout.Alignment) =
            gambarLayout(buatLayout(s, p, w, align), x, top)

        fun gambarLayout(l: StaticLayout, x: Float, top: Float) {
            c.save()
            c.translate(x, top)
            l.draw(c)
            c.restore()
        }
    }

    private fun buatLayout(s: String, p: TextPaint, w: Float, align: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, p, w.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setIncludePad(false)
            .build()
}
