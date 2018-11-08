package com.github.qingmei2.core.base

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.github.qingmei2.core.di.httpClientModule
import com.github.qingmei2.core.di.prefsModule
import com.github.qingmei2.module_core.BuildConfig
import com.qingmei2.rhine.logger.initLogger
import com.qingmei2.sample.di.serviceModule
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.androidModule
import org.kodein.di.android.support.androidSupportModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

@SuppressLint("Registered")
open class BaseApplication : Application(), KodeinAware {

    override val kodein: Kodein = Kodein.lazy {
        bind<Context>() with singleton { this@BaseApplication }
        import(androidModule(this@BaseApplication))
        import(androidSupportModule(this@BaseApplication))

        import(serviceModule)
        import(httpClientModule)
        import(prefsModule)
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        initLogger(BuildConfig.DEBUG)
    }

    companion object {
        lateinit var INSTANCE: BaseApplication
    }
}