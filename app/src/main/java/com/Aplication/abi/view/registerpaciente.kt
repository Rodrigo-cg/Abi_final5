package com.Aplication.abi.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import com.Aplication.abi.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream

class registerpaciente : AppCompatActivity() {


    private lateinit var profileImageView: ImageView
    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private lateinit var uploadButton: Button
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registerpaciente)

        profileImageView = findViewById(R.id.profileImageView2)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        uploadButton = findViewById(R.id.uploadButton)

        cameraButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        galleryButton.setOnClickListener {
            dispatchPickImageIntent()
        }

        uploadButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                val circularImageUri = getCircularImageUri(uri)
                if (circularImageUri != null) {
                    uploadImage(circularImageUri)
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun dispatchPickImageIntent() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? android.graphics.Bitmap
                    imageBitmap?.let {
                        profileImageView.setImageBitmap(imageBitmap)
                        selectedImageUri = saveImageToExternalStorage(imageBitmap)
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    imageUri?.let {
                        profileImageView.setImageURI(imageUri)
                        selectedImageUri = imageUri
                    }
                }
            }
        }
    }

    private fun saveImageToExternalStorage(image: android.graphics.Bitmap): Uri? {
        val imagesFolder = getExternalFilesDir(null)
        val imageFile = java.io.File(imagesFolder, "profile_image.png")
        val imageUri = Uri.fromFile(imageFile)

        try {
            val outputStream = java.io.FileOutputStream(imageFile)
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return imageUri
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error al guardar la imagen: ${e.message}")
        }

        return null
    }

    private fun uploadImage(imageUri: Uri) {
        val storageRef = Firebase.storage.reference
        val imagesRef = storageRef.child("perfilimag")
        val profileImageRef = imagesRef.child("profile_image.png")

        profileImageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Imagen subida exitosamente")
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al subir la imagen: ${it.message}")
            }
    }
    private fun getCircularImageUri(imageUri: Uri): Uri? {
        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

        // Verificar si la imagen proviene de la cámara frontal
        val isFrontCameraImage = isFrontCameraImage(imageUri)

        // Invertir horizontalmente si es una imagen de la cámara frontal
        val transformedBitmap = if (isFrontCameraImage) {
            val matrix = Matrix()
            matrix.setScale(-1f, 1f)
            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        } else {
            originalBitmap
        }

        val circularBitmap = Bitmap.createBitmap(transformedBitmap.width, transformedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circularBitmap)
        val shader = BitmapShader(transformedBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint().apply {
            isAntiAlias = true
            this.shader = shader
        }
        val radius = transformedBitmap.width.coerceAtMost(transformedBitmap.height) / 2.toFloat()
        canvas.drawCircle(transformedBitmap.width / 2.toFloat(), transformedBitmap.height / 2.toFloat(), radius, paint)

        // Utilizar Glide para cargar la imagen invertida en profileImageView
        Glide.with(this)
            .load(circularBitmap)
            .apply(RequestOptions.circleCropTransform())
            .into(profileImageView)

        return saveImageToExternalStorage(circularBitmap)
    }
    private fun isFrontCameraImage(imageUri: Uri): Boolean {
        val cursor = contentResolver.query(imageUri, arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val orientation = cursor.getInt(0)
                return orientation == 180
            }
        }
        return false
    }
}