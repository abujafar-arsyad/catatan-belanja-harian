package id.catatanbelanja.harian.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.struk.StrukRenderer
import id.catatanbelanja.harian.struk.StrukShare
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Layar penuh: isi catatan opsional, lihat pratinjau struk, lalu Bagikan / Simpan ke Galeri. */
@Composable
fun BagikanStrukDialog(app: BelanjaApp, tanggal: Long, namaBuku: String, items: List<Item>, onTutup: () -> Unit) {
    val context = LocalContext.current
    val namaPencatat by app.settings.namaPencatat.collectAsStateWithLifecycle()
    val warna by app.settings.warna.collectAsStateWithLifecycle()
    var catatan by rememberSaveable { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Gambar ulang pratinjau saat catatan/daftar berubah (diberi jeda agar tidak berat saat mengetik)
    LaunchedEffect(items, catatan, namaPencatat, warna) {
        delay(if (bitmap == null) 0 else 400)
        bitmap = withContext(Dispatchers.Default) {
            StrukRenderer.render(tanggal, items, namaPencatat, catatan, warna, namaBuku)
        }
    }

    fun simpan() {
        val bmp = bitmap ?: return
        val ok = StrukShare.simpanKeGaleri(context, bmp, tanggal, namaBuku)
        Toast.makeText(
            context,
            if (ok) "Struk tersimpan di Galeri (folder Catatan Belanja)" else "Gagal menyimpan ke Galeri",
            Toast.LENGTH_LONG,
        ).show()
    }

    val izinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diizinkan ->
        if (diizinkan) simpan()
        else Toast.makeText(context, "Izin penyimpanan dibutuhkan untuk menyimpan ke Galeri", Toast.LENGTH_LONG).show()
    }

    Dialog(onDismissRequest = onTutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTutup) { Icon(Icons.Filled.Close, contentDescription = "Tutup") }
                    Text("Bagikan Struk", style = MaterialTheme.typography.titleLarge)
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = catatan,
                        onValueChange = { catatan = it.take(300) },
                        label = { Text("Catatan di struk (opsional)") },
                        placeholder = { Text("contoh: Uang kembalian Rp 5.000") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Pratinjau:", style = MaterialTheme.typography.titleMedium)
                    Box(
                        Modifier.fillMaxWidth()
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = bitmap
                        if (bmp == null) {
                            CircularProgressIndicator(Modifier.padding(48.dp))
                        } else {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Pratinjau struk",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Column(Modifier.fillMaxWidth().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            bitmap?.let {
                                val total = items.sumOf { i -> i.subtotal }
                                StrukShare.bagikan(context, it, tanggal, namaBuku, "$namaBuku — ${tanggalLengkap(tanggal)}: ${rupiah(total)}")
                            }
                        },
                        enabled = bitmap != null,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bagikan (WhatsApp, dll.)")
                    }
                    OutlinedButton(
                        onClick = {
                            val perluIzin = StrukShare.perluIzinTulis && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            ) != PackageManager.PERMISSION_GRANTED
                            if (perluIzin) izinLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else simpan()
                        },
                        enabled = bitmap != null,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) { Text("Simpan ke Galeri") }
                }
            }
        }
    }
}
