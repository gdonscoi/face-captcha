package br.com.oiti.certiface.challenge.test

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Created by bzumpano on 25/11/17.
 */
/**
 * Runnable that saves an [Image] into the specified [File], and updates
 * [android.provider.MediaStore] to include the resulting file.
 *
 *
 * This can be constructed through an [ImageSaverBuilder] as the necessary image and
 * result information becomes available.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ImageSaver(
        private val mImage: Image,
        private val mFile: File,
        private val mCaptureResult: CaptureResult,
        private val mCharacteristics: CameraCharacteristics,
        private val mContext: Context,
        private val mReader: RefCountedAutoCloseable<ImageReader>): Runnable {

    private val TAG = this.javaClass.name

    override fun run() {
        var success = false
        val format = mImage.format
        when (format) {
            ImageFormat.JPEG -> {
                val buffer = mImage.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var output: FileOutputStream? = null
                try {
                    output = FileOutputStream(mFile)
                    output.write(bytes)
                    success = true
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    mImage.close()
                    closeOutput(output)
                }
            }
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(mCharacteristics, mCaptureResult)
                var output: FileOutputStream? = null
                try {
                    output = FileOutputStream(mFile)
                    dngCreator.writeImage(output, mImage)
                    success = true
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    mImage.close()
                    closeOutput(output)
                }
            }
            else -> {
                Log.e(TAG, "Cannot save image, unexpected image format:" + format)
            }
        }

        // Decrement reference count to allow ImageReader to be closed to free up resources.
        mReader.close()

        // If saving the file succeeded, update MediaStore.
        if (success) {
            MediaScannerConnection.scanFile(mContext, arrayOf(mFile.path), null, object : MediaScannerConnection.MediaScannerConnectionClient {
                override fun onMediaScannerConnected() {
                    // Do nothing
                }

                override fun onScanCompleted(path: String, uri: Uri) {
                    Log.i(TAG, "Scanned $path:")
                    Log.i(TAG, "-> uri=" + uri)
                }
            })/*mimeTypes*/
        }
    }

    /**
     * Cleanup the given [OutputStream].
     *
     * @param outputStream the stream to close.
     */
    private fun closeOutput(outputStream: OutputStream?) {
        if (null != outputStream) {
            try {
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}
