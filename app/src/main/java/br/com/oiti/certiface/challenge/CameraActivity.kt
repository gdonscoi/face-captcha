package br.com.oiti.certiface.challenge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.OrientationEventListener
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import br.com.oiti.certiface.R
import br.com.oiti.certiface.challenge.test.*
import kotlinx.android.synthetic.main.activity_challenge.*
import kotlinx.android.synthetic.main.challenge_view.*
import kotlinx.android.synthetic.main.feedback_animation.*
import kotlinx.android.synthetic.main.initial_view.*
import kotlinx.android.synthetic.main.loading_view.*
import kotlinx.android.synthetic.main.result_view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, CameraContract.View {

    private var TAG = this.javaClass.name

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    private var preview: FrameLayout? = null

    private lateinit var presenter: CameraPresenter
    private lateinit var endpoint: String
    private lateinit var appKey: String
    private lateinit var userParams: String


    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * Request code for camera permissions.
     */
    private val REQUEST_CAMERA_PERMISSIONS = 1

    /**
     * Permissions required to take a picture.
     */
    private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * Camera state: Device is closed.
     */
    private val STATE_CLOSED = 0

    /**
     * Camera state: Device is opened, but is not capturing.
     */
    private val STATE_OPENED = 1

    /**
     * Camera state: Showing camera preview.
     */
    private val STATE_PREVIEW = 2

    /**
     * Camera state: Waiting for 3A convergence before capturing a photo.
     */
    private val STATE_WAITING_FOR_3A_CONVERGENCE = 3

    /**
     * Timeout for the pre-capture sequence.
     */
    private val PRECAPTURE_TIMEOUT_MS: Long = 1000

    /**
     * Tolerance when comparing aspect ratios.
     */
    private val ASPECT_RATIO_TOLERANCE = 0.005

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_WIDTH = 1920

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_HEIGHT = 1080


    /**
     * An additional thread for running tasks that shouldn't block the UI.  This is used for all
     * callbacks from the [CameraDevice] and [CameraCaptureSession]s.
     */
    private lateinit var mBackgroundThread: HandlerThread

    // *********************************************************************************************
    // State protected by mCameraStateLock.
    //
    // The following state is used across both the UI and background threads.  Methods with "Locked"
    // in the name expect mCameraStateLock to be held while calling.

    /**
     * ID of the current [CameraDevice].
     */
    private lateinit var mCameraId: String

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var mCaptureSession: CameraCaptureSession? = null

    /**
     * A lock protecting camera state.
     */
    private val mCameraStateLock = Any()

    /**
     * A reference to the open [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * A reference counted holder wrapping the [ImageReader] that handles JPEG image
     * captures. This is used to allow us to clean up the [ImageReader] when all background
     * tasks using its [Image]s have completed.
     */
    private var mJpegImageReader: RefCountedAutoCloseable<ImageReader>? = null

    /**
     * A reference counted holder wrapping the [ImageReader] that handles RAW image captures.
     * This is used to allow us to clean up the [ImageReader] when all background tasks using
     * its [Image]s have completed.
     */
    private var mRawImageReader: RefCountedAutoCloseable<ImageReader>? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * JPEG image is ready to be saved.
     */
    private val mOnJpegImageAvailableListener = ImageReader.OnImageAvailableListener { dequeueAndSaveImage(mJpegResultQueue, mJpegImageReader) }

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * RAW image is ready to be saved.
     */
    private val mOnRawImageAvailableListener = ImageReader.OnImageAvailableListener { dequeueAndSaveImage(mRawResultQueue, mRawImageReader) }

    /**
     * Request ID to [ImageSaverBuilder] mapping for in-progress JPEG captures.
     */
    private val mJpegResultQueue = TreeMap<Int, ImageSaverBuilder>()

    /**
     * Request ID to [ImageSaverBuilder] mapping for in-progress RAW captures.
     */
    private val mRawResultQueue = TreeMap<Int, ImageSaverBuilder>()

    /**
     * The [CameraCharacteristics] for the currently configured camera device.
     */
    private var mCharacteristics: CameraCharacteristics? = null

    /**
     * The state of the camera device.
     *
     * @see .mPreCaptureCallback
     */
    private var mState = STATE_CLOSED

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private var mTextureView: AutoFitTextureView? = null

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var mPreviewRequestBuilder: CaptureRequest.Builder

    /**
     * Whether or not the currently configured camera device is fixed-focus.
     */
    private var mNoAFRun = false

    /**
     * Number of pending user requests to capture a photo.
     */
    private var mPendingUserCaptures = 0

    /**
     * Timer to use with pre-capture sequence to ensure a timely capture if 3A convergence is
     * taking too long.
     */
    private var mCaptureTimer: Long = 0

    /**
     * A counter for tracking corresponding [CaptureRequest]s and [CaptureResult]s
     * across the [CameraCaptureSession] capture callbacks.
     */
    private val mRequestCounter = AtomicInteger()

    /**
     * An [OrientationEventListener] used to determine when device rotation has occurred.
     * This is mainly necessary for when the device is rotated by 180 degrees, in which case
     * onCreate or onConfigurationChanged is not called as the view dimensions remain the same,
     * but the orientation of the has changed, and thus the preview rotation must be updated.
     */
    private var mOrientationListener: OrientationEventListener? = null

    //**********************************************************************************************

    /**
     * [CameraDevice.StateCallback] is called when the currently active [CameraDevice]
     * changes its state.
     */
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here if
            // the TextureView displaying this has been set up.
            synchronized(mCameraStateLock) {
                mState = STATE_OPENED
                mCameraOpenCloseLock.release()
                mCameraDevice = cameraDevice

                // Start the preview session if the TextureView has been set up already.
                if (mPreviewSize != null && mTextureView!!.isAvailable) {
                    createCameraPreviewSessionLocked()
                }
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            synchronized(mCameraStateLock) {
                mState = STATE_CLOSED
                mCameraOpenCloseLock.release()
                cameraDevice.close()
                mCameraDevice = null
            }
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Log.e(TAG, "Received camera device error: " + error)
            synchronized(mCameraStateLock) {
                mState = STATE_CLOSED
                mCameraOpenCloseLock.release()
                cameraDevice.close()
                mCameraDevice = null
            }
            val activity = this@CameraActivity
            if (null != activity) {
                activity!!.finish()
            }
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events for the preview and
     * pre-capture sequence.
     */
    private val mPreCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            synchronized(mCameraStateLock) {
                when (mState) {
                    STATE_PREVIEW -> {
                    }// We have nothing to do when the camera preview is running normally.
                    STATE_WAITING_FOR_3A_CONVERGENCE -> {
                        var readyToCapture = true
                        if (!mNoAFRun) {
                            val afState = result.get(CaptureResult.CONTROL_AF_STATE) ?: return@synchronized

                            // If auto-focus has reached locked state, we are ready to capture
                            readyToCapture = afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                        }

                        // If we are running on an non-legacy device, we should also wait until
                        // auto-exposure and auto-white-balance have converged as well before
                        // taking a picture.
                        if (!isLegacyLocked()) {
                            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                            val awbState = result.get(CaptureResult.CONTROL_AWB_STATE)
                            if (aeState == null || awbState == null) {
                                return@synchronized
                            }

                            readyToCapture = readyToCapture &&
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED &&
                                    awbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED
                        }

                        // If we haven't finished the pre-capture sequence but have hit our maximum
                        // wait timeout, too bad! Begin capture anyway.
                        if (!readyToCapture && hitTimeoutLocked()) {
                            Log.w(TAG, "Timed out waiting for pre-capture sequence to complete.")
                            readyToCapture = true
                        }

                        if (readyToCapture && mPendingUserCaptures > 0) {
                            // Capture once for each user tap of the "Picture" button.
                            while (mPendingUserCaptures > 0) {
                                captureStillPictureLocked()
                                mPendingUserCaptures--
                            }
                            // After this, the camera will go back to the normal state of preview.
                            mState = STATE_PREVIEW
                        }
                    }
                }
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles the still JPEG and RAW capture
     * request.
     */
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest,
                                      timestamp: Long, frameNumber: Long) {
            val currentDateTime = generateTimestamp()
            val rawFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "RAW_$currentDateTime.dng")
            val jpegFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "JPEG_$currentDateTime.jpg")

            // Look up the ImageSaverBuilder for this request and update it with the file name
            // based on the capture start time.
            var jpegBuilder: ImageSaverBuilder? = null
            var rawBuilder: ImageSaverBuilder? = null
            val requestId = request.tag as Int
            synchronized(mCameraStateLock) {
                jpegBuilder = mJpegResultQueue[requestId]
                rawBuilder = mRawResultQueue[requestId]
            }

            jpegBuilder?.setFile(jpegFile)
            rawBuilder?.setFile(rawFile)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest,
                                        result: TotalCaptureResult) {
            val requestId = request.tag as Int
            var jpegBuilder: ImageSaverBuilder?
            var rawBuilder: ImageSaverBuilder?
            val sb = StringBuilder()

            // Look up the ImageSaverBuilder for this request and update it with the CaptureResult
            synchronized(mCameraStateLock) {
                jpegBuilder = mJpegResultQueue[requestId]
                rawBuilder = mRawResultQueue[requestId]

                if (jpegBuilder != null) {
                    jpegBuilder!!.setResult(result)
                    sb.append("Saving JPEG as: ")
                    sb.append(jpegBuilder!!.getSaveLocation())
                }
                if (rawBuilder != null) {
                    rawBuilder!!.setResult(result)
                    if (jpegBuilder != null) sb.append(", ")
                    sb.append("Saving RAW as: ")
                    sb.append(rawBuilder!!.getSaveLocation())
                }

                // If we have all the results necessary, save the image to a file in the background.
                handleCompletionLocked(requestId, jpegBuilder, mJpegResultQueue)
                handleCompletionLocked(requestId, rawBuilder, mRawResultQueue)

                finishedCaptureLocked()
            }

            printToast(sb.toString())
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest,
                                     failure: CaptureFailure) {
            val requestId = request.tag as Int
            synchronized(mCameraStateLock) {
                mJpegResultQueue.remove(requestId)
                mRawResultQueue.remove(requestId)
                finishedCaptureLocked()
            }
            printToast("Capture failed!")
        }

    }

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events of a
     * [TextureView].
     */
    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            synchronized(mCameraStateLock) {
                mPreviewSize = null
            }
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        endpoint = intent.getStringExtra(PARAM_ENDPOINT)
        appKey = intent.getStringExtra(PARAM_APP_KEY)
        userParams = intent.getStringExtra(PARAM_USER_INFO)

        button_start.setOnClickListener { presenter.start(userParams) }
    }

    public override fun onResume() {
        super.onResume()
        startBackgroundThread()
        openCamera()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we should
        // configure the preview bounds here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView!!.isAvailable) {
            configureTransform(mTextureView!!.getWidth(), mTextureView!!.getHeight())
        } else {
            mTextureView?.setSurfaceTextureListener(mSurfaceTextureListener)
        }
        if (mOrientationListener != null && mOrientationListener!!.canDetectOrientation()) {
            mOrientationListener?.enable()
        }
    }

    public override fun onPause() {
        if (mOrientationListener != null) {
            mOrientationListener?.disable()
        }
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showMissingPermissionError()
                    return
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun initialView() {
        runOnUiThread {
            initialContainer.visibility = View.VISIBLE
            visibilityAnimationFeedback(View.GONE, "")
            iconField.setImageBitmap(null)
            messageField.setImageBitmap(null)
            counterField.text = ""
        }
    }

    override fun startChallenge() {
        runOnUiThread {
            initialContainer.visibility = View.GONE
            visibilityChallengeContainer(View.VISIBLE)
            loadingContainer.visibility = View.GONE
            feedbackAnimationContainer.visibility = View.GONE
        }
    }

    override fun takePicture(callback: Camera.PictureCallback) {
        mCamera?.takePicture(null, null, callback)
    }

    override fun loadIcon(icon: Bitmap?) {
        runOnUiThread {
            iconField.setImageBitmap(icon)
        }
    }

    override fun setMessage(message: Bitmap?) {
        runOnUiThread {
            messageField.setImageBitmap(message)
        }
    }

    override fun setCounter(count: String) {
        runOnUiThread {
            counterField.text = count
        }
    }

    override fun loadingView() {
        runOnUiThread {
            visibilityChallengeContainer(View.GONE)
            loadingContainer.visibility = View.VISIBLE
        }
    }

    override fun finishChallenge(valid: Boolean) {
        val data = Intent()

        data.putExtra(PARAM_ACTIVITY_RESULT, valid)
        setResult(RESULT_OK, data)

        finish()
    }

    override fun animationFeedback(visibility: Int, message: String) {
        runOnUiThread {
            visibilityChallengeContainer(View.GONE)
            loadingContainer.visibility = View.GONE
            visibilityAnimationFeedback(visibility, message)
        }
    }

    /**
     * Closes the current [CameraDevice].
     */
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            synchronized(mCameraStateLock) {

                // Reset state and clean up resources used by the camera.
                // Note: After calling this, the ImageReaders will be closed after any background
                // tasks saving Images from these readers have been completed.
                mPendingUserCaptures = 0
                mState = STATE_CLOSED
                if (null != mCaptureSession) {
                    mCaptureSession?.close()
                    mCaptureSession = null
                }
                if (null != mCameraDevice) {
                    mCameraDevice?.close()
                    mCameraDevice = null
                }
                if (null != mJpegImageReader) {
                    mJpegImageReader?.close()
                    mJpegImageReader = null
                }
                if (null != mRawImageReader) {
                    mRawImageReader?.close()
                    mRawImageReader = null
                }
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        synchronized(mCameraStateLock) {
            mBackgroundHandler = Handler(mBackgroundThread.getLooper())
        }
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
            synchronized(mCameraStateLock) {
                mBackgroundHandler = null
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun visibilityAnimationFeedback(visibility: Int, message: String) {
        feedbackAnimationContainer.visibility = visibility
        resultContainer.visibility = visibility
        textAnimation.text = message
        textResult.text = message
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun visibilityChallengeContainer(visibility: Int) {
        challengeContainer.visibility = visibility
        iconField.visibility = visibility
    }

    /**
     * Opens the camera specified by [.mCameraId].
     */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (!setUpCameraOutputs()) {
            return
        }
        if (!hasAllPermissionsGranted()) {
            requestCameraPermissions()
            return
        }

        val activity = this@CameraActivity
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for any previously running session to finish.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            var cameraId: String
            var backgroundHandler: Handler
            synchronized(mCameraStateLock) {
                cameraId = mCameraId
                backgroundHandler = mBackgroundHandler!!

                // Attempt to open the camera. mStateCallback will be called on the background handler's
                // thread when this succeeds or fails.
                manager.openCamera(cameraId, mStateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    /**
     * Sets up state related to camera that is needed before opening a [CameraDevice].
     */
    private fun setUpCameraOutputs(): Boolean {
        val activity = this@CameraActivity

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (manager == null) {
//            ErrorDialog.buildErrorDialog("This device doesn't support Camera2 API.").show(fragmentManager, "dialog")
            printToast("This device doesn't support Camera2 API.")
            return false
        }
        try {
            // Find a CameraDevice that supports RAW captures, and configure state.
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We only use a camera that supports RAW in this sample.
                if (!contains(characteristics.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                    continue
                }

                val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                // For still image captures, we use the largest available size.
                val largestJpeg = Collections.max(
                        Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea())

                val largestRaw = Collections.max(
                        Arrays.asList(*map.getOutputSizes(ImageFormat.RAW_SENSOR)),
                        CompareSizesByArea())

                synchronized(mCameraStateLock) {
                    // Set up ImageReaders for JPEG and RAW outputs.  Place these in a reference
                    // counted wrapper to ensure they are only closed when all background tasks
                    // using them are finished.
                    if (mJpegImageReader == null || mJpegImageReader!!.getAndRetain() == null) {
                        mJpegImageReader = RefCountedAutoCloseable<ImageReader>(
                                ImageReader.newInstance(largestJpeg.width,
                                        largestJpeg.height, ImageFormat.JPEG, /*maxImages*/5))
                    }
                    mJpegImageReader!!.get()!!.setOnImageAvailableListener(
                            mOnJpegImageAvailableListener, mBackgroundHandler)

                    if (mRawImageReader == null || mRawImageReader!!.getAndRetain() == null) {
                        mRawImageReader = RefCountedAutoCloseable<ImageReader>(
                                ImageReader.newInstance(largestRaw.width,
                                        largestRaw.height, ImageFormat.RAW_SENSOR, /*maxImages*/ 5))
                    }
                    mRawImageReader!!.get()!!.setOnImageAvailableListener(
                            mOnRawImageAvailableListener, mBackgroundHandler)

                    mCharacteristics = characteristics
                    mCameraId = cameraId
                }
                return true
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        // If we found no suitable cameras for capturing RAW, warn the user.
//        ErrorDialog.buildErrorDialog("This device doesn't support capturing RAW photos").show(fragmentManager, "dialog")
        printToast("This device doesn't support capturing RAW photos")
        return false
    }

    /**
     * Tells whether all the necessary permissions are granted to this app.
     *
     * @return True if all the required permissions are granted.
     */
    private fun hasAllPermissionsGranted(): Boolean {
        for (permission in CAMERA_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this@CameraActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Requests permissions necessary to use camera and save pictures.
     */
    private fun requestCameraPermissions() {
        if (shouldShowRationale()) {
            PermissionConfirmationDialog.newInstance().show(this@CameraActivity.supportFragmentManager, "dialog")
        } else {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSIONS)
        }
    }

    /**
     * Shows that this app really needs the permission and finishes the app.
     */
    private fun showMissingPermissionError() {
        val activity = this@CameraActivity
        if (activity != null) {
            Toast.makeText(activity, R.string.request_permission, Toast.LENGTH_SHORT).show()
            activity!!.finish()
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting the permissions.
     *
     * @return True if the UI should be shown.
     */
    private fun shouldShowRationale(): Boolean {
        for (permission in CAMERA_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@CameraActivity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * Retrieve the next [Image] from a reference counted [ImageReader], retaining
     * that [ImageReader] until that [Image] is no longer in use, and set this
     * [Image] as the result for the next request in the queue of pending requests.  If
     * all necessary information is available, begin saving the image to a file in a background
     * thread.
     *
     * @param pendingQueue the currently active requests.
     * @param reader       a reference counted wrapper containing an [ImageReader] from which
     * to acquire an image.
     */
    private fun dequeueAndSaveImage(pendingQueue: TreeMap<Int, ImageSaverBuilder>,
                                    reader: RefCountedAutoCloseable<ImageReader>?) {
        synchronized(mCameraStateLock) {
            val entry = pendingQueue.firstEntry()
            val builder = entry.value

            // Increment reference count to prevent ImageReader from being closed while we
            // are saving its Images in a background thread (otherwise their resources may
            // be freed while we are writing to a file).
            if (reader == null || reader.getAndRetain() == null) {
                Log.e(TAG, "Paused the activity before we could save the image," + " ImageReader already closed.")
                pendingQueue.remove(entry.key)
                return
            }

            val image: Image
            try {
                image = reader.get()!!.acquireNextImage()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Too many images queued for saving, dropping image for request: " + entry.key)
                pendingQueue.remove(entry.key)
                return
            }

            builder.setRefCountedReader(reader).setImage(image)

            handleCompletionLocked(entry.key, builder, pendingQueue)
        }
    }

    /**
     * If the given request has been completed, remove it from the queue of active requests and
     * send an [ImageSaver] with the results from this request to a background thread to
     * save a file.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     *
     * @param requestId the ID of the [CaptureRequest] to handle.
     * @param builder   the [ImageSaver.ImageSaverBuilder] for this request.
     * @param queue     the queue to remove this request from, if completed.
     */
    private fun handleCompletionLocked(requestId: Int, builder: ImageSaverBuilder?,
                                       queue: TreeMap<Int, ImageSaverBuilder>) {
        if (builder == null) return
        val saver = builder!!.buildIfComplete()
        if (saver != null) {
            queue.remove(requestId)
            AsyncTask.THREAD_POOL_EXECUTOR.execute(saver!!)
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     */
    private fun createCameraPreviewSessionLocked() {
        try {
            val texture = mTextureView!!.surfaceTexture
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice!!.createCaptureSession(Arrays.asList(surface,
                    mJpegImageReader!!.get()!!.getSurface(),
                    mRawImageReader!!.get()!!.getSurface()), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    synchronized(mCameraStateLock) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return
                        }

                        try {
                            setup3AControlsLocked(mPreviewRequestBuilder)
                            // Finally, we start displaying the camera preview.
                            cameraCaptureSession.setRepeatingRequest(
                                    mPreviewRequestBuilder.build(),
                                    mPreCaptureCallback, mBackgroundHandler)
                            mState = STATE_PREVIEW
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                            return
                        } catch (e: IllegalStateException) {
                            e.printStackTrace()
                            return
                        }

                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    printToast("Failed to configure camera.")
                }
            }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Configure the given [CaptureRequest.Builder] to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     *
     * @param builder the builder to configure.
     */
    private fun setup3AControlsLocked(builder: CaptureRequest.Builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO)

        val minFocusDist = mCharacteristics!!.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = minFocusDist == null || minFocusDist.equals(0)

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCharacteristics!!.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO)
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (contains(mCharacteristics!!.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON)
        }

        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCharacteristics!!.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
    }

    /**
     * Check if we are using a device that only supports the LEGACY hardware level.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     *
     * @return true if this is a legacy device.
     */
    private fun isLegacyLocked(): Boolean {
        return mCharacteristics!!.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

    /**
     * Check if the timer for the pre-capture sequence has been hit.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     *
     * @return true if the timeout occurred.
     */
    private fun hitTimeoutLocked(): Boolean {
        return SystemClock.elapsedRealtime() - mCaptureTimer > PRECAPTURE_TIMEOUT_MS
    }

    /**
     * Send a capture request to the camera device that initiates a capture targeting the JPEG and
     * RAW outputs.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     */
    private fun captureStillPictureLocked() {
        try {
            val activity = this@CameraActivity
            if (null == activity || null == mCameraDevice) {
                return
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

            captureBuilder.addTarget(mJpegImageReader!!.get()!!.getSurface())
            captureBuilder.addTarget(mRawImageReader!!.get()!!.getSurface())

            // Use the same AE and AF modes as the preview.
            setup3AControlsLocked(captureBuilder)

            // Set orientation.
            val rotation = activity!!.getWindowManager().getDefaultDisplay().getRotation()
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    sensorToDeviceRotation(mCharacteristics!!, rotation))

            // Set request tag to easily track results in callbacks.
            captureBuilder.setTag(mRequestCounter.getAndIncrement())

            val request = captureBuilder.build()

            // Create an ImageSaverBuilder in which to collect results, and add it to the queue
            // of active requests.
            val jpegBuilder = ImageSaverBuilder(activity)
                    .setCharacteristics(mCharacteristics)
            val rawBuilder = ImageSaverBuilder(activity)
                    .setCharacteristics(mCharacteristics)

            mJpegResultQueue.put(request.tag as Int, jpegBuilder)
            mRawResultQueue.put(request.tag as Int, rawBuilder)

            mCaptureSession!!.capture(request, mCaptureCallback, mBackgroundHandler)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     *
     * @param c                 the [CameraCharacteristics] to query for the camera sensor
     * orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     * orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    private fun sensorToDeviceRotation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation)

        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    /**
     * Generate a string containing a formatted timestamp with the current date and time.
     *
     * @return a [String] representing a time.
     */
    private fun generateTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Called after a RAW/JPEG capture has completed; resets the AF trigger state for the
     * pre-capture sequence.
     *
     *
     * Call this only with [.mCameraStateLock] held.
     */
    private fun finishedCaptureLocked() {
        try {
            // Reset the auto-focus trigger in case AF didn't run quickly enough.
            if (!mNoAFRun) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)

                mCaptureSession!!.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler)

                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Configure the necessary [android.graphics.Matrix] transformation to `mTextureView`,
     * and start/restart the preview capture session if necessary.
     *
     *
     * This method should be called after the camera state has been initialized in
     * setUpCameraOutputs.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = this@CameraActivity
        synchronized(mCameraStateLock) {
            if (null == mTextureView || null == activity) {
                return
            }

            val map = mCharacteristics!!.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // For still image captures, we always use the largest available size.
            val largestJpeg = Collections.max(Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea())

            // Find the rotation of the device relative to the native device orientation.
            val deviceRotation = activity!!.getWindowManager().getDefaultDisplay().getRotation()
            val displaySize = Point()
            activity!!.getWindowManager().getDefaultDisplay().getSize(displaySize)

            // Find the rotation of the device relative to the camera sensor's orientation.
            val totalRotation = sensorToDeviceRotation(mCharacteristics!!, deviceRotation)

            // Swap the view dimensions for calculation as needed if they are rotated relative to
            // the sensor.
            val swappedDimensions = totalRotation == 90 || totalRotation == 270
            var rotatedViewWidth = viewWidth
            var rotatedViewHeight = viewHeight
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y

            if (swappedDimensions) {
                rotatedViewWidth = viewHeight
                rotatedViewHeight = viewWidth
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }

            // Preview should not be larger than display size and 1080p.
            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT
            }

            // Find the best preview size for these view dimensions and configured JPEG size.
            val previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedViewWidth, rotatedViewHeight, maxPreviewWidth, maxPreviewHeight,
                    largestJpeg)

            if (swappedDimensions) {
                mTextureView?.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth())
            } else {
                mTextureView?.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight())
            }

            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            val rotation = if (mCharacteristics!!.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                (360 + ORIENTATIONS.get(deviceRotation)) % 360
            else
                (360 - ORIENTATIONS.get(deviceRotation)) % 360

            val matrix = Matrix()
            val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            val bufferRect = RectF(0f, 0f, previewSize.getHeight().toFloat(), previewSize.getWidth().toFloat())
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()

            // Initially, output stream images from the Camera2 API will be rotated to the native
            // device orientation from the sensor's orientation, and the TextureView will default to
            // scaling these buffers to fill it's view bounds.  If the aspect ratios and relative
            // orientations are correct, this is fine.
            //
            // However, if the device orientation has been rotated relative to its native
            // orientation so that the TextureView's dimensions are swapped relative to the
            // native device orientation, we must do the following to ensure the output stream
            // images are not incorrectly scaled by the TextureView:
            //   - Undo the scale-to-fill from the output buffer's dimensions (i.e. its dimensions
            //     in the native device orientation) to the TextureView's dimension.
            //   - Apply a scale-to-fill from the output buffer's rotated dimensions
            //     (i.e. its dimensions in the current device orientation) to the TextureView's
            //     dimensions.
            //   - Apply the rotation from the native device orientation to the current device
            //     rotation.
            if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                val scale = Math.max(
                        viewHeight.toFloat() / previewSize.getHeight(),
                        viewWidth.toFloat() / previewSize.getWidth())
                matrix.postScale(scale, scale, centerX, centerY)

            }
            matrix.postRotate(rotation.toFloat(), centerX, centerY)

            mTextureView?.setTransform(matrix)

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(previewSize, mPreviewSize!!)) {
                mPreviewSize = previewSize
                if (mState != STATE_CLOSED) {
                    createCameraPreviewSessionLocked()
                }
            }
        }
    }

    /**
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     * class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int,
                                  textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea())
        } else if (notBigEnough.size > 0) {
            return Collections.max(notBigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            return choices[0]
        }
    }

    /**
     * Return true if the two given [Size]s have the same aspect ratio.
     *
     * @param a first [Size] to compare.
     * @param b second [Size] to compare.
     * @return true if the sizes have the same aspect ratio, otherwise false.
     */
    private fun checkAspectsEqual(a: Size, b: Size): Boolean {
        val aAspect = a.width / a.height.toDouble()
        val bAspect = b.width / b.height.toDouble()
        return Math.abs(aAspect - bAspect) <= ASPECT_RATIO_TOLERANCE
    }

    private fun printToast(text: String, e: Throwable? = null) {
        val context = applicationContext
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()

        if (e != null) {
            Log.d(TAG, text + ": " + e.message, e)
        }
    }

    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    private fun contains(modes: IntArray?, mode: Int): Boolean {
        if (modes == null) {
            return false
        }
        for (i in modes) {
            if (i == mode) {
                return true
            }
        }
        return false
    }

    companion object {

        val PARAM_APP_KEY = "app_key"
        val PARAM_ENDPOINT = "endpoint"
        val PARAM_USER_INFO = "user_info"
        val PARAM_ACTIVITY_RESULT = "certiface_result"

        private val REQUEST_CAMERA_PERMISSION = 1
    }
}
