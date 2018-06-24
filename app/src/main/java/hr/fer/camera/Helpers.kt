package hr.fer.camera

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.os.AsyncTask
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import hr.fer.camera.Fragments.PreviewFragment
import org.apache.commons.lang3.SerializationUtils
import org.opencv.android.Utils
import org.opencv.core.CvException
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList


class Helpers {

    companion object {

        var fileInput = ArrayList<String>()
        var descriptorsFile: String = "descriptors.txt"

        val objectKeyPointsKey: String = "objectsKeyPoints"
        val objectDescriptionKey: String = "objectsDescriptors"

        public var counterFound = false



        fun convertImageToBitmap(fragment: PreviewFragment): Bitmap {
            val image: Image = fragment.latestImage
            val planes = image.planes
            val buffer = planes[0].buffer
            val offset = 0
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * fragment.MAX_PREVIEW_WIDTH
            // create bitmap

            var bitmap = Bitmap.createBitmap(fragment.MAX_PREVIEW_WIDTH + rowPadding / pixelStride, fragment.MAX_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            val rotation = 90.0
            return rotateImage(bitmap, rotation.toFloat())
        }


        fun rotateImage(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                    matrix, true)
        }

        public fun convertOutputToBitmap(img: Mat): Bitmap? {
            val bmp: Bitmap
            try {
                //Imgproc.cvtColor(img, img, Imgproc.COLOR_GRAY2RGBA);
                bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(img, bmp)
                return bmp
            } catch (e: CvException) {
                Log.e("Exception", e.localizedMessage)
            }

            return null

        }

        /*
        fun populateTextFileWithDescriptors(bitmaps: List<Bitmap>): MutableList<MatOfKeyPoint>? {
            var linesToWrite = SURF().getAllObjectsKeypoints(bitmaps)
            linesToWrite.forEach {
                //println(it)
                var matOfKeyPoint: MatOfKeyPoint = it
                println("---------------------------------------------------------------------")
                matOfKeyPoint.toArray().forEach { println(it) }
                println("---------------------------------------------------------------------")
            }
            return linesToWrite
        }
        */

        /*
        fun storeToFile(fileName: String, linesToWrite: MutableList<MatOfKeyPoint?>) {
            File(fileName).printWriter().use { out ->
                linesToWrite.forEach { line ->
                    out.write(line.toString())
                }
            }
        }
        */

        fun readAllFromFile(fileName: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.lines(Paths.get(fileName)).use { stream -> stream.forEach { fileInput.add(it) } }
            }
        }

        fun checkIfFileExists(fileName: String) {
            val file = File(fileName)
            if (!file.exists()) {
                println("Input file doesn't exist!\n “PATIENCE YOU MUST HAVE my young padawan!")
            }
        }


        fun matToJson(mat: Mat): String {
            val obj = JsonObject()

            if (mat.isContinuous) {
                val cols = mat.cols()
                val rows = mat.rows()
                val elemSize = mat.elemSize().toInt()
                val type = mat.type()

                obj.addProperty("rows", rows)
                obj.addProperty("cols", cols)
                obj.addProperty("type", type)

                // We cannot set binary data to a json object, so:
                // Encoding data byte array to Base64.
                val dataString: String

                val data = IntArray(cols * rows * elemSize)
                mat.get(0, 0, data)
                dataString = String(SerializationUtils.serialize(data))

                obj.addProperty("data", dataString)

                val gson = Gson()

                return gson.toJson(obj)
            } else {
                println("Mat not continuous.")
            }
            return "{}"
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun matFromJson(json: String): Mat {

            val parser = JsonParser()
            val JsonObject = parser.parse(json).getAsJsonObject()

            val rows = JsonObject.get("rows").getAsInt()
            val cols = JsonObject.get("cols").getAsInt()
            val type = JsonObject.get("type").getAsInt()

            val mat = Mat(rows, cols, type)

            val dataString = JsonObject.get("data").getAsString()
            val data = Base64.getDecoder().decode(dataString.toByteArray())
            mat.put(0, 0, data)
            return mat
        }


        public fun writeObjectKeyToPreferences(context: Context, data: LinkedList<MatOfKeyPoint>, key: String) {
            val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val prefsEditor = mPrefs.edit()
            val gson = Gson()
            for ((i, matrix) in data.withIndex()) {
                val json = gson.toJson(matrix)
                prefsEditor.putString(key + i.toString(), json)
            }
            prefsEditor.apply()
        }

        public fun writeDescriptorsToPreferences(context: Context, data: List<MatOfKeyPoint>, key: String) {
            val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val prefsEditor = mPrefs.edit()
            val gson = Gson()
            for ((i, matrix) in data.withIndex()) {
                val json = gson.toJson(matrix)
                prefsEditor.putString(key + i.toString(), json)
            }
            prefsEditor.apply()
        }

        public fun readKeysFromPreferences(key: String, context: Context): LinkedList<MatOfKeyPoint> {
            val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val gson = Gson()
            var data: LinkedList<MatOfKeyPoint> = LinkedList()
            var i = 0
            while (true){
                val json = mPrefs.getString(key + i.toString(), "")
                i++
                val obj = gson.fromJson<MatOfKeyPoint>(json, MatOfKeyPoint::class.java)
                if (obj == null) return data
                data.add(obj)
            }
            return data
        }

        public fun readDescriptorsFromPreferences(key: String, context: Context): LinkedList<MatOfKeyPoint> {
            val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val gson = Gson()
            var data: LinkedList<MatOfKeyPoint> = LinkedList()
            var i = 0
            while(true) {
                val json = mPrefs.getString(key + i.toString(), "")
                i++
                val obj = gson.fromJson<MatOfKeyPoint>(json, MatOfKeyPoint::class.java)
                if (obj == null) return data
                data.add(obj)
            }
            return data
        }
    }
}