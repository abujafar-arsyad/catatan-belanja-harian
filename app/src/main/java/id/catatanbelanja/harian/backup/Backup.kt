package id.catatanbelanja.harian.backup

import id.catatanbelanja.harian.data.Item
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Ekspor / impor data ke JSON (lengkap, untuk pindah HP) dan CSV (bisa dibuka di Excel/Google Sheets).
 * Tanggal ditulis sebagai teks "2026-11-18" supaya mudah dibaca manusia.
 */
object Backup {
    private const val FORMAT = "CatatanBelanjaHarian"
    private val KOLOM_CSV = listOf("tanggal", "nama", "jumlah", "satuan", "harga", "subtotal", "kategori", "toko", "catatan")

    data class HasilImpor(val items: List<Item>, val namaPencatat: String?)

    // ---------- JSON ----------
    fun keJson(items: List<Item>, namaPencatat: String): String {
        val arr = JSONArray()
        items.forEach { i ->
            arr.put(JSONObject().apply {
                put("tanggal", LocalDate.ofEpochDay(i.tanggal).toString())
                put("nama", i.nama)
                put("jumlah", i.jumlah)
                put("satuan", i.satuan)
                put("harga", i.harga)
                put("kategori", i.kategori ?: JSONObject.NULL)
                put("toko", i.toko ?: JSONObject.NULL)
                put("catatan", i.catatan ?: JSONObject.NULL)
                put("dibuat", i.dibuat)
            })
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("versi", 1)
            put("diekspor", System.currentTimeMillis())
            put("namaPencatat", namaPencatat)
            put("items", arr)
        }.toString(2)
    }

    fun dariJson(teks: String): HasilImpor {
        val root = JSONObject(teks)
        val arr = root.optJSONArray("items") ?: error("File bukan backup Catatan Belanja")
        val items = (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Item(
                tanggal = LocalDate.parse(o.getString("tanggal")).toEpochDay(),
                nama = o.getString("nama"),
                jumlah = o.optDouble("jumlah", 1.0),
                satuan = o.optString("satuan", "pcs"),
                harga = o.getLong("harga"),
                kategori = o.optStringOrNull("kategori"),
                toko = o.optStringOrNull("toko"),
                catatan = o.optStringOrNull("catatan"),
                dibuat = o.optLong("dibuat", System.currentTimeMillis() + idx),
            )
        }
        return HasilImpor(items, root.optStringOrNull("namaPencatat"))
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).ifBlank { null }

    // ---------- CSV ----------
    fun keCsv(items: List<Item>): String = buildString {
        append('﻿') // BOM agar Excel membaca UTF-8 dengan benar
        appendLine(KOLOM_CSV.joinToString(","))
        items.forEach { i ->
            appendLine(
                listOf(
                    LocalDate.ofEpochDay(i.tanggal).toString(), i.nama, i.jumlah.toString(), i.satuan,
                    i.harga.toString(), i.subtotal.toString(), i.kategori.orEmpty(), i.toko.orEmpty(), i.catatan.orEmpty(),
                ).joinToString(",") { csvSel(it) }
            )
        }
    }

    fun dariCsv(teks: String): HasilImpor {
        val baris = parseCsv(teks.removePrefix("﻿")).filter { r -> r.any { it.isNotBlank() } }
        if (baris.isEmpty()) error("File CSV kosong")
        val header = baris.first().map { it.trim().lowercase() }
        fun idx(n: String) = header.indexOf(n)
        val iTgl = idx("tanggal"); val iNama = idx("nama"); val iHarga = idx("harga")
        if (iTgl < 0 || iNama < 0 || iHarga < 0) error("Kolom tanggal, nama, dan harga wajib ada")
        val iJml = idx("jumlah"); val iSat = idx("satuan"); val iKat = idx("kategori")
        val iToko = idx("toko"); val iCat = idx("catatan")
        val awal = System.currentTimeMillis()
        val items = baris.drop(1).mapIndexed { n, r ->
            fun ambil(i: Int) = if (i in r.indices) r[i].trim() else ""
            Item(
                tanggal = LocalDate.parse(ambil(iTgl)).toEpochDay(),
                nama = ambil(iNama),
                jumlah = ambil(iJml).replace(',', '.').toDoubleOrNull() ?: 1.0,
                satuan = ambil(iSat).ifEmpty { "pcs" },
                harga = ambil(iHarga).filter { it.isDigit() }.toLong(),
                kategori = ambil(iKat).ifEmpty { null },
                toko = ambil(iToko).ifEmpty { null },
                catatan = ambil(iCat).ifEmpty { null },
                dibuat = awal + n,
            )
        }
        return HasilImpor(items, null)
    }

    private fun csvSel(s: String): String =
        if (s.any { it == ',' || it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    /** Parser CSV sederhana (mendukung tanda kutip dan baris baru di dalam kutip). */
    private fun parseCsv(teks: String): List<List<String>> {
        val hasil = mutableListOf<List<String>>()
        var baris = mutableListOf<String>()
        val sel = StringBuilder()
        var dalamKutip = false
        var i = 0
        while (i < teks.length) {
            val ch = teks[i]
            if (dalamKutip) {
                if (ch == '"') {
                    if (i + 1 < teks.length && teks[i + 1] == '"') { sel.append('"'); i++ } else dalamKutip = false
                } else sel.append(ch)
            } else when (ch) {
                '"' -> dalamKutip = true
                ',', ';' -> { baris.add(sel.toString()); sel.clear() }
                '\r' -> {}
                '\n' -> { baris.add(sel.toString()); sel.clear(); hasil.add(baris); baris = mutableListOf() }
                else -> sel.append(ch)
            }
            i++
        }
        if (sel.isNotEmpty() || baris.isNotEmpty()) { baris.add(sel.toString()); hasil.add(baris) }
        return hasil
    }

    /** Kunci untuk mendeteksi data yang sama saat "gabungkan". */
    fun kunci(i: Item) = "${i.tanggal}|${i.nama.trim().lowercase()}|${i.jumlah}|${i.harga}"
}
