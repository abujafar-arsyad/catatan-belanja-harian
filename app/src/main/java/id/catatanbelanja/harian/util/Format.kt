package id.catatanbelanja.harian.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocaleID: Locale = Locale.forLanguageTag("id-ID")

private val symbols = DecimalFormatSymbols(LocaleID).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}
private val angkaFormat = DecimalFormat("#,##0", symbols)
private val jumlahFormat = DecimalFormat("#,##0.###", symbols)

/** 12500 -> "Rp 12.500" */
fun rupiah(nilai: Long): String =
    (if (nilai < 0) "-Rp " else "Rp ") + angkaFormat.format(kotlin.math.abs(nilai))

/** 12500 -> "12.500" (tanpa "Rp") */
fun angka(nilai: Long): String = angkaFormat.format(nilai)

/** 1.0 -> "1", 0.5 -> "0,5" */
fun formatJumlah(jumlah: Double): String = jumlahFormat.format(jumlah)

/** "0,5" atau "0.5" -> 0.5 */
fun parseJumlah(teks: String): Double? = teks.trim().replace(',', '.').toDoubleOrNull()

private val fmtLengkap = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LocaleID)
private val fmtPendek = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", LocaleID)
private val fmtTanggal = DateTimeFormatter.ofPattern("d MMM yyyy", LocaleID)
private val fmtBulan = DateTimeFormatter.ofPattern("MMMM yyyy", LocaleID)

/** "Rabu, 18 November 2026" */
fun tanggalLengkap(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(fmtLengkap)

/** "Rab, 18 Nov 2026" */
fun tanggalPendek(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(fmtPendek)

fun tanggalSaja(date: LocalDate): String = date.format(fmtTanggal)

fun namaBulan(date: LocalDate): String =
    date.format(fmtBulan).replaceFirstChar { it.uppercase(LocaleID) }

fun hariIni(): Long = LocalDate.now().toEpochDay()
