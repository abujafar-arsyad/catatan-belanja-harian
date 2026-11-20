package id.catatanbelanja.harian

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.catatanbelanja.harian.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Memastikan data pengguna lama tidak hilang saat aplikasi di-update:
 * database versi 1 (tanpa buku) -> 2 (buku) -> 3 (status sudah diganti).
 *
 * Database versi 1 dibuat persis seperti skema app/schemas/.../1.json, lalu dibuka
 * dengan Room seperti di aplikasi sungguhan. Room menjalankan migrasi dan memeriksa
 * bahwa hasilnya cocok dengan skema terbaru (gagal jika ada kolom/indeks yang salah).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrasiTest {

    @Test
    fun dataLama_tetapUtuh_setelahUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val namaDb = "uji-migrasi.db"
        val file = context.getDatabasePath(namaDb).apply { parentFile?.mkdirs(); delete() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`tanggal` INTEGER NOT NULL, `nama` TEXT NOT NULL, `jumlah` REAL NOT NULL, `satuan` TEXT NOT NULL, " +
                    "`harga` INTEGER NOT NULL, `kategori` TEXT, `toko` TEXT, `catatan` TEXT, `dibuat` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_tanggal` ON `items` (`tanggal`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_nama` ON `items` (`nama`)")
            db.execSQL(
                "INSERT INTO items (id, tanggal, nama, jumlah, satuan, harga, kategori, toko, catatan, dibuat) " +
                    "VALUES (7, 20775, 'Beras', 5.0, 'kg', 13500, 'Sembako', 'Pasar', NULL, 123)"
            )
            db.version = 1
        }

        val room = Room.databaseBuilder(context, AppDatabase::class.java, namaDb)
            .addMigrations(AppDatabase.MIGRASI_1_2, AppDatabase.MIGRASI_2_3)
            .allowMainThreadQueries()
            .build()
        val db = room.openHelper.writableDatabase
        assertEquals(3, db.version)

        db.query("SELECT nama, harga, bukuId, lunas FROM items WHERE id = 7").use { c ->
            c.moveToFirst()
            assertEquals("Beras", c.getString(0))
            assertEquals(13500L, c.getLong(1))
            assertEquals(1L, c.getLong(2))   // masuk ke "Belanja Rumah"
            assertEquals(0, c.getInt(3))     // belum diganti
        }
        db.query("SELECT id, nama, talangan FROM buku").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(1L, c.getLong(0))
            assertEquals("Belanja Rumah", c.getString(1))
            assertEquals(0, c.getInt(2))
        }
        room.close()
    }
}
