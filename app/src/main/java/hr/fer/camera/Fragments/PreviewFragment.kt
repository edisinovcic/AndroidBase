package hr.fer.camera.Fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.media.ImageReader
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.Toast
import hr.fer.camera.Helpers
import hr.fer.camera.Helpers.Companion.counterFound
import hr.fer.camera.MainActivity
import hr.fer.camera.MainActivity.Companion.fragment
import hr.fer.camera.MainActivity.Companion.objectsDescriptors
import hr.fer.camera.MainActivity.Companion.objectsKeyPoints
import hr.fer.camera.R
import hr.fer.camera.surf.SURF
import kotlinx.android.synthetic.main.fragment_preview.*
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.collections.ArrayList

class PreviewFragment : Fragment() {

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        const val REQUEST_WRITE_STORAGE_REQUEST_CODE = 112
        private val TAG = PreviewFragment::class.qualifiedName
        @JvmStatic
        fun newInstance() = PreviewFragment()
    }

    var point: Point = Point(0, 0)
    private var sensorOrientation = 0
    val MAX_PREVIEW_WIDTH = 1280
    val MAX_PREVIEW_HEIGHT = 720
    lateinit var captureSession: CameraCaptureSession
    lateinit var captureRequestBuilder: CaptureRequest.Builder
    var imageReader: ImageReader? = null
    lateinit var latestImage: Image
    var isCapturing = false


    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        latestImage = it.acquireLatestImage()
        isCapturing = false
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(session: CameraCaptureSession?, request: CaptureRequest?, result: TotalCaptureResult?) {
            Toast.makeText(context, "Image captured!", Toast.LENGTH_LONG).show()

            captureSession.apply {
                stopRepeating()
                abortCaptures()
                capture(captureRequestBuilder.build(), null, null)
                previewSession()
            }

        }
    }


    private lateinit var cameraDevice: CameraDevice
    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice?) {
            Log.d(TAG, "camera device opened")
            if (camera != null) {
                cameraDevice = camera
                previewSession()
            }

        }

        override fun onDisconnected(camera: CameraDevice?) {
            Log.d(TAG, "camera device disconnected")
            camera?.close()
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            Log.d(TAG, "camera device error")
            this@PreviewFragment.activity?.finish()
        }

    }
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun previewSession() {
        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession?) {
                        Log.e(TAG, "creating capture session failded!")
                    }

                    override fun onConfigured(session: CameraCaptureSession?) {
                        if (session != null) {
                            captureSession = session
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        }
                    }

                }, backgroundHandler)
    }

    private fun setupCaptureSession() {
        //val rotation = activity?.windowManager?.defaultDisplay?.rotation


        imageReader = ImageReader.newInstance(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
        }


    }

    fun captureImageSession() {

        setupCaptureSession()

        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val textureSurface = Surface(surfaceTexture)
        val imageSurface = imageReader?.surface

        val surfaces = ArrayList<Surface?>().apply {
            add(textureSurface)
            add(imageSurface)
        }

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(imageSurface)

        cameraDevice.createCaptureSession(surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession?) {
                        Log.e(TAG, "Creating capture session failed!")
                    }

                    override fun onConfigured(session: CameraCaptureSession?) {
                        if (session != null) {
                            captureSession = session
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null)
                        }
                    }

                }, backgroundHandler)
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized)
            captureSession.close()
        if (this::cameraDevice.isInitialized)
            cameraDevice.close()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera thread").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            else -> throw  IllegalArgumentException("Key not recognized")
        }
    }

    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter { lens == cameraCharacteristics(it, CameraCharacteristics.LENS_FACING) }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)
        Log.d(TAG, "deviceId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            openCamera()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(activity!!, Manifest.permission.CAMERA)) {
            Log.d(TAG, "App has camera permission")
            connectCamera()
        } else {
            EasyPermissions.requestPermissions(activity!!,
                    getString(R.string.camera_request_rationale),
                    REQUEST_CAMERA_PERMISSION,
                    Manifest.permission.CAMERA)
        }
        EasyPermissions.requestPermissions(activity!!,
                getString(R.string.abc_action_bar_home_description),
                REQUEST_WRITE_STORAGE_REQUEST_CODE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()
        if (previewTextureView.isAvailable)
            openCamera()
        else
            previewTextureView.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {

        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun openCamera() {
        checkCameraPermission()
    }

    lateinit var bitmaps: List<Bitmap>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_preview, container, false)
        view.setOnTouchListener { v, event ->
            //If focus is triggered again but last was not finished

            if (manualFocusEngaged) {
                true
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                point.set(event.x.toInt(), event.y.toInt())
                manualFocusEngaged = true
                focusOnPoint()
            }
            true
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }


    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }


    //-------------------------------------------------
    // For focus on touch area

    var manualFocusEngaged: Boolean = false
    var focusAreaTouch: MeteringRectangle? = null

    private fun focusOnPoint() {
        var rect: CameraCharacteristics.Key<Rect> = CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
        val halfTouchWidth = 100
        val halfTouchHeight = 100
        focusAreaTouch = MeteringRectangle(
                Math.max(point.x - halfTouchWidth, 0),
                Math.max(point.y - halfTouchHeight, 0),
                halfTouchWidth * 2,
                halfTouchHeight * 2,
                MeteringRectangle.METERING_WEIGHT_MAX - 1
        )

        fragment.isCapturing = true
        fragment.captureImageSession()
        while (fragment.isCapturing);
        val bitmap: Bitmap = Helpers.convertImageToBitmap(MainActivity.fragment)
        bitmaps = MainActivity.assetList
        counterFound = false
        SURFBackground().execute(bitmap)

        //Wait until thread is finished; upgrade to listener
        while (!counterFound);

        var points = SURF.points
        println(points)
        var coef = 0.75f
        rectangle.visibility = View.VISIBLE
        rectangle.top = ((points.get(0).y + points.get(1).y) * coef).toInt()
        rectangle.right = ((points.get(2).x + points.get(3).x) * coef).toInt()
        rectangle.bottom = ((points.get(4).y + points.get(5).y) * coef).toInt()
        rectangle.left = ((points.get(6).x + points.get(7).x) * coef).toInt()

        val img = Mat()
        Utils.bitmapToMat(bitmap, img)
        Core.line(img, points.get(0), points.get(1), Scalar(0.0, 255.0, 255.0), 10)
        Core.line(img, points.get(2), points.get(3), Scalar(0.0, 255.0, 255.0), 10)
        Core.line(img, points.get(4), points.get(5), Scalar(0.0, 255.0, 255.0), 10)
        Core.line(img, points.get(6), points.get(7), Scalar(0.0, 255.0, 255.0), 10)

        val finalImage: Bitmap? = Helpers.convertOutputToBitmap(img)

        manualFocusEngaged = false

        return

    }

    private inner class SURFBackground : AsyncTask<Bitmap, Int, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            println("Started SURF in background")
        }

        override fun doInBackground(vararg inputImage: Bitmap): String? {
            if (SURF().detect(bitmaps, objectsKeyPoints, objectsDescriptors, inputImage.get(0))){
                Helpers.counterFound = true
            }
            return "Surf found!"
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
        }
    }


}