package br.com.oiti.certiface.challenge.camera2

import android.graphics.SurfaceTexture
import android.view.TextureView

class TextureListener(private val onSurfaceTextureAvailableCallback: () -> Unit): TextureView.SurfaceTextureListener {

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        onSurfaceTextureAvailableCallback()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }
}
