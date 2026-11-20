package id.catatanbelanja.harian.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Buku
import kotlinx.coroutines.launch

/** Bagian Pengaturan untuk menambah, mengganti nama, dan menghapus buku belanja. */
@Composable
fun KelolaBuku(app: BelanjaApp, daftarBuku: List<Buku>) {
    val scope = rememberCoroutineScope()
    val dao = app.db.itemDao()
    val idAktif by app.settings.bukuAktif.collectAsStateWithLifecycle()

    var ubah by remember { mutableStateOf<Buku?>(null) }          // ganti nama (id 0 = buku baru)
    var hapus by remember { mutableStateOf<Pair<Buku, Int>?>(null) } // buku + jumlah catatannya

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Buku belanja", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Pisahkan catatan belanja, misalnya: Belanja Rumah, Belanja Kantor, Belanja Ibu Ana, Belanja Pak Yanto. " +
                    "Total, riwayat, dan struk dihitung per buku.",
                style = MaterialTheme.typography.bodyMedium,
            )
            daftarBuku.forEachIndexed { i, b ->
                if (i > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.nama, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (b.id == idAktif) {
                            Text("Sedang dipakai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (b.talangan) {
                            Text("Uang ditalangi (ada status sudah/belum diganti)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { ubah = b }) { Icon(Icons.Filled.Edit, contentDescription = "Ubah ${b.nama}") }
                    IconButton(
                        onClick = { scope.launch { hapus = b to dao.countItemsInBuku(b.id) } },
                        enabled = daftarBuku.size > 1, // minimal harus ada satu buku
                    ) { Icon(Icons.Filled.Delete, contentDescription = "Hapus ${b.nama}") }
                }
            }
            OutlinedButton(onClick = { ubah = Buku(id = 0, nama = "") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("+ Tambah buku")
            }
        }
    }

    ubah?.let { b ->
        var nama by remember(b) { mutableStateOf(b.nama) }
        var talangan by remember(b) { mutableStateOf(b.talangan) }
        val baru = b.id == 0L
        val namaBersih = nama.trim()
        val kembar = daftarBuku.any { it.id != b.id && it.nama.equals(namaBersih, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { ubah = null },
            title = { Text(if (baru) "Buku baru" else "Ubah buku") },
            text = {
              Column {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it.take(40) },
                    label = { Text("Nama buku") },
                    placeholder = { Text("contoh: Belanja Kantor") },
                    isError = kembar,
                    supportingText = if (kembar) ({ Text("Nama ini sudah dipakai") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                PilihanTalangan(talangan) { talangan = it }
              }
            },
            confirmButton = {
                TextButton(
                    enabled = namaBersih.isNotEmpty() && !kembar,
                    onClick = {
                        scope.launch {
                            if (baru) tambahBuku(app, namaBersih, daftarBuku, talangan)
                            else dao.updateBuku(b.copy(nama = namaBersih, talangan = talangan))
                        }
                        ubah = null
                    },
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { ubah = null }) { Text("Batal") } },
        )
    }

    hapus?.let { (b, jumlah) ->
        AlertDialog(
            onDismissRequest = { hapus = null },
            title = { Text("Hapus \"${b.nama}\"?") },
            text = {
                Text(
                    if (jumlah == 0) "Buku ini masih kosong."
                    else "Buku ini berisi $jumlah catatan barang. Semua catatan di buku ini ikut terhapus dan tidak bisa dikembalikan.\n\nSaran: buat backup dulu.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.hapusBuku(b.id)
                        if (idAktif == b.id) daftarBuku.firstOrNull { it.id != b.id }?.let { app.settings.setBukuAktif(it.id) }
                    }
                    hapus = null
                }) { Text("Ya, Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { hapus = null }) { Text("Batal") } },
        )
    }
}
