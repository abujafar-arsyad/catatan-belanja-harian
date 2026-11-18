package id.catatanbelanja.harian.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.roundToLong

/**
 * Satu baris barang belanja.
 * [tanggal] disimpan sebagai "epoch day" (jumlah hari sejak 1 Jan 1970), mudah diurutkan & dikelompokkan.
 * [harga] adalah harga satuan dalam Rupiah (tanpa desimal).
 */
@Entity(tableName = "items", indices = [Index("tanggal"), Index("nama"), Index("bukuId")])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Buku belanja tempat barang ini dicatat (Rumah, Kantor, dll). */
    @ColumnInfo(defaultValue = "1") val bukuId: Long = Buku.ID_BAWAAN,
    val tanggal: Long,
    val nama: String,
    val jumlah: Double = 1.0,
    val satuan: String = "pcs",
    val harga: Long,
    val kategori: String? = null,
    val toko: String? = null,
    val catatan: String? = null,
    val dibuat: Long = System.currentTimeMillis(),
) {
    val subtotal: Long get() = (jumlah * harga).roundToLong()
}

/**
 * "Buku" belanja untuk memisahkan catatan, misalnya Belanja Rumah, Belanja Kantor,
 * Belanja Ibu Ana. Total, riwayat, dan struk dihitung per buku.
 */
@Entity(tableName = "buku")
data class Buku(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val urutan: Int = 0,
) {
    companion object {
        const val ID_BAWAAN = 1L
        const val NAMA_BAWAAN = "Belanja Rumah"
    }
}

/** Total belanja per tanggal (dipakai di halaman Riwayat). */
data class TotalHarian(
    val tanggal: Long,
    val total: Long,
    val jumlahItem: Int,
)

/** Saran autocomplete: nama barang + data terakhir kali dibeli. */
data class SaranBarang(
    val nama: String,
    val harga: Long,
    val satuan: String,
    val kategori: String?,
    val toko: String?,
)

object Pilihan {
    val satuan = listOf("pcs", "kg", "gram", "liter", "bungkus", "ikat", "buah", "lainnya")
    val kategori = listOf("Sayur", "Lauk", "Bumbu", "Sembako", "Jajan", "Kebutuhan Rumah", "Lainnya")
}
