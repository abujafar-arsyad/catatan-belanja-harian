package id.catatanbelanja.harian.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import id.catatanbelanja.harian.util.LocaleID
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.mutableLongStateOf
import id.catatanbelanja.harian.util.hariIni
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.catatanbelanja.harian.BelanjaApp
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.data.Item
import id.catatanbelanja.harian.data.Pilihan
import id.catatanbelanja.harian.data.SaranBarang
import id.catatanbelanja.harian.ui.theme.GayaTotal
import id.catatanbelanja.harian.util.formatJumlah
import id.catatanbelanja.harian.util.parseJumlah
import id.catatanbelanja.harian.util.rupiah
import id.catatanbelanja.harian.util.tanggalLengkap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormBarangScreen(app: BelanjaApp, tanggal: Long, itemId: Long, buku: Buku, onSelesai: () -> Unit) {
    val dao = app.db.itemDao()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val modeEdit = itemId != 0L

    var asli by remember { mutableStateOf<Item?>(null) }
    var dimuat by rememberSaveable { mutableStateOf(!modeEdit) }

    var nama by rememberSaveable { mutableStateOf("") }
    var jumlahTeks by rememberSaveable { mutableStateOf("1") }
    var satuan by rememberSaveable { mutableStateOf("pcs") }
    var satuanLain by rememberSaveable { mutableStateOf("") }
    var hargaTeks by rememberSaveable { mutableStateOf("") }
    var kategori by rememberSaveable { mutableStateOf<String?>(null) }
    var toko by rememberSaveable { mutableStateOf("") }
    var catatan by rememberSaveable { mutableStateOf("") }
    var cobaSimpan by remember { mutableStateOf(false) }
    var konfirmasiHapus by remember { mutableStateOf(false) }
    // Tanggal belanja: bisa diganti, misalnya untuk mencatat belanja kemarin yang terlupa
    var tanggalDipilih by rememberSaveable { mutableLongStateOf(tanggal) }
    var pilihTanggal by remember { mutableStateOf(false) }

    // Saran autocomplete
    var saran by remember { mutableStateOf<List<SaranBarang>>(emptyList()) }
    var tampilkanSaran by remember { mutableStateOf(!modeEdit) }
    val fokusNama = remember { FocusRequester() }

    LaunchedEffect(itemId) {
        if (modeEdit) {
            val item = dao.getById(itemId)
            asli = item
            if (item != null && !dimuat) {
                nama = item.nama
                jumlahTeks = formatJumlah(item.jumlah).replace(".", "")
                if (item.satuan in Pilihan.satuan) satuan = item.satuan
                else { satuan = "lainnya"; satuanLain = item.satuan }
                hargaTeks = item.harga.toString()
                kategori = item.kategori
                toko = item.toko.orEmpty()
                catatan = item.catatan.orEmpty()
                tanggalDipilih = item.tanggal
                dimuat = true
            }
        }
    }

    LaunchedEffect(nama, tampilkanSaran) {
        if (!tampilkanSaran || nama.isBlank()) { saran = emptyList(); return@LaunchedEffect }
        delay(150)
        saran = dao.suggestions(nama.trim()).filterNot { it.nama.equals(nama.trim(), ignoreCase = true) && it.harga.toString() == hargaTeks }
    }

    val jumlah = parseJumlah(jumlahTeks)
    val harga = hargaTeks.toLongOrNull()
    val subtotal = if (jumlah != null && harga != null) (jumlah * harga).roundToLong() else 0L
    val satuanFinal = if (satuan == "lainnya") satuanLain.trim().ifEmpty { "lainnya" } else satuan

    val errNama = cobaSimpan && nama.isBlank()
    val errJumlah = cobaSimpan && (jumlah == null || jumlah <= 0)
    val errHarga = cobaSimpan && harga == null

    fun buatItem(): Item? {
        if (nama.isBlank() || jumlah == null || jumlah <= 0 || harga == null) return null
        val dasar = asli
        return Item(
            id = dasar?.id ?: 0,
            tanggal = tanggalDipilih,
            bukuId = dasar?.bukuId ?: buku.id,
            lunas = dasar?.lunas ?: false,
            nama = nama.trim().replaceFirstChar { it.uppercase() },
            jumlah = jumlah,
            satuan = satuanFinal,
            harga = harga,
            kategori = kategori,
            toko = toko.trim().ifEmpty { null },
            catatan = catatan.trim().ifEmpty { null },
            dibuat = dasar?.dibuat ?: System.currentTimeMillis(),
        )
    }

    fun kosongkan() {
        nama = ""; jumlahTeks = "1"; satuan = "pcs"; satuanLain = ""; hargaTeks = ""
        kategori = null; catatan = ""; cobaSimpan = false; tampilkanSaran = true
        // toko sengaja tidak dikosongkan: biasanya belanja di toko yang sama
    }

    fun simpan(lagi: Boolean) {
        cobaSimpan = true
        val item = buatItem() ?: return
        scope.launch {
            if (modeEdit) dao.update(item) else dao.insert(item)
            if (item.tanggal != tanggal) {
                // Barang pindah ke tanggal lain: beri tahu supaya tidak bingung kenapa tidak muncul di daftar ini
                Toast.makeText(context, "${item.nama} disimpan di tanggal ${tanggalLengkap(item.tanggal)}", Toast.LENGTH_LONG).show()
            }
            if (lagi) {
                Toast.makeText(context, "${item.nama} tersimpan", Toast.LENGTH_SHORT).show()
                kosongkan()
                fokusNama.requestFocus()
            } else {
                onSelesai()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (modeEdit) "Ubah Barang" else "Tambah Barang") },
                navigationIcon = {
                    IconButton(onClick = onSelesai) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                },
                actions = {
                    if (modeEdit) {
                        IconButton(onClick = { konfirmasiHapus = true }) {
                            Icon(Icons.Filled.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subtotal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(rupiah(subtotal), style = GayaTotal.copy(fontSize = GayaTotal.fontSize * 0.8f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!modeEdit) {
                            OutlinedButton(onClick = { simpan(lagi = true) }, modifier = Modifier.weight(1f).height(56.dp)) {
                                Text("Simpan + Lagi")
                            }
                        }
                        Button(onClick = { simpan(lagi = false) }, modifier = Modifier.weight(1f).height(56.dp)) {
                            Text("Simpan")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                onClick = { pilihTanggal = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tanggalLengkap(tanggalDipilih), style = MaterialTheme.typography.titleMedium)
                        Text("Dicatat di: ${buku.nama}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Ubah tanggal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it; tampilkanSaran = true },
                label = { Text("Nama barang *") },
                placeholder = { Text("contoh: Cabai rawit") },
                isError = errNama,
                supportingText = if (errNama) ({ Text("Nama barang wajib diisi") }) else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(fokusNama),
            )

            if (saran.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "Pernah dibeli (ketuk untuk memakai):",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    )
                    saran.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                nama = s.nama
                                hargaTeks = s.harga.toString()
                                if (s.satuan in Pilihan.satuan) satuan = s.satuan
                                else { satuan = "lainnya"; satuanLain = s.satuan }
                                if (s.kategori != null) kategori = s.kategori
                                if (!s.toko.isNullOrBlank() && toko.isBlank()) toko = s.toko
                                tampilkanSaran = false
                            }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.nama, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("${rupiah(s.harga)}/${s.satuan}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = jumlahTeks,
                    onValueChange = { v -> jumlahTeks = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8) },
                    label = { Text("Jumlah") },
                    isError = errJumlah,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.width(120.dp),
                )
                OutlinedTextField(
                    value = hargaTeks,
                    onValueChange = { v -> hargaTeks = v.filter { it.isDigit() }.trimStart('0').take(11) },
                    label = { Text("Harga satuan *") },
                    prefix = { Text("Rp ") },
                    isError = errHarga,
                    supportingText = {
                        when {
                            errHarga -> Text("Harga wajib diisi")
                            harga != null -> Text(rupiah(harga))
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Satuan", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pilihan.satuan.forEach { s ->
                    FilterChip(
                        selected = satuan == s,
                        onClick = { satuan = s },
                        label = { Text(s, style = MaterialTheme.typography.bodyLarge) },
                    )
                }
            }
            if (satuan == "lainnya") {
                OutlinedTextField(
                    value = satuanLain,
                    onValueChange = { satuanLain = it.take(15) },
                    label = { Text("Tulis satuan (contoh: papan, renteng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()
            Text("Tambahan (boleh dikosongkan)", style = MaterialTheme.typography.titleMedium)

            Text("Kategori", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pilihan.kategori.forEach { k ->
                    FilterChip(
                        selected = kategori == k,
                        onClick = { kategori = if (kategori == k) null else k },
                        label = { Text(k, style = MaterialTheme.typography.bodyLarge) },
                    )
                }
            }

            OutlinedTextField(
                value = toko,
                onValueChange = { toko = it },
                label = { Text("Nama toko / pasar") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text("Catatan barang") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pilihTanggal) {
        // Kalender selalu berbahasa Indonesia, walaupun bahasa HP diatur ke bahasa lain
        val konfigurasi = LocalConfiguration.current
        val konfigurasiID = remember(konfigurasi) { Configuration(konfigurasi).apply { setLocale(LocaleID) } }
        CompositionLocalProvider(LocalConfiguration provides konfigurasiID) {
        val hariIniMs = hariIni() * HARI_MS
        val state = rememberDatePickerState(
            initialSelectedDateMillis = tanggalDipilih * HARI_MS,
            selectableDates = object : SelectableDates {
                // Belanja yang dicatat tidak mungkin di masa depan
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hariIniMs
            },
        )
        DatePickerDialog(
            onDismissRequest = { pilihTanggal = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { tanggalDipilih = it / HARI_MS }
                    pilihTanggal = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { pilihTanggal = false }) { Text("Batal") } },
        ) {
            DatePicker(
                state = state,
                title = { Text("Tanggal belanja", Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = {
                    Text(
                        state.selectedDateMillis?.let { tanggalLengkap(it / HARI_MS) } ?: "Pilih tanggal",
                        Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                showModeToggle = false,
            )
        }
        }
    }

    if (konfirmasiHapus) {
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = false },
            title = { Text("Hapus barang?") },
            text = { Text("\"${asli?.nama ?: nama}\" akan dihapus dari catatan.") },
            confirmButton = {
                TextButton(onClick = {
                    konfirmasiHapus = false
                    asli?.let { item -> scope.launch { dao.delete(item); onSelesai() } }
                }) { Text("Ya, Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { konfirmasiHapus = false }) { Text("Batal") } },
        )
    }
}

/** Satu hari dalam milidetik (DatePicker memakai waktu UTC tengah malam). */
private const val HARI_MS = 86_400_000L
