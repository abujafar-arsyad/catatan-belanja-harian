package id.catatanbelanja.harian.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.struk.LaporanPdf
import id.catatanbelanja.harian.struk.StrukShare
import id.catatanbelanja.harian.util.rupiah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dialog membuat laporan PDF untuk satu minggu / bulan, lalu dibagikan atau disimpan ke Download. */
@Composable
fun LaporanDialog(app: BelanjaApp, buku: Buku, r: Ringkasan, onTutup: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rincian by remember { mutableStateOf(true) }
    var sibuk by remember { mutableStateOf(false) }

    /** Membuat file PDF di cache; null jika gagal. */
    suspend fun buatPdf(): File? = withContext(Dispatchers.IO) {
        runCatching {
            val items = app.db.itemDao().getItemsRange(buku.id, r.dari.toEpochDay(), r.sampai.toEpochDay())
            val aman = buku.nama.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "Belanja" }
            val file = StrukShare.fileSementara(context, "Laporan_${aman}_${r.dari}_${r.sampai}.pdf")
            file.outputStream().use { out ->
                LaporanPdf.tulis(
                    LaporanPdf.Isi(
                        namaBuku = buku.nama,
                        judulPeriode = r.judul,
                        dari = r.dari,
                        sampai = r.sampai,
                        items = items,
                        namaPencatat = app.settings.namaPencatat.value,
                        talangan = buku.talangan,
                        rincian = rincian,
                        warna = app.settings.warna.value,
                    ),
                    out,
                )
            }
            file
        }.getOrNull()
    }

    fun bagikan() = scope.launch {
        sibuk = true
        val file = buatPdf()
        sibuk = false
        if (file == null) {
            Toast.makeText(context, "Gagal membuat laporan", Toast.LENGTH_LONG).show()
        } else {
            StrukShare.bagikanFile(
                context, file, "application/pdf",
                "Laporan ${buku.nama} ${r.judul}: ${rupiah(r.total)}", "Bagikan laporan lewat…",
            )
        }
    }

    fun simpan() = scope.launch {
        sibuk = true
        val ok = buatPdf()?.let { f -> withContext(Dispatchers.IO) { StrukShare.simpanKeDownload(context, f, "application/pdf") } } ?: false
        sibuk = false
        Toast.makeText(
            context,
            if (ok) "Laporan tersimpan di folder Download" else "Gagal menyimpan laporan",
            Toast.LENGTH_LONG,
        ).show()
    }

    val izin = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) simpan() else Toast.makeText(context, "Izin penyimpanan dibutuhkan", Toast.LENGTH_LONG).show()
    }

    AlertDialog(
        onDismissRequest = { if (!sibuk) onTutup() },
        title = { Text("Laporan ${r.judul}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(buku.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${r.hariBelanja} hari belanja · ${r.jumlahItem} barang", style = MaterialTheme.typography.bodyMedium)
                Text("Total ${rupiah(r.total)}", style = MaterialTheme.typography.titleMedium)
                if (buku.talangan) {
                    Text(
                        if (r.belumLunas > 0) "Belum diganti: ${rupiah(r.belumLunas)}" else "Semua sudah diganti",
                        color = if (r.belumLunas > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = rincian, role = Role.Checkbox, onValueChange = { rincian = it })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = rincian, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sertakan rincian setiap barang", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(4.dp))
                if (sibuk) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Membuat PDF…")
                    }
                } else {
                    Button(onClick = { bagikan() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bagikan PDF (WhatsApp, dll.)")
                    }
                    OutlinedButton(
                        onClick = {
                            val perlu = StrukShare.perluIzinTulis && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            ) != PackageManager.PERMISSION_GRANTED
                            if (perlu) izin.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else simpan()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Simpan ke folder Download") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onTutup, enabled = !sibuk) { Text("Tutup") } },
    )
}
