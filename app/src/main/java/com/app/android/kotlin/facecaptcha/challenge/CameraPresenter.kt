package com.app.android.kotlin.facecaptcha.challenge

import android.hardware.Camera
import android.os.Environment
import android.util.Log
import com.app.android.kotlin.facecaptcha.data.source.remote.MockChallengeRemoteDataSource
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
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

    var mPicture: Camera.PictureCallback? = null

    fun challenge(p: String) {
        Thread({
            mPicture = Camera.PictureCallback { data, camera ->
                photos?.add(data)
                camera?.stopPreview()
                camera?.startPreview()
            }

            val challengeResponse = dataSource?.challenge(p, ChallengeCallback())

            var totalTimeChallenges = 0L
            challengeResponse?.challenges?.map { totalTimeChallenges += it.tempoEmSegundos }


            val manager = CameraManager(challengeResponse!!)
            manager.start(totalTimeChallenges, ManagerCallback())

        }).start()
    }

    fun destroy() {
        view = null
        photos = null
        mPicture = null
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
            Log.i("challenge fotos size: ", photos?.size.toString())

            saveFiles()
        }

        private fun saveFiles() {
            val sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val pictureFileDir = File(sdDir, "FaceCaptcha")

            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Log.d("Save file", "Muda na mão a permissão para salvar, teste soh para ver como fica as fotos")
                return
            }

            photos?.forEach {
                val dateFormat = SimpleDateFormat("yyyymmddhhmmssSSS", Locale.getDefault())
                val date = dateFormat.format(Date())
                val photoFile = "Picture_$date.jpg"

                val filename = pictureFileDir.getPath() + File.separator + photoFile

                val pictureFile = File(filename)

                try {
                    val fos = FileOutputStream(pictureFile)
                    fos.write(it)
                    fos.close()
                } catch (error: Exception) {
                    Log.d("Write", "File" + filename + "not saved: "
                            + error.message)
                }
            }
        }

    }
}
