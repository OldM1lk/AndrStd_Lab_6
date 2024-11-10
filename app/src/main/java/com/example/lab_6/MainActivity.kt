package com.example.lab_6

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Timber.plant(Timber.DebugTree())

        val url = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=ff49fcd4d4a08aa6aafb6ea3de826464&tags=cat&format=json&nojsoncallback=1"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val linksList = parsePhotos(url)

                withContext(Dispatchers.Main) {
                    displayImages(linksList)
                }
            }
            catch (e: IOException) {
                Timber.e("Ошибка: ${e.message}")
            }
        }
    }

    private fun parsePhotos(url: String): List<String> {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body
        val json = responseBody?.string()
        val wrapper = Gson().fromJson(json, Wrapper::class.java)
        val linksList = wrapper.photos.photo.map { photo ->
            "https://farm${photo.farm}.staticflickr.com/${photo.server}/${photo.id}_${photo.secret}_z.jpg"
        }
        Timber.i("Links: $linksList")

        wrapper.photos.photo.forEachIndexed { index, photo ->
            if (index % 5 == 0) {
                Timber.d(photo.toString())
            }
        }

        return linksList
    }

    private fun displayImages(linksList: List<String>) {
        val recyclerView: RecyclerView = findViewById(R.id.rView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GridListAdapter(linksList, this)
    }

    fun onImageClick(link: String) {
        val intent = Intent(this, PicViewer::class.java)
        intent.putExtra("picLink", link)
        startActivityForResult(intent, 1)
        Timber.i(link)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val picLink = data?.getStringExtra("picLink")
            val snackbar = Snackbar.make(findViewById(R.id.main), "Картинка добавлена в избранное", Snackbar.LENGTH_LONG)
            snackbar.setAction("Открыть") {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(picLink))
                startActivity(browserIntent)
            }
            snackbar.show()
        }
    }
}