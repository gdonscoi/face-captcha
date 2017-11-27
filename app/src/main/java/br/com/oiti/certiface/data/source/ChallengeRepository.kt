package br.com.oiti.certiface.data.source

import android.os.Handler
import android.os.HandlerThread
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import br.com.oiti.certiface.data.model.challenge.ChallengeResponse
import br.com.oiti.certiface.data.source.remote.ChallengeRemoteDataSource


class ChallengeRepository(endpoint: String, appKey: String) {

    private val backgroundThread = HandlerThread(this::javaClass.name)
    private var backgroundHandler: Handler

    private val dataSource: ChallengeRemoteDataSource = ChallengeRemoteDataSource(endpoint, appKey)

    init {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    fun destroy() {
        backgroundHandler.removeCallbacksAndMessages(null)
    }

    fun challenge(params: String, onSuccess: (response: ChallengeResponse) -> Unit) {
        backgroundHandler.post({
            val response = dataSource.challenge(params)
            onSuccess(response)
        })
    }

    fun captcha(chKey: String, images: Map<ByteArray, String>, onSuccess: (response: CaptchaResponse) -> Unit) {
        backgroundHandler.post({
            val response = dataSource.captcha(chKey, images)
            onSuccess(response)
        })
    }
}
