package kodama.app

import android.app.Application
import kodama.core.di.initKoin
import kodama.ui.di.preferenceModule
import org.koin.android.ext.koin.androidContext

class Kodama : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            appDeclaration = {
                androidContext(this@Kodama)
            },
            additionalDeclaration = {
                modules(preferenceModule)
            },
        )
    }
}
