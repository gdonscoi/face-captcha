package com.app.android.kotlin.facecaptcha.challenge

import com.app.android.kotlin.facecaptcha.data.source.remote.MockChallengeRemoteDataSource

class CameraPresenter(contract: CameraContract.View?, source: MockChallengeRemoteDataSource) {

    companion object {
        private var view: CameraContract.View? = null
        private var dataSource: MockChallengeRemoteDataSource? = null
    }

    init {
        view = contract
        dataSource = source
    }

    fun challenge(p: String) {
        Thread({
            val challengeResponse = dataSource?.challenge(p, ChallengeCallback())

            var totalTimeChallenges = 0L
            challengeResponse?.challenges?.map { totalTimeChallenges += it.tempoEmSegundos }

            val manager = CameraManager(challengeResponse!!)
            manager.start(totalTimeChallenges, ManagerCallback())
        }).start()
    }

    fun destroy() {
        view = null
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

        fun takePicture(countIteration: Int, delayFrameMillis: Long) {
            Thread({
                (1..countIteration).forEach {
                    view?.tookPicture()
                    Thread.sleep(delayFrameMillis)
                }
            }).start()
        }

        fun onComplete() {
            view?.finishCaptcha("Processando, aguarde...")
            view?.loadIcon("")
            view?.setCounter("")
        }

    }
}
