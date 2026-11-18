package id.catatanbelanja.harian

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import id.catatanbelanja.harian.data.ModeTema
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
    const val HARI = "hari/{tanggal}"
    const val FORM = "form/{tanggal}/{id}"

    fun hari(tanggal: Long) = "hari/$tanggal"
    fun form(tanggal: Long, id: Long = 0) = "form/$tanggal/$id"
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
                    adalahHariIni = true,
                    onKembali = null,
                    onTambah = { nav.navigate(Rute.form(hariIni)) },
                    onEdit = { id -> nav.navigate(Rute.form(hariIni, id)) },
                )
            }
            composable(Rute.RIWAYAT) {
                RiwayatScreen(
                    app = app,
                    onBukaTanggal = { nav.navigate(Rute.hari(it)) },
                    onCari = { nav.navigate(Rute.CARI) },
                )
            }
            composable(Rute.PENGATURAN) { PengaturanScreen(app) }
            composable(Rute.CARI) {
                CariHargaScreen(app, onKembali = { nav.popBackStack() }, onBukaTanggal = { nav.navigate(Rute.hari(it)) })
            }
            composable(Rute.HARI, arguments = listOf(navArgument("tanggal") { type = NavType.LongType })) { e ->
                val tanggal = e.arguments?.getLong("tanggal") ?: hariIni
                HariScreen(
                    app = app,
                    tanggal = tanggal,
                    adalahHariIni = tanggal == hariIni,
                    onKembali = { nav.popBackStack() },
                    onTambah = { nav.navigate(Rute.form(tanggal)) },
                    onEdit = { id -> nav.navigate(Rute.form(tanggal, id)) },
                )
            }
            composable(
                Rute.FORM,
                arguments = listOf(
                    navArgument("tanggal") { type = NavType.LongType },
                    navArgument("id") { type = NavType.LongType },
                ),
            ) { e ->
                FormBarangScreen(
                    app = app,
                    tanggal = e.arguments?.getLong("tanggal") ?: hariIni,
                    itemId = e.arguments?.getLong("id") ?: 0L,
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
