package com.Aplication.abi.view

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import de.hdodenhof.circleimageview.CircleImageView
import com.Aplication.abi.R


class RegisterActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private val db: FirebaseFirestore? = null

    lateinit var storageReference: StorageReference
    private var profileImageView: CircleImageView? = null
    private var image_url: Uri? = null
    var progressDialog: ProgressDialog? = null
    var storegapath = "perfilimg/*"
    private val COD_SEL_STORAGE = 200
    private val COD_SEL_IMAGE = 300
    private val uploadtask: StorageTask<*>? = null
    private val storageprofileref: StorageReference? = null
    var photo = "photo"
    var idd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.Aplication.abi.R.layout.activity_register)
        val lblLogin = findViewById(R.id.lblLogin) as TextView

        profileImageView=findViewById(R.id.pet_photo);
        txtUser = findViewById(R.id.txtUser);
        txtMail = findViewById(R.id.txtMail);
        txtPhone = findViewById(R.id.txtPhone);
        txtPassword = findViewById(R.id.txtPassword);
        lblLogin = findViewById(R.id.lblLogin);
        btnRegister = findViewById(R.id.btnRegister);
        creaactu_img=findViewById(R.id.btn_photo);
        r_imag=findViewById(R.id.btn_remove_photo);
        paciente_voluntario=(RadioGroup) findViewById(R.id.pac_vol_rg);
        progressDialog = ProgressDialog(this);

        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

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

    fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    } // End openLoginActivity
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
        pac_volunt = if (paciente_voluntario.getCheckedRadioButtonId() === R.id.pacienteRB) {
            "paciente"
        } else {
            "voluntario"
        }
        val name: String = txtUser.getText().toString()
        val dnii: String = DNI.getText().toString()
        val mail: String = txtMail.getText().toString()
        val phone: String = txtPhone.getText().toString()
        val password: String = txtPassword.getEditText().getText().toString()
        if (TextUtils.isEmpty(name)) {
            txtMail.setError("Ingrese un Nombre")
            txtMail.requestFocus()
        } else if (TextUtils.isEmpty(mail)) {
            txtMail.setError("Ingrese un Correo")
            txtMail.requestFocus()
        } else if (TextUtils.isEmpty(phone)) {
            txtMail.setError("Ingrese un Teléfono")
            txtMail.requestFocus()
        } else if (TextUtils.isEmpty(password)) {
            txtMail.setError("Ingrese una Contraseña")
            txtMail.requestFocus()
        } else {
            val finalPac_volunt = pac_volunt
            mAuth!!.createUserWithEmailAndPassword(mail, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userID = mAuth!!.currentUser!!.uid
                    val documentReference = db!!.collection("users").document(userID)
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
                    subirPhoto(image_url!!, userID)
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