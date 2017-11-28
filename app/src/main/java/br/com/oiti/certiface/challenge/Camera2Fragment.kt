package br.com.oiti.certiface.challenge

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Size
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
 * Uses new camera2 api
 *
 * @see <a href="https://www.youtube.com/watch?v=Xtp3tH27OFs" />
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Fragment: AbstractChallengeFragment() {

    private val cameraPreview by lazy { TextureView(activity) }
    private val cameraManager by lazy { activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    private lateinit var reader: ImageReader
    private lateinit var captureSurface: Surface
    private var previewSurface: Surface? = null

    private val textureListener = TextureListener({ openFrontFacingCamera() })

    private val stateCallback = CameraDeviceStateCallback(
            { startCameraDevice(it!!) },
            { stopCameraDevice() },
            {_, _ -> stopCameraDevice() })


    override fun getCameraPreview(): View? = cameraPreview

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraPreview.surfaceTextureListener = textureListener
    }

    override fun onResume() {
        super.onResume()
        initPreviewComponents()

        if (hasCameraRequirements()) {
            cameraFrameLayout.addView(cameraPreview)

            if (cameraPreview.isAvailable) {
                openFrontFacingCamera()
            } else {
                cameraPreview.surfaceTextureListener = textureListener
            }
        }
    }

    override fun buildTakePictureHandler(
            photos: HashMap<ByteArray, String>,
            afterTakePicture: (data: ByteArray) -> Unit): Any = ImageReaderListener({
        it.acquireLatestImage().use { image ->
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.capacity())

            buffer.get(data)
            image.close()
            afterTakePicture(data)
        }
    })

    override fun takePicture(callback: Any) {
        val handler = callback as ImageReaderListener

        reader.setOnImageAvailableListener(handler, backgroundHandler)

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
        stopCameraDevice()
        stopPreviewComponents()
    }

    private fun startCameraDevice(camera: CameraDevice) {
        cameraDevice = camera
        createCameraPreview()
    }

    private fun stopCameraDevice() {
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun initPreviewComponents() {
        reader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, 1)
        captureSurface = reader.surface
    }

    private fun stopPreviewComponents() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null

        reader.close()
        captureSurface.release()

        previewSurface?.release()
        previewSurface = null
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
            createPreviewRequestHandler(it)
            createCaptureRequestHandler(it)
        }
    }

    private fun createPreviewRequestHandler(cameraDevice: CameraDevice) {
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder!!.addTarget(previewSurface)
    }

    private fun createCaptureRequestHandler(cameraDevice: CameraDevice) {
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder!!.addTarget(captureSurface)

        val rotation = getRotation(activity)
        captureRequestBuilder!!.set(CaptureRequest.JPEG_ORIENTATION, rotation)
    }

    @SuppressLint("MissingPermission")
    private fun openFrontFacingCamera() {
        cameraId = getFrontFacingCameraId()

        createPreviewSurface()

        cameraManager.openCamera(cameraId, stateCallback, null)
    }

    private fun createPreviewSurface() {
        val texture = cameraPreview.surfaceTexture
        val size = getBestSupportedImageSize()

        texture.setDefaultBufferSize(size.width, size.height)
        previewSurface = Surface(texture)
    }

    private fun updatePreview() {
        previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, backgroundHandler)
    }

    private fun getBestSupportedImageSize(): Size {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val pixelsCount = IMAGE_WIDTH * IMAGE_HEIGHT

        map.getOutputSizes(IMAGE_FORMAT).forEach { size ->
            val currentPixelsCount = size.width * size.height

            if (currentPixelsCount < pixelsCount) {
                return size
            }
        }

        return Size(IMAGE_WIDTH, IMAGE_HEIGHT)
    }
}
