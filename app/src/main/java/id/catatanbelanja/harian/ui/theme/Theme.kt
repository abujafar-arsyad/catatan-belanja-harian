package id.catatanbelanja.harian.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.catatanbelanja.harian.data.ModeTema

// Hijau lembut + oranye muda sebagai aksen angka total
private val Hijau = Color(0xFF2E7D4F)
private val HijauMuda = Color(0xFFDDF3D8)
private val Oranye = Color(0xFFE07A1F)
private val OranyeMuda = Color(0xFFFFE6CC)

private val Terang = lightColorScheme(
    primary = Hijau,
    onPrimary = Color.White,
    primaryContainer = HijauMuda,
    onPrimaryContainer = Color(0xFF0B3A1F),
    secondary = Oranye,
    onSecondary = Color.White,
    secondaryContainer = OranyeMuda,
    onSecondaryContainer = Color(0xFF4A2300),
    background = Color(0xFFF6FBF4),
    surface = Color(0xFFF6FBF4),
    surfaceVariant = Color(0xFFE6EFE3),
    surfaceContainer = Color(0xFFEDF5EA),
    surfaceContainerHigh = Color(0xFFE7F0E4),
    surfaceContainerLow = Color(0xFFF1F8EE),
    error = Color(0xFFB3261E),
)

private val Gelap = darkColorScheme(
    primary = Color(0xFF8FD6A6),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF1E5435),
    onPrimaryContainer = Color(0xFFC9F0D3),
    secondary = Color(0xFFFFB877),
    onSecondary = Color(0xFF4A2300),
    secondaryContainer = Color(0xFF6A3A0D),
    onSecondaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF10140F),
    surface = Color(0xFF10140F),
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
fun CatatanBelanjaTheme(mode: ModeTema, content: @Composable () -> Unit) {
    val gelap = when (mode) {
        ModeTema.SISTEM -> isSystemInDarkTheme()
        ModeTema.TERANG -> false
        ModeTema.GELAP -> true
    }
    MaterialTheme(colorScheme = if (gelap) Gelap else Terang, typography = Besar, content = content)
}
