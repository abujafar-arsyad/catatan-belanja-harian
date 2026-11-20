package id.catatanbelanja.harian.struk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.data.WarnaTema
import id.catatanbelanja.harian.util.angka
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap

/**
 * Menggambar struk belanja ke Bitmap PNG.
 * Lebar tetap 1080 px, tinggi dihitung dulu (lintasan "ukur"), lalu digambar (lintasan "gambar"),
 * sehingga gambar tidak pernah terpotong walau barangnya banyak.
 */
object StrukRenderer {
    private const val LEBAR = 1080
    private const val TEPI = 56f
    private const val JARAK_KOLOM = 18f

    private val HITAM = Color.rgb(0x1A, 0x1A, 0x1A)
    private val ABU = Color.rgb(0x6B, 0x6B, 0x6B)
    private val MERAH = Color.rgb(0xC6, 0x28, 0x28)

    private fun paint(size: Float, bold: Boolean = false, color: Int = HITAM) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun render(
        tanggal: Long,
        items: List<Item>,
        namaPencatat: String,
        catatan: String,
        warna: WarnaTema = WarnaTema.UNGU,
        namaBuku: String = "",
        statusGanti: Boolean? = null,
    ): Bitmap {
        val ukur = Penggambar(null, tanggal, items, namaPencatat, catatan, warna, namaBuku, statusGanti)
        val tinggi = ukur.jalankan().toInt() + 1
        val bitmap = Bitmap.createBitmap(LEBAR, tinggi, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        Penggambar(canvas, tanggal, items, namaPencatat, catatan, warna, namaBuku, statusGanti).jalankan()
        return bitmap
    }

    /** Jika [c] null, hanya menghitung tinggi. */
    private class Penggambar(
        val c: Canvas?,
        val tanggal: Long,
        val items: List<Item>,
        val namaPencatat: String,
        val catatan: String,
        val warna: WarnaTema,
        val namaBuku: String,
        val statusGanti: Boolean?,
    ) {
        val UTAMA = warna.warnaStruk
        val ZEBRA = warna.zebraStruk
        var y = 0f
        val lebarIsi = LEBAR - 2 * TEPI

        // Lebar kolom: No | Nama | Jumlah | Harga | Subtotal
        val wNo = 62f
        val wJumlah = 165f
        val wHarga = 175f
        val wSub = 200f
        val wNama = lebarIsi - wNo - wJumlah - wHarga - wSub - 4 * JARAK_KOLOM
        val xNo = TEPI
        val xNama = xNo + wNo + JARAK_KOLOM
        val xJumlah = xNama + wNama + JARAK_KOLOM
        val xHarga = xJumlah + wJumlah + JARAK_KOLOM
        val xSub = xHarga + wHarga + JARAK_KOLOM

        val pJudul = paint(66f, bold = true, color = UTAMA)
        val pBuku = paint(42f, bold = true, color = UTAMA)
        val pTanggal = paint(40f, bold = true)
        val pKecil = paint(30f, color = ABU)
        val pHeader = paint(31f, bold = true)
        val pIsi = paint(33f)
        val pIsiTebal = paint(33f, bold = true)
        val pTotalLabel = paint(52f, bold = true)
        val pTotal = paint(68f, bold = true, color = UTAMA)
        val pSedang = paint(34f)

        fun jalankan(): Float {
            y = TEPI + 8f

            // Judul & tanggal
            teks("Belanja Harian", pJudul, Layout.Alignment.ALIGN_CENTER)
            if (namaBuku.isNotBlank()) {
                y += 4f
                teks(namaBuku.trim(), pBuku, Layout.Alignment.ALIGN_CENTER)
            }
            y += 10f
            teks(tanggalLengkap(tanggal), pTanggal, Layout.Alignment.ALIGN_CENTER)
            if (namaPencatat.isNotBlank()) {
                y += 6f
                teks("Dicatat oleh: ${namaPencatat.trim()}", pKecil, Layout.Alignment.ALIGN_CENTER)
            }
            y += 28f
            garis(putus = true)
            y += 20f

            // Kepala tabel
            val tinggiHeader = baris(
                listOf("No", "Nama Barang", "Jumlah", "Harga", "Subtotal"),
                pHeader, pHeader, latar = null,
            )
            y += tinggiHeader + 14f
            garis(tebal = 3f)
            y += 6f

            // Isi tabel
            items.forEachIndexed { i, item ->
                y += 8f
                val h = baris(
                    listOf(
                        "${i + 1}",
                        item.nama,
                        "${formatJumlah(item.jumlah)} ${item.satuan}",
                        angka(item.harga),
                        angka(item.subtotal),
                    ),
                    pIsi, pIsiTebal,
                    latar = if (i % 2 == 1) ZEBRA else null,
                )
                y += h + 8f
            }

            // Total
            y += 8f
            garis(tebal = 3f)
            y += 6f
            garis(tebal = 3f)
            y += 30f
            val total = items.sumOf { it.subtotal }
            val hTotal = maxOf(
                ukurTinggi("TOTAL", pTotalLabel, lebarIsi),
                ukurTinggi(rupiah(total), pTotal, lebarIsi),
            )
            gambar("TOTAL", pTotalLabel, TEPI, y + (hTotal - ukurTinggi("TOTAL", pTotalLabel, lebarIsi)) / 2, lebarIsi, Layout.Alignment.ALIGN_NORMAL)
            gambar(rupiah(total), pTotal, TEPI, y, lebarIsi, Layout.Alignment.ALIGN_OPPOSITE)
            y += hTotal + 14f
            teks("Jumlah item: ${items.size} barang", pSedang, Layout.Alignment.ALIGN_NORMAL)
            if (statusGanti != null) {
                y += 8f
                val pStatus = paint(36f, bold = true, color = if (statusGanti) UTAMA else MERAH)
                teks(if (statusGanti) "Status: SUDAH DIGANTI ✓" else "Status: BELUM DIGANTI", pStatus, Layout.Alignment.ALIGN_NORMAL)
            }
            y += 24f

            // Catatan
            if (catatan.isNotBlank()) {
                garis(putus = true)
                y += 22f
                teks("Catatan:", pIsiTebal, Layout.Alignment.ALIGN_NORMAL)
                y += 4f
                teks(catatan.trim(), pSedang, Layout.Alignment.ALIGN_NORMAL)
                y += 24f
            }

            garis(putus = true)
            y += 20f
            teks("Dibuat dengan aplikasi Catatan Belanja Harian", pKecil, Layout.Alignment.ALIGN_CENTER)
            y += TEPI
            return y
        }

        /** Menggambar satu baris tabel; mengembalikan tingginya (nama barang bisa beberapa baris). */
        fun baris(sel: List<String>, p: TextPaint, pSub: TextPaint, latar: Int?): Float {
            val kolom = listOf(
                Triple(xNo, wNo, Layout.Alignment.ALIGN_NORMAL),
                Triple(xNama, wNama, Layout.Alignment.ALIGN_NORMAL),
                Triple(xJumlah, wJumlah, Layout.Alignment.ALIGN_NORMAL),
                Triple(xHarga, wHarga, Layout.Alignment.ALIGN_OPPOSITE),
                Triple(xSub, wSub, Layout.Alignment.ALIGN_OPPOSITE),
            )
            val layouts = sel.mapIndexed { i, s ->
                val (_, w, a) = kolom[i]
                buatLayout(s, if (i == 4) pSub else p, w, a)
            }
            val h = layouts.maxOf { it.height }.toFloat()
            if (c != null && latar != null) {
                val bg = Paint().apply { color = latar }
                c.drawRect(TEPI - 10f, y - 8f, LEBAR - TEPI + 10f, y + h + 8f, bg)
            }
            layouts.forEachIndexed { i, l -> gambarLayout(l, kolom[i].first, y) }
            return h
        }

        fun teks(s: String, p: TextPaint, align: Layout.Alignment) {
            val l = buatLayout(s, p, lebarIsi, align)
            gambarLayout(l, TEPI, y)
            y += l.height
        }

        fun gambar(s: String, p: TextPaint, x: Float, top: Float, w: Float, align: Layout.Alignment) {
            gambarLayout(buatLayout(s, p, w, align), x, top)
        }

        fun ukurTinggi(s: String, p: TextPaint, w: Float) = buatLayout(s, p, w, Layout.Alignment.ALIGN_NORMAL).height.toFloat()

        fun gambarLayout(l: StaticLayout, x: Float, top: Float) {
            val canvas = c ?: return
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
        }

        fun garis(tebal: Float = 2f, putus: Boolean = false) {
            val canvas = c ?: run { y += tebal; return }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (putus) ABU else HITAM
                strokeWidth = tebal
                if (putus) pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            canvas.drawLine(TEPI, y, LEBAR - TEPI, y, p)
            y += tebal
        }
    }

    private fun buatLayout(s: String, p: TextPaint, w: Float, align: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, p, w.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.08f)
            .build()
}
