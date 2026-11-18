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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.TotalHarian
import id.catatanbelanja.harian.util.hariIni
import id.catatanbelanja.harian.util.namaBulan
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import id.catatanbelanja.harian.util.tanggalSaja
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Ringkasan satu periode (minggu/bulan). */
private data class Ringkasan(val judul: String, val total: Long, val hariBelanja: Int, val jumlahItem: Int)

private fun awalMinggu(d: LocalDate) = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun ringkasMingguan(data: List<TotalHarian>): List<Ringkasan> =
    data.groupBy { awalMinggu(LocalDate.ofEpochDay(it.tanggal)) }
        .toSortedMap(compareByDescending { it })
        .map { (senin, list) ->
            val minggu = senin.plusDays(6)
            Ringkasan(
                "${tanggalSaja(senin)} – ${tanggalSaja(minggu)}",
                list.sumOf { it.total }, list.size, list.sumOf { it.jumlahItem },
            )
        }

private fun ringkasBulanan(data: List<TotalHarian>): List<Ringkasan> =
    data.groupBy { LocalDate.ofEpochDay(it.tanggal).withDayOfMonth(1) }
        .toSortedMap(compareByDescending { it })
        .map { (awal, list) ->
            Ringkasan(namaBulan(awal), list.sumOf { it.total }, list.size, list.sumOf { it.jumlahItem })
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(app: BelanjaApp, onBukaTanggal: (Long) -> Unit, onCari: () -> Unit) {
    val dao = app.db.itemDao()
    val harian by remember { dao.observeDailyTotals() }.collectAsStateWithLifecycle(emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val hari = LocalDate.ofEpochDay(hariIni())
    val seninIni = awalMinggu(hari)
    val totalMingguIni = harian.filter { !LocalDate.ofEpochDay(it.tanggal).isBefore(seninIni) && !LocalDate.ofEpochDay(it.tanggal).isAfter(seninIni.plusDays(6)) }.sumOf { it.total }
    val totalBulanIni = harian.filter { LocalDate.ofEpochDay(it.tanggal).let { d -> d.year == hari.year && d.month == hari.month } }.sumOf { it.total }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Belanja") },
                actions = {
                    IconButton(onClick = onCari) { Icon(Icons.Filled.Search, contentDescription = "Cari harga barang") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KotakRingkas("Minggu ini", totalMingguIni, Modifier.weight(1f))
                KotakRingkas("Bulan ini", totalBulanIni, Modifier.weight(1f))
            }
            Card(
                onClick = onCari,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text("  Cari riwayat harga barang…", style = MaterialTheme.typography.bodyLarge)
                }
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Per Hari", "Per Minggu", "Per Bulan").forEachIndexed { i, judul ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(judul, style = MaterialTheme.typography.labelLarge) })
                }
            }

            if (harian.isEmpty()) {
                Text(
                    "Belum ada riwayat belanja.",
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> items(harian, key = { it.tanggal }) { h ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onBukaTanggal(h.tanggal) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(tanggalLengkap(h.tanggal), style = MaterialTheme.typography.titleMedium)
                                    Text("${h.jumlahItem} barang", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rupiah(h.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                    1 -> items(ringkasMingguan(harian), key = { it.judul }) { KartuRingkasan(it) }
                    else -> items(ringkasBulanan(harian), key = { it.judul }) { KartuRingkasan(it) }
                }
            }
        }
    }
}

@Composable
private fun KotakRingkas(label: String, total: Long, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(rupiah(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun KartuRingkasan(r: Ringkasan) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(r.judul, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${r.hariBelanja} hari belanja · ${r.jumlahItem} barang\nRata-rata ${rupiah(r.total / r.hariBelanja.coerceAtLeast(1))}/hari",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(rupiah(r.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
