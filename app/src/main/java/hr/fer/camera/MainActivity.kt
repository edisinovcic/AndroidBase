package hr.fer.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import hr.fer.camera.Fragments.PreviewFragment
import hr.fer.camera.Helpers.Companion.objectDescriptionKey
import hr.fer.camera.Helpers.Companion.objectKeyPointsKey
import hr.fer.camera.Helpers.Companion.readDescriptorsFromPreferences
import hr.fer.camera.Helpers.Companion.readKeysFromPreferences
import hr.fer.camera.Helpers.Companion.writeDescriptorsToPreferences
import hr.fer.camera.Helpers.Companion.writeObjectKeyToPreferences
import hr.fer.camera.surf.SURF
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.core.MatOfKeyPoint
import java.io.BufferedInputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var fragment: PreviewFragment
        lateinit var assetList: List<Bitmap>
        lateinit var objectsKeyPoints: LinkedList<MatOfKeyPoint>
        lateinit var objectsDescriptors: LinkedList<MatOfKeyPoint>
    }

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

        assetList = getLocalAssets()
    }

    fun getLocalAssets(): List<Bitmap> {
        return listOf(
                BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo0_cropano.jpg"))),
                BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo1_cropano.jpg"))),
                BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo2_cropano.jpg"))),
                BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo3_cropano.jpg")))/*,
                BitmapFactory.decodeStream(BufferedInputStream(assets.open("brojilo4_cropano.jpg")))
        */
        )
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
        ComputeKeypointAndDescriptors().execute()
        //objectsKeyPoints = SURF().getAllObjectsKeypoints(getLocalAssets())
        //objectsDescriptors = SURF().getAllObjectsDescriptors(getLocalAssets(), objectsKeyPoints)
        //objectsKeyPoints = readKeysFromPreferences(objectKeyPointsKey, this)
        //objectsDescriptors = readDescriptorsFromPreferences(objectDescriptionKey, this)
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

    private fun writeToFile() {
        writeToExternalStorage()
    }

    private fun writeToExternalStorage() {
        val fileName = "descriptors.txt"
        val root = File(getExternalStorageDirectory(), fileName)
        if (!root.exists()) {
            root.mkdirs()
            root.createNewFile()
        }


        val fos = this.openFileOutput(fileName, Context.MODE_PRIVATE)
        val os = ObjectOutputStream(fos)

        val data = SURF().getAllObjectsKeypoints(getLocalAssets())

        File(root.path).printWriter().use { out ->
            data.forEach { matrix ->
                matrix.toArray().forEach { keyPoint ->
                    out.write(keyPoint.toString() + "\n")
                }
                out.write("--------------------------------------------------------------------------------------------------------------------\n")
            }
        }

        os.close()
        fos.close()
    }


    private inner class ComputeKeypointAndDescriptors : AsyncTask<String, Int, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            println("Started computing keypoints and descriptors")
        }

        override fun doInBackground(vararg params: String): String {
            objectsKeyPoints = SURF().getAllObjectsKeypoints(getLocalAssets())
            objectsDescriptors = SURF().getAllObjectsDescriptors(getLocalAssets(), objectsKeyPoints)
            return "All Done!"
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            writeObjectKeyToPreferences(applicationContext, objectsKeyPoints, objectKeyPointsKey)
            writeDescriptorsToPreferences(applicationContext, objectsDescriptors, objectDescriptionKey)
            Toast.makeText(applicationContext, "Keypoints and deescriptors for counter have been calculated!", Toast.LENGTH_LONG).show()
        }
    }

}

