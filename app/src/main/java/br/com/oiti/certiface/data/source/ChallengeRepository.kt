package br.com.oiti.certiface.data.source

import android.os.Handler
import android.os.HandlerThread
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import br.com.oiti.certiface.data.model.challenge.ChallengeResponse
import br.com.oiti.certiface.data.source.remote.ChallengeRemoteDataSource


class ChallengeRepository(endpoint: String, appKey: String) {

    private val handlerThread = HandlerThread(this::javaClass.name)
    private var handler: Handler

    private val dataSource: ChallengeRemoteDataSource = ChallengeRemoteDataSource(endpoint, appKey)

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }

    fun challenge(params: String, onSuccess: (response: ChallengeResponse) -> Unit) {
        handler.post({
            val response = dataSource.challenge(params)
            onSuccess(response)
        })
    }

    fun captcha(chKey: String, images: Map<ByteArray, String>, onSuccess: (response: CaptchaResponse) -> Unit) {
        handler.post({
            val response = dataSource.captcha(chKey, images)
            onSuccess(response)
        })
    }

}
