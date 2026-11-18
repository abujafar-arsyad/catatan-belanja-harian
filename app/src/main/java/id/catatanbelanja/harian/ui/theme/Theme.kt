package id.catatanbelanja.harian.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.catatanbelanja.harian.data.WarnaTema

/** Kumpulan warna lembut untuk satu pilihan tema (versi terang & gelap). */
private class Palet(
    val utama: Color, val wadahUtama: Color, val diWadahUtama: Color,
    val aksen: Color, val wadahAksen: Color, val diWadahAksen: Color,
    val latar: Color, val permukaan: Color, val permukaanTinggi: Color,
    val utamaGelap: Color, val wadahUtamaGelap: Color, val diWadahUtamaGelap: Color,
    val aksenGelap: Color, val wadahAksenGelap: Color, val diWadahAksenGelap: Color,
    val latarGelap: Color,
)

// Hijau lembut + aksen oranye muda
private val Hijau = Palet(
    utama = Color(0xFF2E7D4F), wadahUtama = Color(0xFFDDF3D8), diWadahUtama = Color(0xFF0B3A1F),
    aksen = Color(0xFFE07A1F), wadahAksen = Color(0xFFFFE6CC), diWadahAksen = Color(0xFF4A2300),
    latar = Color(0xFFF6FBF4), permukaan = Color(0xFFEDF5EA), permukaanTinggi = Color(0xFFE7F0E4),
    utamaGelap = Color(0xFF8FD6A6), wadahUtamaGelap = Color(0xFF1E5435), diWadahUtamaGelap = Color(0xFFC9F0D3),
    aksenGelap = Color(0xFFFFB877), wadahAksenGelap = Color(0xFF6A3A0D), diWadahAksenGelap = Color(0xFFFFDCC0),
    latarGelap = Color(0xFF10140F),
)

// Ungu lavender + aksen merah muda
private val Ungu = Palet(
    utama = Color(0xFF6A4C93), wadahUtama = Color(0xFFEADDFF), diWadahUtama = Color(0xFF25005A),
    aksen = Color(0xFFC2557A), wadahAksen = Color(0xFFFFD9E3), diWadahAksen = Color(0xFF3E0021),
    latar = Color(0xFFFBF8FF), permukaan = Color(0xFFF3EEFA), permukaanTinggi = Color(0xFFEDE7F6),
    utamaGelap = Color(0xFFD0BCFF), wadahUtamaGelap = Color(0xFF4F378B), diWadahUtamaGelap = Color(0xFFEADDFF),
    aksenGelap = Color(0xFFFFB0C8), wadahAksenGelap = Color(0xFF7A2948), diWadahAksenGelap = Color(0xFFFFD9E3),
    latarGelap = Color(0xFF141218),
)

// Biru langit + aksen oranye muda
private val Biru = Palet(
    utama = Color(0xFF1E6FB8), wadahUtama = Color(0xFFD6E8FF), diWadahUtama = Color(0xFF001D36),
    aksen = Color(0xFFE07A1F), wadahAksen = Color(0xFFFFE6CC), diWadahAksen = Color(0xFF4A2300),
    latar = Color(0xFFF7FAFF), permukaan = Color(0xFFEDF3FB), permukaanTinggi = Color(0xFFE6EEF8),
    utamaGelap = Color(0xFFA8CBFF), wadahUtamaGelap = Color(0xFF0E4A80), diWadahUtamaGelap = Color(0xFFD6E8FF),
    aksenGelap = Color(0xFFFFB877), wadahAksenGelap = Color(0xFF6A3A0D), diWadahAksenGelap = Color(0xFFFFDCC0),
    latarGelap = Color(0xFF0F1418),
)

private fun WarnaTema.palet() = when (this) {
    WarnaTema.HIJAU -> Hijau
    WarnaTema.UNGU -> Ungu
    WarnaTema.BIRU -> Biru
}

/** Warna contoh untuk tombol pilihan warna di Pengaturan. */
fun WarnaTema.contoh(): Color = palet().utama

private fun Palet.terang(): ColorScheme = lightColorScheme(
    primary = utama, onPrimary = Color.White,
    primaryContainer = wadahUtama, onPrimaryContainer = diWadahUtama,
    secondary = aksen, onSecondary = Color.White,
    secondaryContainer = wadahAksen, onSecondaryContainer = diWadahAksen,
    background = latar, surface = latar,
    surfaceContainer = permukaan, surfaceContainerHigh = permukaanTinggi, surfaceContainerLow = permukaan,
    surfaceVariant = permukaanTinggi,
)

private fun Palet.gelap(): ColorScheme = darkColorScheme(
    primary = utamaGelap, onPrimary = Color(0xFF101010),
    primaryContainer = wadahUtamaGelap, onPrimaryContainer = diWadahUtamaGelap,
    secondary = aksenGelap, onSecondary = Color(0xFF2A1400),
    secondaryContainer = wadahAksenGelap, onSecondaryContainer = diWadahAksenGelap,
    background = latarGelap, surface = latarGelap,
)

// Huruf sedikit lebih besar dari bawaan agar mudah dibaca
private val Besar = Typography().let { t ->
    Typography(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontSize = 19.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 17.sp, lineHeight = 23.sp),
        bodySmall = t.bodySmall.copy(fontSize = 15.sp),
        labelLarge = t.labelLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontSize = 15.sp),
    )
}

/** Gaya teks angka total yang menonjol. */
val GayaTotal = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)

@Composable
fun CatatanBelanjaTheme(gelap: Boolean, warna: WarnaTema, content: @Composable () -> Unit) {
    val p = warna.palet()
    MaterialTheme(colorScheme = if (gelap) p.gelap() else p.terang(), typography = Besar, content = content)
}
