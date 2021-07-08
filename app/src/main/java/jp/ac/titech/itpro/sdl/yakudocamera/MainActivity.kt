package jp.ac.titech.itpro.sdl.yakudocamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    var camera : Camera? = null

    private val PERMISSION_CAMERA = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /// パーミッション許可を取る
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                        PERMISSION_CAMERA)
            }else{
                StartCamera()
            }
        }else{
            StartCamera()
        }

        Log.i("yakudo_camera", "end onCreate")

        val buttonGo1 = findViewById<Button>(R.id.photo_button)
        buttonGo1.setOnClickListener { v: View? ->
            if (camera!!.bitmap!=null) {
                val intent = Intent(this@MainActivity, PhotoActivity::class.java)
                val uri: Uri = bitmapToUri(camera!!.bitmap!!)
                intent.putExtra("photo_bitmap", uri)
                startActivity(intent)
            }
        }
    }

    fun StartCamera(){
        val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList;
        camera = Camera(this, cameraIds[0])
        camera!!.init(this::MainImage)
        camera!!.openCamera()
    }

    fun MainImage(){
        val photoView = findViewById<ImageView>(R.id.photo_view)

        val matrix = Matrix()
        matrix.postRotate(90.0f)
        val rotated_bitmap = Bitmap.createBitmap(camera!!.bitmap!!, 0, 0,
                camera!!.bitmap!!.width, camera!!.bitmap!!.height, matrix, true)

        //photoView.setRotation(90f)
        //photoView.setImageBitmap(camera!!.bitmap)
        photoView.setImageBitmap(rotated_bitmap)



        //photoView.setImageMatrix(matrix);
        //Log.i("yakudo_camera", camera!!.bitmap!!.width.toString() + ", " + camera!!.bitmap!!.height.toString())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.size <= 0) {
            return
        }
        when (requestCode) {
            PERMISSION_CAMERA -> {
                run {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        StartCamera()
                    } else {
                        Toast.makeText(this,
                                "許可が必要です", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                return
            }
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val cacheDir: File = this.cacheDir
        val fileName: String = "bitmapToUri" + ".jpg"
        val file = File(cacheDir, fileName)
        val fileOutputStream: FileOutputStream? = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream?.close()
        val contentSchemaUri: Uri = FileProvider.getUriForFile(this, "jp.ac.titech.itpro.sdl.yakudocamera.fileprovider", file)
        return contentSchemaUri
    }

}