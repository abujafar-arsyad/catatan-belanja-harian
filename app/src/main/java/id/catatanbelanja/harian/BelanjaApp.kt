package id.catatanbelanja.harian

import android.app.Application
import id.catatanbelanja.harian.data.AppDatabase
import id.catatanbelanja.harian.data.Settings

class BelanjaApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: Settings by lazy { Settings(this) }
}
