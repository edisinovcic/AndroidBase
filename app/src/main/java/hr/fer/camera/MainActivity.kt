package hr.fer.camera

import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.opencv.highgui.Highgui
import org.opencv.core.Mat



class MainActivity : AppCompatActivity() {

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


        val fragment = PreviewFragment.newInstance()
        addFragment(fragment)


    }

    fun getLocalAssets(): List<Bitmap> {
        val bookObject = BitmapFactory.decodeStream(BufferedInputStream(assets.open("bookobject.jpg")))
        val bookScene = BitmapFactory.decodeStream(BufferedInputStream(assets.open("bookscene.jpg")))
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
        //TODO: add behaviour for on button click

        SURF().detect(getLocalAssets())



    }

    private fun selectDrawerItem(item: MenuItem) {
        var fragment: Fragment? = null

//        val fragmentClass = TODO("Will be implemented later")
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


}
