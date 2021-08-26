package com.unava.dia.lightplay.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.RequestManager
import com.unava.dia.lightplay.R
import com.unava.dia.lightplay.other.AppConstants.ACTION_START_SERVICE
import com.unava.dia.lightplay.other.AppConstants.ACTION_STOP_SERVICE
import com.unava.dia.lightplay.service.PlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                Intent(this, PlayService::class.java).also { _ ->
                    sendCommandToService(ACTION_START_SERVICE, uri)
                }
            }
        btnStop.setOnClickListener {
            Intent(this, PlayService::class.java).also { _ ->
                sendCommandToService(ACTION_STOP_SERVICE, null)
            }
        }
        btnOpen.setOnClickListener {
            getContent.launch("*/*")
        }
    }

    private fun sendCommandToService(action: String, uri: Uri?) =
        Intent(this, PlayService::class.java).also {
            it.action = action
            it.putExtra("URI", uri.toString())
            startService(it)
        }
}