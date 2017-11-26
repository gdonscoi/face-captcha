package br.com.oiti.certiface.challenge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
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
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var imageDimension: Size? = null

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

    override fun buildTakePictureCallback(photos: HashMap<ByteArray, String>, afterTakePicture: (data: ByteArray) -> Unit): Any {
        val callback = ImageReaderListener({
            it.acquireLatestImage().use { image ->
                Log.d(TAG, "Callback invoked at " + Date().time)
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.capacity())

                buffer.get(data)
                afterTakePicture(data)
            }
        })

        return callback
    }

    override fun takePicture(callback: Any) {
        val imageReaderListener = callback as ImageReaderListener
        val reader = getImageReader()
        val outputSurfaces = ArrayList<Surface>()
        outputSurfaces.add(reader.surface)
        outputSurfaces.add(Surface(cameraPreview.surfaceTexture))

        val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(reader.surface)
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        // Orientation
        val rotation = windowManager.defaultDisplay.rotation
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

        reader.setOnImageAvailableListener(imageReaderListener, mBackgroundHandler)

        val imageCaptureListener = CameraCaptureSessionCaptureCallback({_, _ -> createCameraPreview() })
        val captureSessionStateCallback = CameraCaptureSessionStateCallback({ it.capture(captureBuilder.build(), imageCaptureListener, mBackgroundHandler) })

        cameraDevice!!.createCaptureSession(outputSurfaces, captureSessionStateCallback, mBackgroundHandler)
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

    private fun getImageReader(): ImageReader {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
        var jpegSizes: Array<Size>? = null
        var width = 640
        var height = 480

        characteristics?.let {
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG)
        }

        if (jpegSizes?.isNotEmpty() == true) {
            width = jpegSizes!![0].width
            height = jpegSizes!![0].height
        }

//        return ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
        return ImageReader.newInstance(imageDimension!!.width, imageDimension!!.height, ImageFormat.JPEG, 1)
    }

    private fun createCameraPreview() {
        val texture = cameraPreview.surfaceTexture

        texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
        val surface = Surface(texture)
        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder!!.addTarget(surface)

        val cameraCaptureSessionStateCallback = object: CameraCaptureSession.StateCallback(){

            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                cameraDevice?.let {
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession
                    updatePreview()
                }
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                printToast("Configuration change")
            }
        }

        cameraDevice!!.createCaptureSession(Arrays.asList(surface), cameraCaptureSessionStateCallback, null)
    }

    @SuppressLint("MissingPermission")
    private fun openFrontFacingCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraId = getFrontFacingCameraId()
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

        imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]

        manager.openCamera(cameraId, stateCallback, null)
    }

    private fun updatePreview() {
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
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
