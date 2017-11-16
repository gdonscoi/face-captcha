package com.app.android.kotlin.facecaptcha.feature.initial

class MainCallback(private val view: MainContract.View?) {

    fun onBeforeSend() {
        view?.onBefore()
    }

    fun onSuccess() {
        view?.onSuccess()
    }

    fun onError(error: String) {
        view?.onError(error)
    }

    fun onComplete() {
        view?.onComplete()
    }

}