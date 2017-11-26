package br.com.oiti.certiface.challenge.test

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.support.annotation.RequiresApi
import java.io.File

/**
 * Created by bzumpano on 25/11/17.
 */
/**
 * Builder class for constructing [ImageSaver]s.
 *
 *
 * This class is thread safe.
 *
 * Construct a new ImageSaverBuilder using the given [Context].
 *
 * @param context a [Context] to for accessing the
 * [android.provider.MediaStore].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ImageSaverBuilder(private val mContext: Context) {

    private var mImage: Image? = null
    private var mFile: File? = null
    private var mCaptureResult: CaptureResult? = null
    private var mCharacteristics: CameraCharacteristics? = null
    private var mReader: RefCountedAutoCloseable<ImageReader>? = null

    @Synchronized
    fun setRefCountedReader(
            reader: RefCountedAutoCloseable<ImageReader>?): ImageSaverBuilder {
        if (reader == null) throw NullPointerException()

        mReader = reader
        return this
    }

    @Synchronized
    fun setImage(image: Image?): ImageSaverBuilder {
        if (image == null) throw NullPointerException()
        mImage = image
        return this
    }

    @Synchronized
    fun setFile(file: File?): ImageSaverBuilder {
        if (file == null) throw NullPointerException()
        mFile = file
        return this
    }

    @Synchronized
    fun setResult(result: CaptureResult?): ImageSaverBuilder {
        if (result == null) throw NullPointerException()
        mCaptureResult = result
        return this
    }

    @Synchronized
    fun setCharacteristics(
            characteristics: CameraCharacteristics?): ImageSaverBuilder {
        if (characteristics == null) throw NullPointerException()
        mCharacteristics = characteristics
        return this
    }

    @Synchronized
    fun buildIfComplete(): ImageSaver? {
        return if (!isComplete()) {
            null
        } else ImageSaver(mImage!!, mFile!!, mCaptureResult!!, mCharacteristics!!, mContext,
                mReader!!)
    }

    @Synchronized
    fun getSaveLocation(): String {
        return if (mFile == null) "Unknown" else mFile.toString()
    }

    private fun isComplete(): Boolean {
        return (mImage != null && mFile != null && mCaptureResult != null
                && mCharacteristics != null)
    }
}
