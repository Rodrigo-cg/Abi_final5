package com.Aplication.abi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.Aplication.abi.R
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import de.hdodenhof.circleimageview.CircleImageView


class RegisterActivity : AppCompatActivity() {
    lateinit var storageReference: StorageReference
    private val profileImageView: CircleImageView? = null
    private var image_url: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val lblLogin = findViewById(R.id.lblLogin) as TextView
        storageReference = FirebaseStorage.getInstance().getReference();

        lblLogin.setOnClickListener(View.OnClickListener { view: View? ->startActivity(Intent(this, LoginActivity::class.java));
        })

        profileImageView!!.setOnClickListener {
            val pick = true
            if (pick == true) {
                if (!checkCameraPermission()) {
                    requestCameraPermission()
                } else PickImage()
            } else {
                if (!checkStoragePermission()) {
                    requestStoragePermission()
                } else PickImage()
            }
        }

    }
    private fun PickImage() {
        CropImage.activity().start(this)
    }

    private fun requestStoragePermission() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
    }
    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 100
        )
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkCameraPermission(): Boolean {
        val res1 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val res2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return res1 && res2
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                image_url = result.uri
                Picasso.get().load(image_url).into(profileImageView)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }




}