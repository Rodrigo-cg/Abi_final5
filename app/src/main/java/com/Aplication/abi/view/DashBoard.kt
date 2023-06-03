package com.Aplication.abi.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.Aplication.abi.R
import com.google.firebase.auth.FirebaseAuth

class DashBoard : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)
        val cuenta = findViewById(R.id.ASHBOARD) as TextView
        mAuth = FirebaseAuth.getInstance()

        val user = mAuth!!.currentUser
        val email = user?.email

        if (email != null) {
            // Utiliza el valor de 'email' para realizar las acciones necesarias
            // Por ejemplo, mostrarlo en un TextView o utilizarlo en una lógica adicional
            cuenta.text = email
        }
        cuenta.setOnClickListener(View.OnClickListener { view: View? ->salir()

        })
    }
    private fun salir(){
        FirebaseAuth.getInstance().signOut()
         startActivity(Intent(this, LoginActivity::class.java))
        }

}