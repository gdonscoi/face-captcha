package com.app.android.kotlin.facecaptcha.challenge

import android.hardware.Camera
import com.app.android.kotlin.facecaptcha.data.source.ChallengeRepository
import java.util.*


class CameraPresenter(contract: CameraContract.View?) {

    companion object {
        private var view: CameraContract.View? = null
        private var photos: MutableCollection<ByteArray>? = null

        // Mock
        private val BASE_URL = "https://comercial.certiface.com.br:8443"
        private val APP_KEY = "oKIZ1jjpRyXCDDNiR--_OPNGiNmraDZIuGE1rlUyZwOGJJzDtJR7BahJ4MqnobwetlmjXFsYFeze0eBRAGS2KWmjFUp08HYsv6pyI3KZklISJVmKJDSgfmkRmPaBR9ZJP3wtVWFDwNR9kS_vecameg"
    }

    private val repository = ChallengeRepository(BASE_URL, APP_KEY)

    init {
        view = contract
        photos = ArrayList()
    }

    fun challenge(params: String) {
        repository.challenge(params, { challengeResponse ->
            val manager = CameraManager(challengeResponse!!)
            manager.start(ManagerCallback())
        })
    }

    fun destroy() {
        view = null
        photos = null
    }

    class ManagerCallback {
        fun onBeforeSend() {
        }

        fun onError(error: String) {
        }

        fun setCount(count: String) {
            view?.setCounter(count)
        }

        fun loadMessage(mensagem: String) {
            view?.setMessage(mensagem)
        }

        fun loadIcon(icon: String) {
            view?.loadIcon(icon)
        }

        fun takePicture(pictureCallback: Camera.PictureCallback) {
            view?.tookPicture(pictureCallback)
        }

        fun sendPhotos(photos: MutableCollection<ByteArray>) {
//            dataSource?.sendPhotos(photos)
        }

        fun onComplete() {
            view?.finishCaptcha("Processando, aguarde...")
            view?.loadIcon("")
            view?.setCounter("")
//            dataSource?.captcha()
        }

    }
}
