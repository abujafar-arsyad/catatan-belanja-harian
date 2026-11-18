package id.catatanbelanja.harian.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalPendek
import kotlinx.coroutines.flow.flowOf

/** Cari nama barang untuk melihat riwayat harganya dari waktu ke waktu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariHargaScreen(
    app: BelanjaApp,
    daftarBuku: List<Buku>,
    onKembali: () -> Unit,
    onBukaTanggal: (tanggal: Long, bukuId: Long) -> Unit,
) {
    val namaBuku = daftarBuku.associate { it.id to it.nama }
    val dao = app.db.itemDao()
    var kata by rememberSaveable { mutableStateOf("") }
    val kunci = kata.trim()
    val hasil by remember(kunci) {
        if (kunci.isEmpty()) flowOf(emptyList()) else dao.observeSearch(kunci)
    }.collectAsStateWithLifecycle(emptyList<Item>())
    val fokus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fokus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Harga") },
                navigationIcon = { IconButton(onClick = onKembali) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = kata,
                onValueChange = { kata = it },
                label = { Text("Nama barang (contoh: cabai)") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (kata.isNotEmpty()) IconButton(onClick = { kata = "" }) { Icon(Icons.Filled.Clear, "Hapus teks") }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(fokus),
            )

            if (hasil.isNotEmpty()) {
                val termurah = hasil.minBy { it.harga }
                val termahal = hasil.maxBy { it.harga }
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${hasil.size} kali dibeli", style = MaterialTheme.typography.titleMedium)
                        Text("Terakhir: ${rupiah(hasil.first().harga)}/${hasil.first().satuan} (${tanggalPendek(hasil.first().tanggal)})")
                        Text("Termurah: ${rupiah(termurah.harga)}/${termurah.satuan} (${tanggalPendek(termurah.tanggal)})")
                        Text("Termahal: ${rupiah(termahal.harga)}/${termahal.satuan} (${tanggalPendek(termahal.tanggal)})")
                    }
                }
            } else if (kunci.isNotEmpty()) {
                Text("Tidak ditemukan barang \"$kunci\".", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hasil, key = { it.id }) { item ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onBukaTanggal(item.tanggal, item.bukuId) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.nama, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOfNotNull(
                                        tanggalPendek(item.tanggal),
                                        item.toko,
                                        namaBuku[item.bukuId]?.takeIf { daftarBuku.size > 1 },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text("${formatJumlah(item.jumlah)} ${item.satuan} = ${rupiah(item.subtotal)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(rupiah(item.harga), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("per ${item.satuan}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
