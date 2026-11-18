package id.catatanbelanja.harian.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModeTema { SISTEM, TERANG, GELAP }

/** Pengaturan sederhana disimpan di SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("pengaturan", Context.MODE_PRIVATE)

    private val _namaPencatat = MutableStateFlow(prefs.getString(KEY_NAMA, "") ?: "")
    val namaPencatat: StateFlow<String> = _namaPencatat.asStateFlow()

    private val _tema = MutableStateFlow(
        runCatching { ModeTema.valueOf(prefs.getString(KEY_TEMA, null) ?: "") }.getOrDefault(ModeTema.SISTEM)
    )
    val tema: StateFlow<ModeTema> = _tema.asStateFlow()

    fun setNamaPencatat(nama: String) {
        prefs.edit().putString(KEY_NAMA, nama).apply()
        _namaPencatat.value = nama
    }

    fun setTema(mode: ModeTema) {
        prefs.edit().putString(KEY_TEMA, mode.name).apply()
        _tema.value = mode
    }

    private companion object {
        const val KEY_NAMA = "nama_pencatat"
        const val KEY_TEMA = "tema"
    }
}
