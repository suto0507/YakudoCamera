package jp.ac.titech.itpro.sdl.yakudocamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.Image.Plane
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import java.nio.ByteBuffer


class Camera(val context: Context, val cameraId: String) {
    private var captureSession: CameraCaptureSession? = null
    private var camera: CameraDevice? = null
    var imageReader: ImageReader? = null
    var bitmap: Bitmap? = null
    private var MainImageCallback: (() -> Unit)? = null

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image: Image? = imageReader!!.acquireLatestImage()
        if(image!=null) {
            val plane: Plane = image.getPlanes().get(0)
            val buf: ByteBuffer = plane.buffer
            val b = ByteArray(buf.remaining())
            buf[b]
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)

            MainImageCallback?.invoke()

            image.close()
        }

    }
    private val backgroundHandler: Handler = Handler()


    fun init(image_callback: (() -> Unit)) {
        MainImageCallback = image_callback
        imageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG, 2)
        imageReader!!.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
    }

    fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    Log.i("yakudo_camera", "camera onOpened")
                    camera = cam
                    startPreview()
                    Log.i("yakudo_camera", "start camera preview")
                }

                override fun onClosed(cam: CameraDevice) {
                    Log.i("yakudo_camera", "camera onClosed")
                }

                override fun onDisconnected(cam: CameraDevice) {
                    Log.i("yakudo_camera", "onDisconnected")
                    cam.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                }
            }, null)
        }
        catch (ex: SecurityException) {
            throw ex
        }
    }

    fun startPreview() {
        camera!!.createCaptureSession(
                listOf(imageReader!!.getSurface()),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (camera != null) {
                            captureSession = session

                            val request = camera!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            request.addTarget(imageReader!!.getSurface())

                            captureSession!!.setRepeatingRequest(request.build(), null, null)

                            Log.i("yakudo_camera", "setRepeatingRequest")
                        }
                        else {
                            Log.i("yakudo_camera", "onConfigured: camera is null, not starting")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        super.onClosed(session)
                    }
                },
                null)




    }


}