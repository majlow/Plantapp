package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapter.phoToAdapter
import com.example.myapplication.Adapter.plantTypeAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import android.widget.SearchView
import android.widget.Toast
import com.example.myapplication.model.listSpeciesData
import com.example.myapplication.model.*
import kotlin.system.exitProcess

class Home : AppCompatActivity() {
    private lateinit var dbref : DatabaseReference
    private lateinit var userRecyclerview : RecyclerView
    private lateinit var Recyclerview1 : RecyclerView
    private lateinit var arrayList : ArrayList<plantTypeData>
    private lateinit var arrayList1 : ArrayList<phoToData>
    private lateinit var searchView: SearchView
    private lateinit var userArrayList : ArrayList<listSpeciesData>
    private var backButtonPressedTime: Long = 0
    private val backButtonThreshold: Long = 2000 // 2 seconds
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.home
        val specie= findViewById<Button>(R.id.specise)
        val btnprofile=findViewById<CircleImageView>(R.id.profile_image)
        val addingnew=findViewById<Button>(R.id.adding_new)
        searchView = findViewById(R.id.searchView)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
        addingnew.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 1)
        }

        profile()
        btnprofile.setOnClickListener{
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            finish()
        }

        specie.setOnClickListener{
            startActivity(Intent(this, Home_Species::class.java))
            finish()
        }

        val article= findViewById<Button>(R.id.articles)
        article.setOnClickListener{
            startActivity(Intent(this, Home_Articles::class.java))
            finish()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.person -> {
                    val intent = Intent(this, Profile::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    true
                }
                else -> false
            }
        }
        userRecyclerview = findViewById(R.id.RecyclerView)
        userRecyclerview.layoutManager = LinearLayoutManager(this,RecyclerView.HORIZONTAL,false)
        userRecyclerview.setHasFixedSize(true)
        arrayList = ArrayList()
        arrayList = arrayListOf<plantTypeData>()
        getUserData()
        Recyclerview1 = findViewById(R.id.RecyclerView1)
        Recyclerview1.layoutManager = LinearLayoutManager(this,RecyclerView.HORIZONTAL,false)
        Recyclerview1.setHasFixedSize(true)
        arrayList1 = ArrayList()
        arrayList1 = arrayListOf<phoToData>()
        getUserData1()
    }

    private fun getUserData() {
        dbref = FirebaseDatabase.getInstance().getReference("Plants Types")
        dbref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (userSnapshot in snapshot.children){
                        val planttypedata = userSnapshot.getValue(plantTypeData::class.java)
                        arrayList.add(planttypedata!!)
                    }
                    userRecyclerview.adapter = plantTypeAdapter(this@Home, arrayList)
                }

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }
    private fun getUserData1() {
        dbref = FirebaseDatabase.getInstance().getReference("Photography")
        dbref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (userSnapshot in snapshot.children){
                        val photoypedata = userSnapshot.getValue(phoToData::class.java)
                        arrayList1.add(photoypedata!!)
                    }
                    Recyclerview1.adapter = phoToAdapter(this@Home, arrayList1)
                }

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun profile() {
        val image=findViewById<CircleImageView>(R.id.profile_image)
        val tvname=findViewById<TextView>(R.id.tvname)
        val users = FirebaseAuth.getInstance().currentUser ?: return

        users?.let {
            for (profile in it.providerData) {
                // Name, email address, and profile photo Url
                val name = profile.displayName
                val eemail = profile.email
                val photoUrl = profile.photoUrl

                if (name == null) {
                    tvname.text = "Hello!"
                } else {
                    tvname.text = "Hello " + name
                }

                Glide.with(this@Home).load(photoUrl).error(R.drawable.icon_app)
                    .into(image)
                val userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .orderByChild("email")
                    .equalTo(eemail)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (ds in dataSnapshot.children) {
                            val user = ds.getValue(userData::class.java)
                            if (user != null) {
                                tvname.text = "Hello " + user?.fullName
                                Glide.with(this@Home).load(user?.imageAvt)
                                    .error(R.drawable.icon_app)
                                    .into(image)
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            // Create PATH in FireStorage
            val storageRef = Firebase.storage.reference
            val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            // Convert bitmap to byte array for uploading picture to FireStorage
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Upload image to FireStorage
            val uploadTask = imagesRef.putBytes(data)

            // Tracking the result of event upload
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, Home_AddingNew::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (backButtonPressedTime + backButtonThreshold > System.currentTimeMillis()) {
            // Double-press detected, exit the app
            super.onBackPressed()
            exitProcess(0) // Terminate the app process
        } else {
            Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show()
            backButtonPressedTime = System.currentTimeMillis()
        }
    }

}