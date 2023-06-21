package com.Aplication.abi.view

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.Aplication.abi.R
import com.Aplication.abi.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage


class RegisterActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private val db: FirebaseFirestore? = null
    private lateinit var binding: ActivityRegisterBinding
    private var userID: String? = null

    lateinit var storageReference: StorageReference
    private var image_url: Uri? = null
    var progressDialog: ProgressDialog? = null
    var storegapath = "perfilimg/*"
    private val COD_SEL_STORAGE = 200
    private val COD_SEL_IMAGE = 300
    private val uploadtask: StorageTask<*>? = null
    private val storageprofileref: StorageReference? = null
    var photo = "photo"
    var idd: String? = null
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 3

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val lblLogin = findViewById(R.id.lblLogin) as TextView

        progressDialog = ProgressDialog(this);

        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        binding.lblLogin.setOnClickListener(View.OnClickListener { view: View? ->startActivity(Intent(this, LoginActivity::class.java));
        })

        binding.btnPhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }

        binding.btnFile.setOnClickListener {
            dispatchPickImageIntent()
        }

        binding.btnRegister.setOnClickListener {

            createuser()

        }

        if (!arePermissionsGranted()) {
            requestPermissions()
        }

    }
    private fun arePermissionsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
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
                        binding.petPhoto.setImageBitmap(imageBitmap)
                        selectedImageUri = saveImageToExternalStorage(imageBitmap)
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    imageUri?.let {
                        binding.petPhoto.setImageURI(imageUri)
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
                Log.d(registerpaciente.TAG, "Imagen subida exitosamente")
            }
            .addOnFailureListener {
                Log.e(registerpaciente.TAG, "Error al subir la imagen: ${it.message}")
            }
    }

private fun subirPhoto(image_url: Uri, userID: String) {
        progressDialog?.setMessage("Actualizando foto")
        progressDialog?.show()
        val rute_storage_photo: String = (storegapath + "" + photo).toString() + "" + (mAuth?.getUid()
             )
        val reference = storageReference.child(rute_storage_photo)
        reference.putFile(image_url).addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful);
            if (uriTask.isSuccessful) {
                uriTask.addOnSuccessListener { uri ->
                    val download_uri = uri.toString()
                    val map = HashMap<String, Any>()
                    map["photo"] = download_uri
                    db?.collection("users")?.document(userID)?.update(map)
                    Toast.makeText(this@RegisterActivity, "Foto actualizada", Toast.LENGTH_SHORT)
                        .show()
                    progressDialog?.dismiss()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(
                this@RegisterActivity,
                "Error al cargar foto",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    fun createuser() {
        var pac_volunt = "paciente"
        pac_volunt = if (binding.pacVolRg.getCheckedRadioButtonId() === R.id.pacienteRB) {
            "paciente"
        } else {
            "voluntario"
        }
        val name: String = binding.txtUser.getText().toString()
        val dnii: String = binding.txtDNI.getText().toString()
        val mail: String = binding.txtMail.getText().toString()
        val phone: String = binding.txtPhone.getText().toString()
        val password: String = binding.txtPassword.getEditText()?.getText().toString()
        if (TextUtils.isEmpty(name)) {
            binding.txtUser.setError("Ingrese un Nombre")
            binding.txtUser.requestFocus()
        } else if (TextUtils.isEmpty(mail)) {
            binding.txtMail.setError("Ingrese un Correo")
            binding.txtMail.requestFocus()
        } else if (TextUtils.isEmpty(phone)) {
            binding.txtPhone.setError("Ingrese un Teléfono")
            binding.txtPhone.requestFocus()
        } else if (TextUtils.isEmpty(password)) {
            binding.txtPassword.setError("Ingrese una Contraseña")
            binding.txtPassword.requestFocus()
        } else {
            val finalPac_volunt = pac_volunt
            mAuth!!.createUserWithEmailAndPassword(mail, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userID = mAuth!!.currentUser!!.uid
                    val documentReference = db!!.collection("users").document(userID!!)
                    val user: MutableMap<String, Any> =
                        HashMap()
                    user["Nombre"] = name
                    user["DNI"] = dnii
                    user["Correo"] = mail
                    user["Teléfono"] = phone
                    user["Contraseña"] = password
                    user["Condicion"] = finalPac_volunt
                    user["latitud"] = ""
                    user["longitud"] = ""
                    documentReference.set(user).addOnSuccessListener {
                        Log.d(
                            "TAG",
                            "onSuccess: Datos registrados$userID"
                        )
                    }
                    Toast.makeText(this@RegisterActivity, "Usuario Registrado", Toast.LENGTH_SHORT)
                        .show()
                    subirPhoto(image_url!!, userID!!)
                    if (finalPac_volunt == "paciente") {
                        mAuth!!.signInWithEmailAndPassword(mail, password)
                        startActivity(Intent(this@RegisterActivity, registerpaciente::class.java))
                    } else startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Usuario no registrado" + task.exception!!.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}