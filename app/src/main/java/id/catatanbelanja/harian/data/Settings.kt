package id.catatanbelanja.harian.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModeTema { SISTEM, TERANG, GELAP }

/** Pilihan warna utama aplikasi (juga dipakai untuk judul & total di struk PNG). */
enum class WarnaTema(val label: String, val warnaStruk: Int, val zebraStruk: Int) {
    UNGU("Ungu", 0xFF6A4C93.toInt(), 0xFFF6F2FB.toInt()),
    HIJAU("Hijau", 0xFF2E7D4F.toInt(), 0xFFF3F8F1.toInt()),
    BIRU("Biru", 0xFF1E6FB8.toInt(), 0xFFF1F6FC.toInt()),
}

/** Pengaturan sederhana disimpan di SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("pengaturan", Context.MODE_PRIVATE)

    private val _namaPencatat = MutableStateFlow(prefs.getString(KEY_NAMA, "") ?: "")
    val namaPencatat: StateFlow<String> = _namaPencatat.asStateFlow()

    private val _tema = MutableStateFlow(
        runCatching { ModeTema.valueOf(prefs.getString(KEY_TEMA, null) ?: "") }.getOrDefault(ModeTema.SISTEM)
    )
    val tema: StateFlow<ModeTema> = _tema.asStateFlow()

    private val _warna = MutableStateFlow(
        runCatching { WarnaTema.valueOf(prefs.getString(KEY_WARNA, null) ?: "") }.getOrDefault(WarnaTema.UNGU)
    )
    val warna: StateFlow<WarnaTema> = _warna.asStateFlow()

    fun setWarna(w: WarnaTema) {
        prefs.edit().putString(KEY_WARNA, w.name).apply()
        _warna.value = w
    }

    fun setNamaPencatat(nama: String) {
        prefs.edit().putString(KEY_NAMA, nama).apply()
        _namaPencatat.value = nama
    }

    fun setTema(mode: ModeTema) {
        prefs.edit().putString(KEY_TEMA, mode.name).apply()
        _tema.value = mode
    }

    /** Buku belanja yang sedang dipakai (dipilih di halaman Hari Ini / Riwayat). */
    private val _bukuAktif = MutableStateFlow(prefs.getLong(KEY_BUKU, Buku.ID_BAWAAN))
    val bukuAktif: StateFlow<Long> = _bukuAktif.asStateFlow()

    fun setBukuAktif(id: Long) {
        prefs.edit().putLong(KEY_BUKU, id).apply()
        _bukuAktif.value = id
    }

    private companion object {
        const val KEY_BUKU = "buku_aktif"
        const val KEY_NAMA = "nama_pencatat"
        const val KEY_TEMA = "tema"
        const val KEY_WARNA = "warna"
    }
}
