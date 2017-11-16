package com.app.android.kotlin.facecaptcha.feature.initial

interface MainContract {

    interface View {
        fun onBefore()
        fun onSuccess()
        fun onError(message: String)
        fun onComplete()
    }
}