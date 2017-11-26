package br.com.oiti.certiface.challenge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import br.com.oiti.certiface.R
import br.com.oiti.certiface.challenge.camera2.*
import java.io.*
import java.util.*


/**
 * @see https://inducesmile.com/android/android-camera2-api-example-tutorial/
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Activity : AppCompatActivity() {


    private val takePictureButton by lazy { findViewById<Button>(R.id.btn_takepicture) }
    private val textureView by lazy { findViewById<TextureView>(R.id.texture) }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)

        textureView.surfaceTextureListener = textureListener
        takePictureButton.setOnClickListener({ takePicture() })
    }

    override fun onResume() {
        super.onResume()

        if (textureView.isAvailable) {
            openFrontFacingCamera()
        } else {
            textureView.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        Log.e(TAG, "onPause")
        //closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                printToast("Sorry!!!, you can't use this app without granting permission")
                finish()
            }
        }
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

    private fun takePicture() {

        cameraDevice ?: run {
            Log.e(TAG, "cameraDevice is null")
            return
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            var jpegSizes: Array<Size>? = null
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG)
            }
            var width = 640
            var height = 480

            if (jpegSizes?.isNotEmpty() == true) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }

            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(textureView!!.surfaceTexture))

            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            // Orientation
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))


            val imageReaderListener = ImageReaderListener({
                reader.acquireLatestImage().use { image ->
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    save(bytes)
                }
            })
            reader.setOnImageAvailableListener(imageReaderListener, mBackgroundHandler)

            val imageCaptureListener = CameraCaptureSessionCaptureCallback({ createCameraPreview() })
            val captureSessionStateCallback = CameraCaptureSessionStateCallback({ it.capture(captureBuilder.build(), imageCaptureListener, mBackgroundHandler) })

            cameraDevice?.createCaptureSession(outputSurfaces, captureSessionStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun save(bytes: ByteArray) {
        val file = File(Environment.getExternalStorageDirectory().path + "/pic.jpg")
        var output: OutputStream?  = null
        try {
            output = FileOutputStream(file)
            output.write(bytes)
        } finally {
            output?.close()
        }
    }
    
    private fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!

            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)

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

            cameraDevice?.createCaptureSession(Arrays.asList(surface), cameraCaptureSessionStateCallback, null)
            } catch (e: CameraAccessException) {
                 e.printStackTrace()
            }
    }

    private fun openFrontFacingCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraId = getFrontFacingCameraId()
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

        imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
        // Add permission for camera and let user grant the permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@Camera2Activity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
            return
        }
        manager.openCamera(cameraId, stateCallback, null)
    }


    private fun getFrontFacingCameraId(): String? {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId)

        manager.cameraIdList.forEach {
            // We don't use a front facing camera in this sample.
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return it
            }
        }

        return null
    }

    private fun updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return")
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions?.setRepeatingRequest(captureRequestBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun printToast(text: String, e: Throwable? = null) {
        val context = applicationContext
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()

        e?.let { Log.d(TAG, text + ": " + e.message, e) }
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
        private var TAG = this::class.java.name
        private val REQUEST_CAMERA_PERMISSION = 200
        private val ORIENTATIONS = SparseIntArray()
    }

}
