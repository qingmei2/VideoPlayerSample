package com.github.qingmei2.core.ui.login

import com.qingmei2.rhine.ext.viewmodel.addLifecycle
import org.kodein.di.Kodein
import org.kodein.di.android.AndroidComponentsWeakScope
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.scoped
import org.kodein.di.generic.singleton

private const val LOGIN_MODULE_TAG = "LOGIN_MODULE_TAG"

val loginKodeinModule = Kodein.Module(LOGIN_MODULE_TAG) {

    bind<LoginViewDelegate>() with scoped(AndroidComponentsWeakScope).singleton {
        LoginViewDelegate(instance(), instance())
    }

    bind<LoginNavigator>() with scoped(AndroidComponentsWeakScope).singleton {
        LoginNavigator(instance<LoginActivity>())
    }

    bind<LoginViewModel>() with scoped(AndroidComponentsWeakScope).singleton {
        LoginViewModel().apply {
            addLifecycle(instance<LoginActivity>())
        }
    }
}