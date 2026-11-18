package id.catatanbelanja.harian.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class, Buku::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private fun buatBukuBawaan(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO buku (id, nama, urutan) VALUES (?, ?, 0)",
                arrayOf<Any>(Buku.ID_BAWAAN, Buku.NAMA_BAWAAN),
            )
        }

        /** Versi 1 -> 2: tambah tabel buku; semua catatan lama masuk ke "Belanja Rumah". */
        val MIGRASI_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `buku` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nama` TEXT NOT NULL, `urutan` INTEGER NOT NULL)"
                )
                buatBukuBawaan(db)
                db.execSQL("ALTER TABLE `items` ADD COLUMN `bukuId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_bukuId` ON `items` (`bukuId`)")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "belanja.db")
                .addMigrations(MIGRASI_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = buatBukuBawaan(db)
                })
                .build().also { instance = it }
        }
    }
}
