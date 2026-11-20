package id.catatanbelanja.harian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Buku
import kotlinx.coroutines.launch

/**
 * Tombol besar berisi nama buku belanja yang sedang dipakai.
 * Jika [daftarBuku] null, hanya menampilkan nama (tidak bisa diganti).
 */
@Composable
fun PemilihBuku(app: BelanjaApp, buku: Buku, daftarBuku: List<Buku>?, modifier: Modifier = Modifier) {
    var buka by remember { mutableStateOf(false) }
    val bisaGanti = daftarBuku != null
    Card(
        onClick = { if (bisaGanti) buka = true },
        enabled = bisaGanti,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.List, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Buku belanja", style = MaterialTheme.typography.labelMedium)
                Text(buku.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (bisaGanti) {
                Text("Ganti", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
    }

    if (buka && daftarBuku != null) {
        DialogPilihBuku(app, buku, daftarBuku, onTutup = { buka = false })
    }
}

@Composable
private fun DialogPilihBuku(app: BelanjaApp, aktif: Buku, daftarBuku: List<Buku>, onTutup: () -> Unit) {
    val scope = rememberCoroutineScope()
    var namaBaru by remember { mutableStateOf("") }
    var talanganBaru by remember { mutableStateOf(false) }
    var tambahMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Pilih buku belanja") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Pisahkan catatan, misalnya belanja rumah dan belanja kantor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.padding(4.dp))
                daftarBuku.forEach { b ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .selectable(selected = b.id == aktif.id, role = Role.RadioButton, onClick = {
                                app.settings.setBukuAktif(b.id)
                                onTutup()
                            }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = b.id == aktif.id, onClick = null)
                        Text("  ${b.nama}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (!tambahMode) {
                    OutlinedButton(onClick = { tambahMode = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Buku baru")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = namaBaru,
                            onValueChange = { namaBaru = it.take(40) },
                            label = { Text("Nama buku baru") },
                            placeholder = { Text("contoh: Belanja Kantor") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PilihanTalangan(talanganBaru) { talanganBaru = it }
                        OutlinedButton(
                            onClick = {
                                val nama = namaBaru.trim()
                                if (nama.isNotEmpty()) scope.launch {
                                    val id = tambahBuku(app, nama, daftarBuku, talanganBaru)
                                    app.settings.setBukuAktif(id)
                                    onTutup()
                                }
                            },
                            enabled = namaBaru.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Simpan & pakai buku ini") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onTutup) { Text("Tutup") } },
    )
}

/** Menambah buku baru di urutan paling akhir; jika nama sudah ada, buku itu yang dipakai. */
suspend fun tambahBuku(app: BelanjaApp, nama: String, daftarBuku: List<Buku>, talangan: Boolean = false): Long {
    daftarBuku.firstOrNull { it.nama.equals(nama, ignoreCase = true) }?.let { return it.id }
    val urutan = (daftarBuku.maxOfOrNull { it.urutan } ?: 0) + 1
    return app.db.itemDao().insertBuku(Buku(nama = nama, urutan = urutan, talangan = talangan))
}

/** Kotak centang "uang ditalangi" yang dipakai saat membuat / mengubah buku. */
@Composable
fun PilihanTalangan(talangan: Boolean, onUbah: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = talangan, role = Role.Checkbox, onValueChange = onUbah)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = talangan, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Uangnya ditalangi dulu", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Untuk belanja kantor / titipan orang. Bisa ditandai \"sudah diganti\".",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
