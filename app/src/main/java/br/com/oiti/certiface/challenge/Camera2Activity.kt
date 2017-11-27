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
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import br.com.oiti.certiface.challenge.camera2.*
import java.util.*


/**
 * @see <a href="https://inducesmile.com/android/android-camera2-api-example-tutorial/" />
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Activity : AbstractChallengeActivity() {

    private val cameraPreview by lazy { TextureView(this@Camera2Activity) }

    private val mBackgroundHandler: Handler
    private val mBackgroundThread: HandlerThread

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPreview.surfaceTextureListener = textureListener
    }

    override fun onResume() {
        super.onResume()

        if (hasCameraRequirements()) {
            preview.addView(cameraPreview)

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
                Log.d(TAG, "Callback invoked at " + Date().time)
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

        reader?.setOnImageAvailableListener(handler, mBackgroundHandler)

        cameraCaptureSession?.capture(captureRequestBuilder!!.build(), null, null)
    }

    override fun getFrontFacingCameraId(): String? {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        manager.cameraIdList.forEach {
            val characteristics = manager.getCameraCharacteristics(it)
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
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
//            mBackgroundThread = null
//            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreview() {
        setupHandlers()

        val cameraCaptureSessionStateCallback = CameraCaptureSessionStateCallback({ session ->
            cameraDevice?.let {
                // When the session is ready, we start displaying the preview.
                this@Camera2Activity.cameraCaptureSession = session
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
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraId = getFrontFacingCameraId()
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

//        imageSize = map.getOutputSizes(SurfaceTexture::class.java)[0]
        map.getOutputSizes(ImageFormat.JPEG)[0]?.let {
            imageSize = it
            reader = ImageReader.newInstance(it.width, it.height, ImageFormat.JPEG, 1)
            captureSurface = reader!!.surface
        }

        manager.openCamera(cameraId, stateCallback, null)
    }

    private fun updatePreview() {
        previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, mBackgroundHandler)
    }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    companion object {
        private val ORIENTATIONS = SparseIntArray()
    }

}
