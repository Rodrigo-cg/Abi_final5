package com.Aplication.abi.view

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.Aplication.abi.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {
    private lateinit var googleClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN=100
    private val googleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

    }
    private var txtMail: EditText? = null
    private var txtPassword: TextInputLayout? = null
    private var mAuth: FirebaseAuth? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtMail = findViewById(R.id.txtMail);
        txtPassword = findViewById(R.id.txtPassword);
        val lblRegister = findViewById(R.id.lblRegister) as TextView
        val btnLogin = findViewById(R.id.btnLogin) as Button
        val googlebtn=findViewById(R.id.googleBoton) as Button
        mAuth = FirebaseAuth.getInstance()

        btnLogin.setOnClickListener(View.OnClickListener { view: View? -> userLogin() })

        lblRegister.setOnClickListener(View.OnClickListener { view: View? -> openRegisterActivity() })
        googlebtn.setOnClickListener(View.OnClickListener { view: View? -> googleScreen() })

    }

    private fun googleScreen() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient= GoogleSignIn.getClient(this, googleConf);
        googleClient.signOut();
        startActivityForResult(googleClient.signInIntent,GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==GOOGLE_SIGN_IN){
             val task=GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account:GoogleSignInAccount?=task.getResult(ApiException::class.java)
                if(account!=null){
                    val email=account.email
                    if(email!=null){
                        checkIfEmailExists(email,account)
                    }else{
                        Toast.makeText(this, "No existe", Toast.LENGTH_SHORT).show();

                    }
                   /* val credential=GoogleAuthProvider.getCredential(account.idToken,null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if(it.isSuccessful){
                            Toast.makeText(this, "Logeado con Google", Toast.LENGTH_SHORT).show();

                        }else{
                            showAlert()
                        }
                    }*/

                }
            }catch (e:ApiException){
                showAlert()
            }

        }
    }


    private fun openRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun userLogin() {

        var mail = txtMail!!.text.toString()
        var password = txtPassword!!.editText!!.text.toString()

        if (TextUtils.isEmpty(mail)) {
            txtMail!!.setError("Ingrese un correo");
            txtMail!!.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Ingrese una contraseña", Toast.LENGTH_SHORT).show();
            txtPassword!!.requestFocus();


        } else {
            mAuth?.signInWithEmailAndPassword(mail,password)?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(this, "Bienvenido ", Toast.LENGTH_SHORT).show();

                    val user = mAuth?.currentUser
                    startActivity(Intent(this, DashBoard::class.java))

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        baseContext,
                        "Autentificacion fallida.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
    private fun showAlert(){
        val builder=AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error en la autentificacion")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog=builder.create()
        dialog.show()
    }
    private fun checkIfEmailExists(email: String, account: GoogleSignInAccount) {
        mAuth?.fetchSignInMethodsForEmail(email)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods != null && signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                        Toast.makeText(this, "El correo electronico ya esta registrado", Toast.LENGTH_SHORT).show();

                        // El correo electrónico ya está registrado con una contraseña, mostrar un mensaje de error o tomar la acción apropiada
                    } else {
                        firebaseAuthWithGoogle(account)

                    }
                } else {
                    // Error al verificar si el correo electrónico ya está registrado
                    Toast.makeText(this, "Correo ya registrado con google", Toast.LENGTH_SHORT).show();
                }
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        mAuth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bienvenido cuenta logeada con google", Toast.LENGTH_SHORT).show();

                    // El inicio de sesión con Google fue exitoso, el usuario está registrado en Firebase
                    val user = mAuth?.currentUser
                    // Realiza las acciones necesarias con el usuario registrado
                    startActivity(Intent(this, DashBoard::class.java))

                } else {
                    Toast.makeText(this, "cuenta no registrada con google", Toast.LENGTH_SHORT).show();

                    // Error en el inicio de sesión con Google
                }
            }
    }
}