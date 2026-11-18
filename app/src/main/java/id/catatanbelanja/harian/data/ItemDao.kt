package id.catatanbelanja.harian.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    // ---------- Barang ----------
    @Insert
    suspend fun insert(item: Item): Long

    @Insert
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    @Query("SELECT * FROM items WHERE tanggal = :tanggal AND bukuId = :bukuId ORDER BY dibuat ASC, id ASC")
    fun observeByDate(tanggal: Long, bukuId: Long): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY tanggal ASC, dibuat ASC, id ASC")
    suspend fun getAll(): List<Item>

    @Query(
        """SELECT tanggal,
                  SUM(CAST(ROUND(jumlah * harga) AS INTEGER)) AS total,
                  COUNT(*) AS jumlahItem
           FROM items WHERE bukuId = :bukuId GROUP BY tanggal ORDER BY tanggal DESC"""
    )
    fun observeDailyTotals(bukuId: Long): Flow<List<TotalHarian>>

    /** Untuk autocomplete (dari semua buku): tiap nama barang diambil data pembelian terakhirnya. */
    @Query(
        """SELECT i.nama AS nama, i.harga AS harga, i.satuan AS satuan, i.kategori AS kategori, i.toko AS toko
           FROM items i
           WHERE i.id = (SELECT i2.id FROM items i2 WHERE LOWER(i2.nama) = LOWER(i.nama)
                         ORDER BY i2.tanggal DESC, i2.dibuat DESC LIMIT 1)
             AND i.nama LIKE '%' || :kata || '%'
           ORDER BY (CASE WHEN i.nama LIKE :kata || '%' THEN 0 ELSE 1 END), i.tanggal DESC
           LIMIT 6"""
    )
    suspend fun suggestions(kata: String): List<SaranBarang>

    /** Riwayat harga suatu barang dari semua buku (harga pasar tidak tergantung buku). */
    @Query("SELECT * FROM items WHERE nama LIKE '%' || :kata || '%' ORDER BY tanggal DESC, dibuat DESC LIMIT 300")
    fun observeSearch(kata: String): Flow<List<Item>>

    // ---------- Buku ----------
    @Query("SELECT * FROM buku ORDER BY urutan ASC, id ASC")
    fun observeBuku(): Flow<List<Buku>>

    @Query("SELECT * FROM buku ORDER BY urutan ASC, id ASC")
    suspend fun getAllBuku(): List<Buku>

    @Insert
    suspend fun insertBuku(buku: Buku): Long

    @Insert
    suspend fun insertAllBuku(buku: List<Buku>)

    @Update
    suspend fun updateBuku(buku: Buku)

    @Query("SELECT COUNT(*) FROM items WHERE bukuId = :bukuId")
    suspend fun countItemsInBuku(bukuId: Long): Int

    @Query("DELETE FROM items WHERE bukuId = :bukuId")
    suspend fun deleteItemsInBuku(bukuId: Long)

    @Query("DELETE FROM buku WHERE id = :bukuId")
    suspend fun deleteBukuById(bukuId: Long)

    @Query("DELETE FROM buku")
    suspend fun deleteAllBuku()

    /** Hapus buku beserta semua catatan di dalamnya. */
    @Transaction
    suspend fun hapusBuku(bukuId: Long) {
        deleteItemsInBuku(bukuId)
        deleteBukuById(bukuId)
    }

    @Transaction
    suspend fun replaceAll(buku: List<Buku>, items: List<Item>) {
        deleteAll()
        deleteAllBuku()
        insertAllBuku(buku)
        insertAll(items)
    }
}
