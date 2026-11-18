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

    @Query("SELECT * FROM items WHERE tanggal = :tanggal ORDER BY dibuat ASC, id ASC")
    fun observeByDate(tanggal: Long): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY tanggal ASC, dibuat ASC, id ASC")
    suspend fun getAll(): List<Item>

    @Query(
        """SELECT tanggal,
                  SUM(CAST(ROUND(jumlah * harga) AS INTEGER)) AS total,
                  COUNT(*) AS jumlahItem
           FROM items GROUP BY tanggal ORDER BY tanggal DESC"""
    )
    fun observeDailyTotals(): Flow<List<TotalHarian>>

    /** Untuk autocomplete: tiap nama barang diambil data pembelian terakhirnya. */
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

    /** Riwayat harga suatu barang (pencarian di halaman Riwayat). */
    @Query("SELECT * FROM items WHERE nama LIKE '%' || :kata || '%' ORDER BY tanggal DESC, dibuat DESC LIMIT 300")
    fun observeSearch(kata: String): Flow<List<Item>>

    @Transaction
    suspend fun replaceAll(items: List<Item>) {
        deleteAll()
        insertAll(items)
    }
}
