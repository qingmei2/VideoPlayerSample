package com.github.qingmei2.core.ui.login

import com.qingmei2.rhine.base.viewdelegate.IViewDelegate
import com.qingmei2.rhine.ext.livedata.toFlowable

@SuppressWarnings("checkResult")
class LoginViewDelegate(
    val viewModel: LoginViewModel,
    private val navigator: LoginNavigator
) : IViewDelegate {

    init {
        viewModel.loginResult
            .toFlowable()
            .filter { it }
            .subscribe { navigator.toMain() }
    }
}