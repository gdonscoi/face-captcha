package br.com.oiti.certiface.data.source

import android.os.Handler
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import br.com.oiti.certiface.data.model.challenge.ChallengeResponse
import br.com.oiti.certiface.data.source.remote.ChallengeRemoteDataSource
import java.io.InterruptedIOException
import java.util.*


class ChallengeRepository(private var backgroundHandler: Handler?, endpoint: String, appKey: String) {

    private var dataSource: ChallengeRemoteDataSource? = ChallengeRemoteDataSource(endpoint, appKey)

    fun challenge(params: String, onSuccess: (response: ChallengeResponse?) -> Unit) {
        post({
            val response = dataSource?.challenge(params)
            onSuccess(response)
        })
    }

    fun captcha(chKey: String, images: Map<ByteArray, String>, onSuccess: (response: CaptchaResponse?) -> Unit) {
        post({
            val response = dataSource?.captcha(chKey, images)
            onSuccess(response)
        })
    }

    fun destroy() {
        dataSource?.destroy()
        dataSource = null
        backgroundHandler = null
    }

    private fun post(action: () -> Unit) {
        backgroundHandler?.post({
            try {
                action()
            } catch (e: ConcurrentModificationException) {
            } catch (e: InterruptedIOException) {}
        })
    }
}
