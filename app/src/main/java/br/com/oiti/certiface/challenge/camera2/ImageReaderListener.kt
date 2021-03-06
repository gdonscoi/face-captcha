package br.com.oiti.certiface.challenge.camera2

import android.media.ImageReader
import android.os.Build
import android.support.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.KITKAT)
class ImageReaderListener(private val onImageAvailableCallback: (reader: ImageReader) -> Unit): ImageReader.OnImageAvailableListener {

    override fun onImageAvailable(reader: ImageReader) {
        onImageAvailableCallback(reader)
    }
}
