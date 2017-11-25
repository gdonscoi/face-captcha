package br.com.oiti.certiface.data.source

import android.os.Handler
import android.os.HandlerThread
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import br.com.oiti.certiface.data.source.remote.ChallengeRemoteDataSource


/**
 * Created by bzumpano on 16/11/17.
 */
class ChallengeRepository(baseUrl: String, appKey: String) {

    private val handlerThread = HandlerThread(this::javaClass.name)
    private var handler: Handler

    private val dataSource: ChallengeRemoteDataSource = ChallengeRemoteDataSource(baseUrl, appKey)

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

    fun captcha(chkey: String, images: Map<ByteArray, String>, onSuccess: (response: CaptchaResponse) -> Unit) {
        handler.post({
            val response = dataSource.captcha(chkey, images)
            onSuccess(response)
        })
    }

}
