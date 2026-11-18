package id.catatanbelanja.harian.ui.screens

import android.content.Context
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
import id.catatanbelanja.harian.backup.Backup
import id.catatanbelanja.harian.data.ModeTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(app: BelanjaApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = app.db.itemDao()
    val namaTersimpan by app.settings.namaPencatat.collectAsStateWithLifecycle()
    val tema by app.settings.tema.collectAsStateWithLifecycle()
    var nama by rememberSaveable { mutableStateOf(namaTersimpan) }
    var hasilImpor by remember { mutableStateOf<Backup.HasilImpor?>(null) }

    fun pesan(teks: String) = Toast.makeText(context, teks, Toast.LENGTH_LONG).show()

    val eksporJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val isi = Backup.keJson(dao.getAll(), namaTersimpan)
                tulis(context, uri, isi)
            }.isSuccess
            pesan(if (ok) "Backup berhasil disimpan" else "Gagal menyimpan backup")
        }
    }
    val eksporCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching { tulis(context, uri, Backup.keCsv(dao.getAll())) }.isSuccess
            pesan(if (ok) "File CSV berhasil disimpan" else "Gagal menyimpan CSV")
        }
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

            Bagian("Tampilan") {
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
                    "Simpan semua catatan ke file. Kirim file itu ke HP baru (misalnya lewat WhatsApp/Google Drive), " +
                        "lalu pilih \"Impor Data\" di HP baru.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val tgl = LocalDate.now().toString()
                FilledTonalButton(
                    onClick = { eksporJson.launch("backup-belanja-$tgl.json") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Backup Data (JSON)") }
                OutlinedButton(
                    onClick = { eksporCsv.launch("belanja-$tgl.csv") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Ekspor ke Excel (CSV)") }
                OutlinedButton(
                    onClick = { impor.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Impor Data (JSON / CSV)") }
            }

            Text(
                "Catatan Belanja Harian v1.0 — semua data tersimpan di HP ini saja, tanpa internet dan tanpa login.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    hasilImpor?.let { hasil ->
        fun jalankan(ganti: Boolean) {
            hasilImpor = null
            scope.launch {
                val jumlahMasuk = if (ganti) {
                    dao.replaceAll(hasil.items); hasil.items.size
                } else {
                    val ada = dao.getAll().map(Backup::kunci).toHashSet()
                    val baru = hasil.items.filter { Backup.kunci(it) !in ada }
                    dao.insertAll(baru); baru.size
                }
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

private suspend fun tulis(context: Context, uri: Uri, isi: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(isi.toByteArray(Charsets.UTF_8)) }
        ?: error("Tidak bisa menulis file")
}

private suspend fun baca(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: error("Tidak bisa membuka file")
}
