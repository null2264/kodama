package kodama.app

import android.app.Application
import kodama.preferences.di.preferenceStoreModule
import kodama.ui.di.preferenceModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Kodama : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Kodama)
            modules(preferenceStoreModule, preferenceModule)
        }
    }
}
