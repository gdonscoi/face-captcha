package br.com.oiti.certiface.challenge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import br.com.oiti.certiface.challenge.camera2.CameraCaptureSessionStateCallback
import br.com.oiti.certiface.challenge.camera2.CameraDeviceStateCallback
import br.com.oiti.certiface.challenge.camera2.ImageReaderListener
import br.com.oiti.certiface.challenge.camera2.TextureListener
import kotlinx.android.synthetic.main.challenge_fragment.*
import java.util.*


/**
 * @see <a href="https://www.youtube.com/watch?v=Xtp3tH27OFs" />
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Fragment: AbstractChallengeFragment() {

    private val backgroundHandler: Handler
    private val backgroundThread: HandlerThread

    private val cameraPreview by lazy { TextureView(activity) }
    private val cameraManager by lazy { activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var imageSize: Size? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var reader: ImageReader? = null

    private var captureSurface: Surface? = null
    private var previewSurface: Surface? = null

    private val textureListener = TextureListener({ openFrontFacingCamera() })

    private val stateCallback = CameraDeviceStateCallback(
            {camera ->
                cameraDevice = camera
                createCameraPreview()
            },
            { cameraDevice?.close() },
            {_, _ ->
                cameraDevice?.close()
                cameraDevice = null
            })


    override fun getCameraPreview(): View? = cameraPreview

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraPreview.surfaceTextureListener = textureListener
    }

    override fun onResume() {
        super.onResume()

        if (hasCameraRequirements()) {
            cameraFrameLayout.addView(cameraPreview)

            if (cameraPreview.isAvailable) {
                openFrontFacingCamera()
            } else {
                cameraPreview.surfaceTextureListener = textureListener
            }
        }
    }

    override fun buildTakePictureHandler(photos: HashMap<ByteArray, String>, afterTakePicture: (data: ByteArray) -> Unit): Any {
        val handler = ImageReaderListener({
            it.acquireLatestImage().use { image ->
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.capacity())

                buffer.get(data)
                image.close()
                afterTakePicture(data)
            }
        })

        return handler
    }

    override fun takePicture(callback: Any) {
        val handler = callback as ImageReaderListener

        reader?.setOnImageAvailableListener(handler, backgroundHandler)

        cameraCaptureSession?.capture(captureRequestBuilder!!.build(), null, null)
    }

    override fun getFrontFacingCameraId(): String? {
        cameraManager.cameraIdList.forEach {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return it
            }
        }

        return null
    }

    override fun releaseCamera() {
        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreview() {
        setupHandlers()

        val cameraCaptureSessionStateCallback = CameraCaptureSessionStateCallback({ session ->
            cameraDevice?.let {
                this@Camera2Fragment.cameraCaptureSession = session
                updatePreview()
            }
        })

        cameraDevice!!.createCaptureSession(Arrays.asList(previewSurface, captureSurface), cameraCaptureSessionStateCallback, null)
    }

    private fun setupHandlers() {
        cameraDevice?.let {
            val texture = cameraPreview.surfaceTexture
            texture.setDefaultBufferSize(imageSize!!.width, imageSize!!.height)

            previewSurface = Surface(texture)

            previewRequestBuilder = it.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(previewSurface)

            captureSurface = reader?.surface
            captureRequestBuilder = it.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder!!.addTarget(captureSurface)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openFrontFacingCamera() {
        cameraId = getFrontFacingCameraId()
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

//        imageSize = map.getOutputSizes(SurfaceTexture::class.java)[0]
        map.getOutputSizes(ImageFormat.JPEG)[0]?.let {
            imageSize = it
            reader = ImageReader.newInstance(it.width, it.height, ImageFormat.JPEG, 1)
            captureSurface = reader!!.surface
        }

        cameraManager.openCamera(cameraId, stateCallback, null)
    }

    private fun updatePreview() {
        previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, backgroundHandler)
    }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        backgroundThread = HandlerThread(this::javaClass.name)
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    companion object {
        private val ORIENTATIONS = SparseIntArray()
    }
}
