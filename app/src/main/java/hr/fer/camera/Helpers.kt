package hr.fer.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import hr.fer.camera.Fragments.PreviewFragment
import org.opencv.android.Utils
import org.opencv.core.CvException
import org.opencv.core.Mat

class Helpers {

    companion object {

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

    }
}