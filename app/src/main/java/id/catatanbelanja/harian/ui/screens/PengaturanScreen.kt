package id.catatanbelanja.harian.ui.screens

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import id.catatanbelanja.harian.struk.StrukShare
import java.io.File
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.BuildConfig
import id.catatanbelanja.harian.backup.Backup
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.data.ModeTema
import id.catatanbelanja.harian.data.WarnaTema
import id.catatanbelanja.harian.ui.theme.contoh
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(app: BelanjaApp, daftarBuku: List<Buku>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = app.db.itemDao()
    val namaTersimpan by app.settings.namaPencatat.collectAsStateWithLifecycle()
    val tema by app.settings.tema.collectAsStateWithLifecycle()
    val warna by app.settings.warna.collectAsStateWithLifecycle()
    var nama by rememberSaveable { mutableStateOf(namaTersimpan) }
    var hasilImpor by remember { mutableStateOf<Backup.HasilImpor?>(null) }

    fun pesan(teks: String) = Toast.makeText(context, teks, Toast.LENGTH_LONG).show()

    /*
     * Backup tidak memakai pemilih file (di sebagian HP, menulis lewat pintasan "Downloads"
     * di pemilih file ditolak sistem dan menghasilkan file kosong). Sebagai gantinya file dibuat
     * di cache lalu disimpan langsung ke folder Download atau dikirim lewat menu Share.
     */
    val tgl = LocalDate.now().toString()

    suspend fun buatFileEkspor(jenis: JenisEkspor): File? = withContext(Dispatchers.IO) {
        runCatching {
            val isi = when (jenis) {
                JenisEkspor.BACKUP -> Backup.keJson(dao.getAll(), dao.getAllBuku(), namaTersimpan)
                JenisEkspor.CSV -> Backup.keCsv(dao.getAll(), dao.getAllBuku())
            }
            val nama = if (jenis == JenisEkspor.BACKUP) "backup-belanja-$tgl.json" else "belanja-$tgl.csv"
            StrukShare.fileSementara(context, nama).apply { writeText(isi, Charsets.UTF_8) }
        }.onFailure { android.util.Log.e("Backup", "gagal membuat file", it) }.getOrNull()
    }

    fun simpanKeDownload(jenis: JenisEkspor) = scope.launch {
        val ok = buatFileEkspor(jenis)?.let { f ->
            withContext(Dispatchers.IO) { StrukShare.simpanKeDownload(context, f, jenis.mime) }
        } ?: false
        pesan(
            when {
                !ok -> "Gagal menyimpan file"
                jenis == JenisEkspor.BACKUP -> "Backup tersimpan di folder Download (backup-belanja-$tgl.json)"
                else -> "File Excel tersimpan di folder Download (belanja-$tgl.csv)"
            }
        )
    }

    fun kirim(jenis: JenisEkspor) = scope.launch {
        val f = buatFileEkspor(jenis) ?: return@launch pesan("Gagal membuat file")
        StrukShare.bagikanFile(
            context, f, jenis.mime,
            if (jenis == JenisEkspor.BACKUP) "Backup Catatan Belanja $tgl" else "Catatan belanja $tgl (CSV)",
            "Kirim file lewat…",
        )
    }

    var tertunda by remember { mutableStateOf<JenisEkspor?>(null) }
    val izinTulis = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val jenis = tertunda
        tertunda = null
        if (ok && jenis != null) simpanKeDownload(jenis) else pesan("Izin penyimpanan dibutuhkan")
    }
    fun mintaSimpan(jenis: JenisEkspor) {
        val perlu = StrukShare.perluIzinTulis && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) != PackageManager.PERMISSION_GRANTED
        if (perlu) { tertunda = jenis; izinTulis.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) } else simpanKeDownload(jenis)
    }
    val impor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val isi = baca(context, uri)
                if (isi.trimStart('﻿', ' ', '\n', '\r', '\t').startsWith("{")) Backup.dariJson(isi) else Backup.dariCsv(isi)
            }.onSuccess { hasilImpor = it }
                .onFailure { pesan("File tidak bisa dibaca: ${it.message ?: "format salah"}") }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Pengaturan") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Bagian("Nama pencatat") {
                Text("Tampil di struk sebagai \"Dicatat oleh: …\"", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it.take(40); app.settings.setNamaPencatat(nama.trim()) },
                    label = { Text("Nama") },
                    placeholder = { Text("contoh: Ummu Aisyah") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            KelolaBuku(app, daftarBuku)

            Bagian("Tampilan") {
                Text("Warna tema (juga dipakai di struk)", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WarnaTema.entries.forEach { w ->
                        val dipilih = warna == w
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    if (dipilih) 3.dp else 1.dp,
                                    if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp),
                                )
                                .selectable(selected = dipilih, onClick = { app.settings.setWarna(w) }, role = Role.RadioButton)
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(w.contoh()),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (dipilih) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(w.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Terang / gelap", style = MaterialTheme.typography.bodyMedium)
                listOf(
                    ModeTema.SISTEM to "Ikuti pengaturan HP",
                    ModeTema.TERANG to "Terang",
                    ModeTema.GELAP to "Gelap (mode malam)",
                ).forEach { (mode, label) ->
                    Row(
                        Modifier.fillMaxWidth().height(52.dp)
                            .selectable(selected = tema == mode, onClick = { app.settings.setTema(mode) }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tema == mode, onClick = null)
                        Text("  $label", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Bagian("Backup & Pindah HP") {
                Text(
                    "Simpan semua catatan (semua buku) ke satu file backup. Kirim file itu ke HP baru " +
                        "(misalnya lewat WhatsApp/Google Drive), lalu pilih \"Impor Data\" di HP baru.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(
                    onClick = { kirim(JenisEkspor.BACKUP) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Kirim Backup (WhatsApp, Drive, dll.)") }
                OutlinedButton(
                    onClick = { mintaSimpan(JenisEkspor.BACKUP) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Simpan Backup ke folder Download") }
                OutlinedButton(
                    onClick = { mintaSimpan(JenisEkspor.CSV) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Ekspor ke Excel (CSV) ke folder Download") }
                OutlinedButton(
                    onClick = { impor.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Impor Data (JSON / CSV)") }
            }

            Text(
                "Catatan Belanja Harian v${BuildConfig.VERSION_NAME} — semua data tersimpan di HP ini saja, tanpa internet dan tanpa login.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    hasilImpor?.let { hasil ->
        fun jalankan(ganti: Boolean) {
            hasilImpor = null
            scope.launch {
                val jumlahMasuk = terapkanImpor(app, hasil, ganti)
                if (namaTersimpan.isBlank() && !hasil.namaPencatat.isNullOrBlank()) {
                    app.settings.setNamaPencatat(hasil.namaPencatat)
                    nama = hasil.namaPencatat
                }
                pesan("Impor selesai: $jumlahMasuk barang dimasukkan")
            }
        }
        AlertDialog(
            onDismissRequest = { hasilImpor = null },
            title = { Text("Impor ${hasil.items.size} barang?") },
            text = {
                Text(
                    "Gabungkan: data lama tetap ada, data dari file ditambahkan (yang sama dilewati).\n\n" +
                        "Ganti Semua: data di HP ini dihapus dan diganti isi file.",
                )
            },
            confirmButton = { TextButton(onClick = { jalankan(ganti = false) }) { Text("Gabungkan") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { hasilImpor = null }) { Text("Batal") }
                    TextButton(onClick = { jalankan(ganti = true) }) { Text("Ganti Semua", color = MaterialTheme.colorScheme.error) }
                }
            },
        )
    }
}

@Composable
private fun Bagian(judul: String, isi: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(judul, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            isi()
        }
    }
}


private suspend fun baca(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: error("Tidak bisa membuka file")
}

/**
 * Memasukkan hasil impor ke database.
 * - Ganti semua: semua buku & catatan diganti isi file.
 * - Gabungkan: buku dicocokkan berdasarkan nama (buku baru dibuat bila belum ada), catatan yang sama dilewati.
 * Catatan dari file lama yang belum punya buku masuk ke buku yang sedang aktif (atau "Belanja Rumah").
 * Mengembalikan jumlah barang yang dimasukkan.
 */
private suspend fun terapkanImpor(app: BelanjaApp, hasil: Backup.HasilImpor, ganti: Boolean): Int {
    val dao = app.db.itemDao()
    if (ganti) {
        val nama = (hasil.buku + if (hasil.baris.any { it.buku == null }) listOf(Buku.NAMA_BAWAAN) else emptyList())
            .distinctBy { it.lowercase() }
            .ifEmpty { listOf(Buku.NAMA_BAWAAN) }
        val buku = nama.mapIndexed { i, n -> Buku(id = i + 1L, nama = n, urutan = i, talangan = n.lowercase() in hasil.bukuTalangan) }
        val idDari = buku.associate { it.nama.lowercase() to it.id }
        val items = hasil.baris.map { b ->
            b.item.copy(id = 0, bukuId = idDari[(b.buku ?: Buku.NAMA_BAWAAN).lowercase()] ?: buku.first().id)
        }
        dao.replaceAll(buku, items)
        app.settings.setBukuAktif(buku.first().id)
        return items.size
    }
    val ada = dao.getAllBuku().toMutableList()
    val idDari = ada.associate { it.nama.lowercase() to it.id }.toMutableMap()
    hasil.buku.forEach { n ->
        if (n.lowercase() !in idDari) {
            val urutan = (ada.maxOfOrNull { it.urutan } ?: 0) + 1
            val talangan = n.lowercase() in hasil.bukuTalangan
            val id = dao.insertBuku(Buku(nama = n, urutan = urutan, talangan = talangan))
            ada += Buku(id, n, urutan, talangan)
            idDari[n.lowercase()] = id
        }
    }
    val aktif = app.settings.bukuAktif.value.takeIf { id -> ada.any { it.id == id } } ?: ada.first().id
    val items = hasil.baris.map { b ->
        b.item.copy(id = 0, bukuId = b.buku?.let { idDari[it.lowercase()] } ?: aktif)
    }
    val sudahAda = dao.getAll().map(Backup::kunci).toHashSet()
    val baru = items.filter { Backup.kunci(it) !in sudahAda }
    dao.insertAll(baru)
    return baru.size
}

/** Jenis file yang bisa diekspor dari Pengaturan. */
private enum class JenisEkspor(val mime: String) {
    BACKUP("application/json"),
    CSV("text/csv"),
}
