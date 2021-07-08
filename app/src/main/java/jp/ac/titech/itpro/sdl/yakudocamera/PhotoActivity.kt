package jp.ac.titech.itpro.sdl.yakudocamera



import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import kotlin.math.floor


class PhotoActivity : AppCompatActivity() , SensorEventListener {

    var photo_bitmap_origin:Bitmap? = null;
    var photo_bitmap:Bitmap? = null;
    var photo_view:ImageView? = null;
    var yakudo_flag:Boolean = false;
    var sensor_manager:SensorManager? = null;
    var sensor:Sensor? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        photo_view = findViewById<ImageView>(R.id.photo_view)
        val photo_uri = intent.getParcelableExtra<Uri>("photo_bitmap")


        photo_uri?.let { nonNullUri: Uri ->
            photo_bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, nonNullUri) as? Bitmap

            val matrix = Matrix()
            matrix.postRotate(90.0f)
            photo_bitmap = Bitmap.createBitmap(photo_bitmap!!, 0, 0,
                    photo_bitmap!!.width, photo_bitmap!!.height, matrix, true)
            photo_view!!.setImageBitmap(photo_bitmap)

            photo_bitmap_origin = photo_bitmap!!.copy(photo_bitmap!!.getConfig(), true);
        }

        val button_yakudo = findViewById<Button>(R.id.yakudo_button)
        button_yakudo.setOnClickListener { v: View? ->
            if(yakudo_flag){
                yakudo_flag = false
                button_yakudo.setText("yakudo\n off ")
            }else{
                yakudo_flag = true
                button_yakudo.setText("yakudo\n on ")
            }

        }

        val button_save = findViewById<Button>(R.id.save_button)
        button_save.setOnClickListener { v: View? ->
            save()
        }

        val button_share = findViewById<Button>(R.id.share_button)
        button_share.setOnClickListener { v: View? ->
            share()
        }

        val button_reset = findViewById<Button>(R.id.reset_button)
        button_reset.setOnClickListener { v: View? ->
            photo_bitmap = photo_bitmap_origin!!.copy(photo_bitmap_origin!!.getConfig(), true);
            photo_view!!.setImageBitmap(photo_bitmap)

        }

        val button_retake = findViewById<Button>(R.id.retake_button)
        button_retake.setOnClickListener { v: View? ->
            val intent = Intent(this@PhotoActivity, MainActivity::class.java)
            startActivity(intent)
        }

        sensor_manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if(sensor_manager==null){
            Log.i("yakudo_camera", "no sensor")
        }else{
            sensor = sensor_manager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            if(sensor==null)Log.i("yakudo_camera", "no accelerometer")
        }
    }

    override fun onResume() {
        super.onResume()
        sensor_manager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause(){
        super.onPause()
        sensor_manager!!.unregisterListener(this)
    }



    //Googleとかに保存
    fun share(){
        val ldt = LocalDateTime.now()
        val file = File(externalCacheDir, "yakudo"+ldt.year+ldt.monthValue+ldt.dayOfMonth+ldt.hour+ldt.minute+ldt.second+".png")
        val fOut = FileOutputStream(file)
        photo_bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fOut)
        fOut.flush()
        fOut.close()
        file.setReadable(true, false)

        var uri = FileProvider.getUriForFile(this,applicationContext.packageName + ".fileprovider", file )

        val intent = Intent(Intent.ACTION_SEND)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.type = "image/png"
        startActivity(Intent.createChooser(intent, "Share image via"))
    }

    //画像に保存
    fun save(){
        val ldt = LocalDateTime.now()
        val file = File(externalCacheDir, "yakudo"+ldt.year+ldt.monthValue+ldt.dayOfMonth+ldt.hour+ldt.minute+ldt.second+".png")
        val fOut = FileOutputStream(file)
        photo_bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fOut)
        fOut.flush()
        fOut.close()
        file.setReadable(true, false)


        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put("_data", file.absolutePath)
        }

        contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        Log.i("yakudo_camera", "save picture ")
    }



    fun YakudoEffect(){
        val width: Int = photo_bitmap!!.getWidth()
        val height: Int = photo_bitmap!!.getHeight()

        var pixels = IntArray(width * height)
        var pixels_out = IntArray(width * height)
        //var photo_bitmap_out = Bitmap.createBitmap(photo_bitmap!!.getWidth(), photo_bitmap!!.getHeight(), photo_bitmap!!.config)

        photo_bitmap!!.getPixels(pixels,0,width,0,0,width,height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var pixel = pixels[x + y * width]
                var pixel_nx = 0
                var pixel_ny = 0
                var pixel_nxy = 0
                if (x >= width / 2) {
                    pixel_nx = pixels[x - 1 + y * width]
                } else {
                    pixel_nx = pixels[x + 1 + y * width]
                }
                if (y >= height / 2) {
                    pixel_ny = pixels[x + (y - 1) * width]
                    if (x >= width / 2) {
                        pixel_nxy = pixels[x - 1 + (y - 1) * width]
                    } else {
                        pixel_nxy = pixels[x + 1 + (y - 1) * width]
                    }
                } else {
                    pixel_ny = pixels[x + (y + 1) * width]
                    if (x >= width / 2) {
                        pixel_nxy = pixels[x - 1 + (y + 1) * width]
                    } else {
                        pixel_nxy = pixels[x + 1 + (y + 1) * width]
                    }
                }
                // retrieve color of all channels
                var A = Color.alpha(pixel)
                var R = Color.red(pixel)
                var G = Color.green(pixel)
                var B = Color.blue(pixel)

                var A_nx = Color.alpha(pixel_nx)
                var R_nx = Color.red(pixel_nx)
                var G_nx = Color.green(pixel_nx)
                var B_nx = Color.blue(pixel_nx)

                var A_ny = Color.alpha(pixel_ny)
                var R_ny = Color.red(pixel_ny)
                var G_ny = Color.green(pixel_ny)
                var B_ny = Color.blue(pixel_ny)

                var A_nxy = Color.alpha(pixel_nxy)
                var R_nxy = Color.red(pixel_nxy)
                var G_nxy = Color.green(pixel_nxy)
                var B_nxy = Color.blue(pixel_nxy)

                // take conversion up to one single value

                var x_val = (Math.abs(x - width / 2).toFloat()) / (width / 2)
                var y_val = (Math.abs(y - height / 2).toFloat()) / (height / 2)
                var xy_val = 0.0f

                if ((x - width / 2)==0 && (y - height / 2)==0) {
                    x_val = 0f
                    y_val = 0f
                }else if((x - width / 2)==0){
                    x_val = 0f
                }else if((y - height / 2)==0){
                    y_val = 0f
                }else if(x_val>y_val) {
                    xy_val =  1.41f/(x_val/y_val-1+1.41f) * x_val
                    x_val =  (x_val/y_val-1)/(x_val/y_val-1+1.41f) * x_val
                    y_val = 0f
                }else{
                    xy_val =  1.41f/(y_val/x_val-1+1.41f) * y_val
                    y_val =  (y_val/x_val-1)/(y_val/x_val-1+1.41f) * y_val
                    x_val = 0f
                }


                B = floor(B_nx * x_val + B_ny * y_val + B_nxy * xy_val + B * (1 - x_val - y_val - xy_val)).toInt()
                G = floor(G_nx * x_val + G_ny * y_val + G_nxy * xy_val + G * (1 - x_val - y_val - xy_val)).toInt()
                R = floor(R_nx * x_val + R_ny * y_val + R_nxy * xy_val + R * (1 - x_val - y_val - xy_val)).toInt()

                if(B>255)B=255
                if(G>255)G=255
                if(R>255)R=255
                if(B<0)B=0
                if(G<0)G=0
                if(R<0)R=0

                pixels_out[x + y * width] = Color.argb(A,R,G,B)
            }
        }

        photo_bitmap!!.setPixels(pixels_out, 0, width, 0, 0, width, height)
        photo_view!!.setImageBitmap(photo_bitmap)
        Log.i("yakudo_camera", "end effect")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i("yakudo_camera", "sensor accuracy change")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val acceZ = event.values[2]
        //Log.i("yakudo_camera", "acceZ : " + acceZ)

        if(yakudo_flag && (acceZ>2.5 || acceZ<-2.5)){
            YakudoEffect()
        }
    }

    /*
        fun save(){
        try {
            val extStrageDir: File = Environment.getExternalStorageDirectory()
            val ldt = LocalDateTime.now()
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "yakudo"+ldt.year+ldt.monthValue+ldt.dayOfMonth+ldt.hour+ldt.minute+ldt.second+".png")
            val outStream = FileOutputStream(file)
            photo_bitmap!!.compress(CompressFormat.PNG, 100, outStream)
            outStream.close()
            Log.i("yakudo_camera", "save picture " + Environment.DIRECTORY_PICTURES)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun save2(){
        val values = ContentValues()
        val ldt = LocalDateTime.now()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "yakudo"+ldt.year+ldt.monthValue+ldt.dayOfMonth+ldt.hour+ldt.minute+ldt.second+".jpeg")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.IS_PENDING, 1)

        val resolver = applicationContext.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        //File(collection.toString())
        var dir = getExternalFilesDir(collection.toString())
        if (!dir!!.exists()) {
            val result = dir!!.mkdirs()
            if (result) {
                Log.i("yakudo_camera", "make dir "+collection.toString())
            }
        }
        getDir(collection.toString(), Context.MODE_PRIVATE)

        //Log.i("yakudo_camera", "make dir "+collection.toString())
        val item = resolver.insert(collection, values)

        try {
            contentResolver.openOutputStream(item!!).use { outstream ->
                photo_bitmap!!.compress(CompressFormat.JPEG, 100, outstream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(item!!, values, null, null)
    }
     */

}

