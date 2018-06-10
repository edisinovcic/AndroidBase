package hr.fer.camera

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import hr.fer.camera.Fragments.PreviewFragment
import hr.fer.camera.surf.SURF
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedInputStream


class MainActivity : AppCompatActivity() {

    lateinit var fragment: PreviewFragment

    val drawerToogle by lazy {
        ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        navigationView.setNavigationItemSelectedListener {
            selectDrawerItem(it)
            true
        }
        button.setOnClickListener {
            onButtonClicked(it)
        }
        drawerLayout.addDrawerListener(drawerToogle)

        fragment = PreviewFragment.newInstance()
        addFragment(fragment)


    }

    fun getLocalAssets(): List<Bitmap> {
        val bookObject = BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo_cropano.jpg")))
        val bookScene = BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo.jpg")))
        return listOf(bookObject, bookScene)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToogle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToogle.onConfigurationChanged(newConfig)
    }

    private fun onButtonClicked(view: View) {
        Toast.makeText(this, "Button clicked", Toast.LENGTH_LONG).show()

        if (!fragment.isCapturing) {
            fragment.isCapturing = true
            fragment.captureImageSession()
            if (fragment.isCapturing) {
               Thread.sleep(1000) //Wait for capture to be completed
            }
            val bitmap: Bitmap = convertImageToBitmap(fragment.latestImage)
            SURF().detect(arrayListOf(getLocalAssets().get(0), bitmap))
            return
        }

        fragment.isCapturing = false
        fragment.previewSession()
    }


    private fun convertImageToBitmap(image: Image): Bitmap {
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

    private fun selectDrawerItem(item: MenuItem) {
        var fragment: Fragment? = null

        //val fragmentClass = TODO("Will be implemented later")
        Toast.makeText(this, "Menu clicked", Toast.LENGTH_LONG).show()
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (drawerToogle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.fragment_menu, menu)
        return false
    }

    private fun addFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }


    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                matrix, true)
    }

}
