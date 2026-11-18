package id.catatanbelanja.harian

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.catatanbelanja.harian.data.Buku
import id.catatanbelanja.harian.data.ModeTema
import id.catatanbelanja.harian.ui.screens.CariHargaScreen
import id.catatanbelanja.harian.ui.screens.FormBarangScreen
import id.catatanbelanja.harian.ui.screens.HariScreen
import id.catatanbelanja.harian.ui.screens.PengaturanScreen
import id.catatanbelanja.harian.ui.screens.RiwayatScreen
import id.catatanbelanja.harian.ui.theme.CatatanBelanjaTheme
import id.catatanbelanja.harian.util.hariIni

class MainActivity : ComponentActivity() {

    /** Tanggal "hari ini" diperbarui setiap aplikasi dibuka kembali (misalnya lewat tengah malam). */
    private val hariIniState = mutableLongStateOf(hariIni())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BelanjaApp
        setContent {
            val tema by app.settings.tema.collectAsStateWithLifecycle()
            val gelap = when (tema) {
                ModeTema.SISTEM -> isSystemInDarkTheme()
                ModeTema.TERANG -> false
                ModeTema.GELAP -> true
            }
            // Warna ikon status bar mengikuti tema aplikasi (bukan hanya tema HP)
            LaunchedEffect(gelap) {
                val gaya = if (gelap) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = gaya, navigationBarStyle = gaya)
            }
            val warna by app.settings.warna.collectAsStateWithLifecycle()
            CatatanBelanjaTheme(gelap = gelap, warna = warna) {
                AplikasiBelanja(app, hariIniState.longValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hariIniState.longValue = hariIni()
    }
}

private object Rute {
    const val HARI_INI = "hari_ini"
    const val RIWAYAT = "riwayat"
    const val PENGATURAN = "pengaturan"
    const val CARI = "cari"
    const val HARI = "hari/{tanggal}/{buku}"
    const val FORM = "form/{tanggal}/{id}/{buku}"

    fun hari(tanggal: Long, buku: Long) = "hari/$tanggal/$buku"
    fun form(tanggal: Long, buku: Long, id: Long = 0) = "form/$tanggal/$id/$buku"
}

private data class Tab(val rute: String, val label: String, val ikon: ImageVector)

private val tabs = listOf(
    Tab(Rute.HARI_INI, "Hari Ini", Icons.Filled.ShoppingCart),
    Tab(Rute.RIWAYAT, "Riwayat", Icons.Filled.DateRange),
    Tab(Rute.PENGATURAN, "Pengaturan", Icons.Filled.Settings),
)

@Composable
private fun AplikasiBelanja(app: BelanjaApp, hariIni: Long) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val ruteSekarang = entry?.destination?.route
    val tampilkanBar = ruteSekarang in tabs.map { it.rute }

    // Daftar buku belanja & buku yang sedang aktif
    val dao = app.db.itemDao()
    val daftarBuku by remember { dao.observeBuku() }.collectAsStateWithLifecycle(emptyList())
    val idAktif by app.settings.bukuAktif.collectAsStateWithLifecycle()
    val bukuAktif = daftarBuku.firstOrNull { it.id == idAktif }
        ?: daftarBuku.firstOrNull()
        ?: Buku(Buku.ID_BAWAAN, Buku.NAMA_BAWAAN)
    fun namaBuku(id: Long) = daftarBuku.firstOrNull { it.id == id } ?: Buku(id, "")

    Scaffold(
        bottomBar = {
            if (tampilkanBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = ruteSekarang == tab.rute,
                            onClick = { nav.bukaTab(tab.rute) },
                            icon = { Icon(tab.ikon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bawah = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            nav,
            startDestination = Rute.HARI_INI,
            // Layar di dalam mengurus inset atas (status bar) sendiri lewat TopAppBar.
            modifier = Modifier.padding(bawah).consumeWindowInsets(bawah),
        ) {
            composable(Rute.HARI_INI) {
                HariScreen(
                    app = app,
                    tanggal = hariIni,
                    buku = bukuAktif,
                    daftarBuku = daftarBuku,
                    adalahHariIni = true,
                    onKembali = null,
                    onTambah = { nav.navigate(Rute.form(hariIni, bukuAktif.id)) },
                    onEdit = { id -> nav.navigate(Rute.form(hariIni, bukuAktif.id, id)) },
                )
            }
            composable(Rute.RIWAYAT) {
                RiwayatScreen(
                    app = app,
                    buku = bukuAktif,
                    daftarBuku = daftarBuku,
                    onBukaTanggal = { nav.navigate(Rute.hari(it, bukuAktif.id)) },
                    onCari = { nav.navigate(Rute.CARI) },
                )
            }
            composable(Rute.PENGATURAN) { PengaturanScreen(app, daftarBuku) }
            composable(Rute.CARI) {
                CariHargaScreen(
                    app = app,
                    daftarBuku = daftarBuku,
                    onKembali = { nav.popBackStack() },
                    onBukaTanggal = { tanggal, buku -> nav.navigate(Rute.hari(tanggal, buku)) },
                )
            }
            composable(
                Rute.HARI,
                arguments = listOf(
                    navArgument("tanggal") { type = NavType.LongType },
                    navArgument("buku") { type = NavType.LongType },
                ),
            ) { e ->
                val tanggal = e.arguments?.getLong("tanggal") ?: hariIni
                val bukuId = e.arguments?.getLong("buku") ?: bukuAktif.id
                HariScreen(
                    app = app,
                    tanggal = tanggal,
                    buku = namaBuku(bukuId),
                    daftarBuku = null, // di halaman detail buku tidak bisa diganti
                    adalahHariIni = tanggal == hariIni,
                    onKembali = { nav.popBackStack() },
                    onTambah = { nav.navigate(Rute.form(tanggal, bukuId)) },
                    onEdit = { id -> nav.navigate(Rute.form(tanggal, bukuId, id)) },
                )
            }
            composable(
                Rute.FORM,
                arguments = listOf(
                    navArgument("tanggal") { type = NavType.LongType },
                    navArgument("id") { type = NavType.LongType },
                    navArgument("buku") { type = NavType.LongType },
                ),
            ) { e ->
                val bukuId = e.arguments?.getLong("buku") ?: bukuAktif.id
                FormBarangScreen(
                    app = app,
                    tanggal = e.arguments?.getLong("tanggal") ?: hariIni,
                    itemId = e.arguments?.getLong("id") ?: 0L,
                    buku = namaBuku(bukuId),
                    onSelesai = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.bukaTab(rute: String) {
    navigate(rute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
