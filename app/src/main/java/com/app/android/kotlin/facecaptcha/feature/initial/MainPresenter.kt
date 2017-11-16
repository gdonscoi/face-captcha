package com.app.android.kotlin.facecaptcha.feature.initial

import com.app.android.kotlin.facecaptcha.remote.MockedFaceCaptchaService


class MainPresenter(private var view: MainContract.View?,
                    private val remoteDataSource: MockedFaceCaptchaService) {

    fun auth(appKey: String, infoPerson: String) {
        Thread({
            remoteDataSource.auth(appKey)
            val responseChallenge = remoteDataSource.challenge(appKey, infoPerson)

        }).start()
    }

    fun destroy() {
        view = null
    }
}
