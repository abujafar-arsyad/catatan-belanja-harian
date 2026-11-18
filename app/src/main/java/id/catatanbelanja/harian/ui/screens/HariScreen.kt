package id.catatanbelanja.harian.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.ui.theme.GayaTotal
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import kotlinx.coroutines.launch

/**
 * Halaman daftar belanja untuk satu tanggal.
 * Dipakai untuk "Hari Ini" dan juga untuk detail di Riwayat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HariScreen(
    app: BelanjaApp,
    tanggal: Long,
    adalahHariIni: Boolean,
    onKembali: (() -> Unit)?,
    onTambah: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val dao = app.db.itemDao()
    val items by remember(tanggal) { dao.observeByDate(tanggal) }.collectAsStateWithLifecycle(emptyList())
    val total = items.sumOf { it.subtotal }
    val scope = rememberCoroutineScope()

    var pilihan by remember { mutableStateOf<Item?>(null) }     // menu tekan-lama
    var hapus by remember { mutableStateOf<Item?>(null) }       // konfirmasi hapus
    var bagikan by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (adalahHariIni && onKembali == null) "Belanja Hari Ini" else "Detail Belanja") },
                navigationIcon = {
                    if (onKembali != null) {
                        IconButton(onClick = onKembali) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
            )
        },
        bottomBar = {
            PanelTotal(
                total = total,
                jumlahItem = items.size,
                bisaBagikan = items.isNotEmpty(),
                onBagikan = { bagikan = true },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    tanggalLengkap(tanggal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onTambah,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Barang", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        "Belum ada barang.\nTekan \"Tambah Barang\" untuk mulai mencatat.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        "Ketuk barang untuk mengubah, tekan lama untuk pilihan lain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                BarisBarang(
                    nomor = index + 1,
                    item = item,
                    onKlik = { onEdit(item.id) },
                    onTekanLama = { pilihan = item },
                    onHapus = { hapus = item },
                )
            }
        }
    }

    pilihan?.let { item ->
        AlertDialog(
            onDismissRequest = { pilihan = null },
            title = { Text(item.nama) },
            text = { Text("Mau diapakan barang ini?") },
            confirmButton = {
                TextButton(onClick = { pilihan = null; onEdit(item.id) }) {
                    Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Ubah")
                }
            },
            dismissButton = {
                TextButton(onClick = { pilihan = null; hapus = item }) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    hapus?.let { item ->
        AlertDialog(
            onDismissRequest = { hapus = null },
            title = { Text("Hapus barang?") },
            text = { Text("\"${item.nama}\" (${rupiah(item.subtotal)}) akan dihapus dari catatan.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(item) }
                    hapus = null
                }) { Text("Ya, Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { hapus = null }) { Text("Batal") } },
        )
    }

    if (bagikan) {
        BagikanStrukDialog(app = app, tanggal = tanggal, items = items, onTutup = { bagikan = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarisBarang(
    nomor: Int,
    item: Item,
    onKlik: () -> Unit,
    onTekanLama: () -> Unit,
    onHapus: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onKlik, onLongClick = onTekanLama),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$nomor. ${item.nama}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatJumlah(item.jumlah)} ${item.satuan} × ${rupiah(item.harga)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val info = listOfNotNull(item.kategori, item.toko?.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (info.isNotEmpty()) {
                    Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                rupiah(item.subtotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onHapus) {
                Icon(Icons.Filled.Delete, contentDescription = "Hapus ${item.nama}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PanelTotal(total: Long, jumlahItem: Int, bisaBagikan: Boolean, onBagikan: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            val lebar = maxWidth > 600.dp // tablet / layar mendatar: total & tombol sebaris
            val angkaTotal = @Composable {
                Column {
                    Text("TOTAL ($jumlahItem barang)", style = MaterialTheme.typography.labelLarge)
                    Text(rupiah(total), style = GayaTotal, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            val tombol = @Composable { mod: Modifier ->
                Button(
                    onClick = onBagikan,
                    enabled = bisaBagikan,
                    modifier = mod.height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Bagikan Struk")
                }
            }
            if (lebar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { angkaTotal() }
                    tombol(Modifier.width(320.dp))
                }
            } else {
                Column {
                    angkaTotal()
                    Spacer(Modifier.height(8.dp))
                    tombol(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
