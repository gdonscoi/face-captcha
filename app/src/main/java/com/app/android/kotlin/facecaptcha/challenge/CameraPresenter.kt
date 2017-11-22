package com.app.android.kotlin.facecaptcha.challenge

import android.hardware.Camera
import com.app.android.kotlin.facecaptcha.data.source.remote.MockChallengeRemoteDataSource
import java.util.*


class CameraPresenter(contract: CameraContract.View?, source: MockChallengeRemoteDataSource) {

    companion object {
        private var view: CameraContract.View? = null
        private var dataSource: MockChallengeRemoteDataSource? = null
        private var photos: MutableCollection<ByteArray>? = null
    }

    init {
        view = contract
        dataSource = source
        photos = ArrayList()
    }

    fun challenge(p: String) {
        Thread({
            val challengeResponse = dataSource?.challenge(p, ChallengeCallback())

            val manager = CameraManager(challengeResponse!!)
            manager.start(ManagerCallback())

        }).start()
    }

    fun destroy() {
        view = null
        photos = null
    }


    class ChallengeCallback {
        fun onBeforeSend() {
            view?.setMessage("Obtendo dados do server")
            view?.loadIcon("")
            view?.showView()
        }

        fun onError(error: String) {
            view?.setMessage(error)
        }

        fun onComplete() {

        }
    }

    class ManagerCallback {
        fun onBeforeSend() {
            view?.setMessage("Olhe para a camera")
            view?.loadIcon("")
        }

        fun onError(error: String) {
        }

        fun setCount(count: String) {
            view?.setCounter(count)
        }

        fun setMessage(mensagem: String) {
            view?.setMessage(mensagem)
        }

        fun loadIcon(icon: String) {
            view?.loadIcon(icon)
        }

        fun takePicture(pictureCallback: Camera.PictureCallback) {
            view?.tookPicture(pictureCallback)
        }

        fun sendPhotos(photos: MutableCollection<ByteArray>) {
            dataSource?.sendPhotos(photos)
        }

        fun onComplete() {
            view?.finishCaptcha("Processando, aguarde...")
            view?.loadIcon("")
            view?.setCounter("")
            dataSource?.captcha()
        }

    }
}
