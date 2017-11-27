package br.com.oiti.certiface.challenge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.View
import br.com.oiti.certiface.data.model.challenge.ChallengeDataResponse
import br.com.oiti.certiface.data.model.challenge.ChallengeResponse
import br.com.oiti.certiface.data.source.ChallengeRepository
import java.io.ByteArrayOutputStream


class ChallengePresenter(private val view: ChallengeContract.View, endpoint: String, appKey: String) {

    private val backgroundThread = HandlerThread(this::javaClass.name)
    private var backgroundHandler: Handler

    private val repository = ChallengeRepository(endpoint, appKey)

    private val photos = HashMap<ByteArray, String>()


    init {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    fun start(params: String) {
        photos.clear()
        view.startChallenge()
        repository.challenge(params, { challengeResponse -> startChallenges(challengeResponse) })
    }

    fun destroy() {
        repository.destroy()

        backgroundHandler.removeCallbacksAndMessages(null)
        photos.clear()
    }

    private fun startChallenges(apiResponse: ChallengeResponse) {
        val chKey = apiResponse.chkey
        val totalChallengesDurationInSeconds = apiResponse.totalTime
        val snapFrequenceInMillis = apiResponse.snapFrequenceInMillis
        var startChallengeAtInMillis = 0L

        val totalChallengePictures = countTotalChallengePictures(apiResponse)

        setupCounter(totalChallengesDurationInSeconds)

        apiResponse.challenges.forEach { challenge ->
            val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
            val tipoFaceCodigo = challenge.tipoFace.codigo

            scheduleChallenge(
                    challenge,
                    startChallengeAtInMillis,
                    snapFrequenceInMillis,
                    { data ->
                        backgroundHandler.post({
                            photos.put(reduceImage(data), tipoFaceCodigo)
                            if (totalChallengePictures == photos.size) {
                                sendChallenge(chKey, photos)
                            }
                        })
                    })

            // IMPORTANTE: Não podemos deixar de incremetar o tempo de cada desafio no final
            startChallengeAtInMillis += challengeDurationInMillis
        }
    }

    private fun countTotalChallengePictures(apiResponse: ChallengeResponse): Int {
        val snapFrequenceInMillis = apiResponse.snapFrequenceInMillis
        var totalOfPictures = 0

        apiResponse.challenges.forEach { challenge ->
            val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
            val numberOfPictures = challengeDurationInMillis / snapFrequenceInMillis

            totalOfPictures += numberOfPictures
        }

        return totalOfPictures
    }

    private fun setupCounter(durationInSeconds: Int) {

        view.setCounter(durationInSeconds.toString())

        (0..(durationInSeconds - 1)).reversed().forEachIndexed { index, it ->
            val delay = (index + 1) * 1000L
            backgroundHandler.postAtTime({
                view.setCounter(it.toString())
            }, getPostAtTime(delay))
        }
    }

    private fun scheduleChallenge(challenge: ChallengeDataResponse, startAt: Long, snapFrequenceInMillis: Int, afterTakePicture: (data: ByteArray) -> Unit) {
        val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
        val numberOfPictures = challengeDurationInMillis / snapFrequenceInMillis

        val callback = view.buildTakePictureHandler(photos, afterTakePicture)

        // Agenda para alterar icone e imagem do desafio
        backgroundHandler.postAtTime({ loadChallenge(challenge) }, getPostAtTime(startAt))

        // Agenda para capturar imagens do desafio
        (1..numberOfPictures).forEach {
            val delay = startAt + (snapFrequenceInMillis * it)
            backgroundHandler.postAtTime({ takePicture(callback) }, getPostAtTime(delay))
        }
    }

    private fun getPostAtTime(delay: Long): Long = SystemClock.uptimeMillis() + delay

    private fun loadChallenge(challenge: ChallengeDataResponse) {
        loadMessage(challenge.decodedMessage)
        loadIcon(challenge.decodedIcon)
    }

    private fun loadMessage(message: Bitmap?) {
        view.setMessage(message)
    }

    private fun loadIcon(icon: Bitmap?) {
        view.loadIcon(icon)
    }

    private fun takePicture(callback: Any) {
        view.takePicture(callback)
    }

    private fun sendChallenge(chKey: String, images: Map<ByteArray, String>) {
        view.loadingView()

        repository.captcha(chKey, images, { captchaResponse ->
            val messageAnimation: String

            if (captchaResponse.valid) {
                messageAnimation = "Sucesso na autenticação"
                backgroundHandler.post({
                    Thread.sleep(2500)
                    view.finishChallenge(captchaResponse.valid)
                })
            } else {
                messageAnimation = "Erro na autenticação"
                backgroundHandler.post({
                    Thread.sleep(2500)
                    view.initialView()
                })
            }

            view.animationFeedback(View.VISIBLE, messageAnimation)
        })
    }

    private fun reduceImage(byteImage: ByteArray): ByteArray {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ALPHA_8
        options.inTargetDensity = 72
        options.inDensity = 72

        val image = BitmapFactory.decodeByteArray(byteImage, 0, byteImage.size, options)
        val bos = ByteArrayOutputStream()

        image.compress(Bitmap.CompressFormat.JPEG, 100, bos)

        return bos.toByteArray()
    }
}
