package com.github.qingmei2.core.ui.login

import android.arch.lifecycle.MutableLiveData
import com.github.qingmei2.core.base.BaseViewModel
import com.github.qingmei2.core.http.RxSchedulers
import com.qingmei2.rhine.base.viewstate.SimpleViewState
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

@SuppressWarnings("checkResult")
class LoginViewModel : BaseViewModel() {

    val loginResult: MutableLiveData<Boolean> = MutableLiveData()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    fun login() {
        Observable.just(SimpleViewState.result(true))
            .delay(1, TimeUnit.SECONDS)     // mock login delay
            .startWith(SimpleViewState.loading())
            .startWith(SimpleViewState.idle())
            .subscribeOn(RxSchedulers.io)
            .subscribe { viewState ->
                when (viewState) {
                    is SimpleViewState.Idle -> applyState()
                    is SimpleViewState.Loading -> applyState(isLoading = true)
                    is SimpleViewState.Result -> applyState(loginResult = viewState.result)
                    else -> applyState()
                }
            }
    }

    private fun applyState(
        loginResult: Boolean = false,
        isLoading: Boolean = false
    ) {
        this.loginResult.postValue(loginResult)
        this.isLoading.postValue(isLoading)
    }
}