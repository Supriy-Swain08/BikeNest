package com.example.bikenest

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.bikenest.databinding.ActivityBikeDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.razorpay.*
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

//import javax.mail.*
//import javax.mail.internet.InternetAddress
//import javax.mail.internet.MimeMessage


class Bike_Details : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding1: ActivityBikeDetailsBinding
    val orders=Firebase.firestore.collection("User")
    var orderfor=""
    var myVariable: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding1 = DataBindingUtil.setContentView(this, R.layout.activity_bike_details)



        val detailsvalue = intent.getStringExtra("Details")
        val enginevalue = intent.getStringExtra("Engine")
        val mileagevalue = intent.getStringExtra("Mileage")
        val powervalue = intent.getStringExtra("Power")
        val urlvalue = intent.getStringExtra("URL")
        val modelvalue = intent.getStringExtra("Model")
        val brandvalue = intent.getStringExtra("Brand")
        val pricevalue = intent.getStringExtra("Price")

        binding1.desctv.text = detailsvalue
        binding1.enginetv.text = enginevalue
        binding1.mileageTv.text = mileagevalue
        binding1.powertv.text = powervalue
        binding1.brandnametv.text = brandvalue
        binding1.modelnametv.text = modelvalue
        Glide.with(this)
            .load("$urlvalue")
            .into(binding1.imageView7)
        binding1.backbtn.setOnClickListener {
            finish()
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("User").document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection("Favourites").document("$modelvalue")


        docRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                binding1.changewish.setImageResource(R.drawable.baseline_favorite_24)
            }
        }


        binding1.wishlist.setOnClickListener {
            docRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {

                    binding1.Detailsparent.isEnabled = false
                    Toast.makeText(this, "Already Added", Toast.LENGTH_SHORT).show()

                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.Detailsparent, FavFragment())
                    transaction.commit()

                } else {
                    val Favmodel = hashMapOf(
                        "Name" to modelvalue,
                        "Brand" to brandvalue,
                        "Price" to pricevalue,
                        "Engine" to enginevalue,
                        "Details" to detailsvalue,
                        "Mileage" to mileagevalue,
                        "Power" to powervalue,
                        "Imageurl" to urlvalue,
                        "Added" to 1
                    )

                    Firebase.firestore.collection("User")
                        .document(FirebaseAuth.getInstance().currentUser!!.uid)
                        .collection("Favourites").document("$modelvalue")
                        .set(Favmodel, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(
                                ContentValues.TAG,
                                "DocumentSnapshot successfully written!"
                            )
                            binding1.changewish.setImageResource(R.drawable.baseline_favorite_24)
                            Toast.makeText(this, "Added To Favourite", Toast.LENGTH_SHORT).show()

                        }

                }
            }.addOnFailureListener { e ->


            }

        }

        binding1.desctv.movementMethod = ScrollingMovementMethod()

        binding1.calender.setOnClickListener {
            val today = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                orderfor=dayOfMonth.toString()+"-"+monthOfYear.toString()+"-"+year.toString()
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH+2))
            datePickerDialog.show()
        }

        binding1.review.setOnClickListener {

//            startActivity(Intent(this,Order::class.java))
        }


            binding1.appCompatButton2.setOnClickListener {
                if (orderfor.isNotEmpty()) {
                    myVariable = orderfor
                    paymentprocess(10)
            }else Toast.makeText(this, "Please Select Date\nFor Your Ride", Toast.LENGTH_SHORT).show()
        }

        Checkout.preload(this@Bike_Details)


    }

    private fun paymentprocess(i: Int) {

        val checkout=Checkout()
        checkout.setKeyID("rzp_test_jGFjqGvZEsXCBt")

        try {

            val options=JSONObject()

            options.put("name","Bike_nest")
            options.put("description","Description")
            options.put("currency","INR")
            options.put("amount",i*100)

            options.put("prefill.contact", "9777241724")
            options.put("prefill.email", "supriya.swain2018@gmail.com")

            val retryobject=JSONObject()
            retryobject.put("enabled",true)
            retryobject.put("max_count",4)

            options.put("retry",retryobject)

            checkout.open(this@Bike_Details,options)

        }catch (e:Exception){
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }


    }

    override fun onPaymentSuccess(p0: String?) {
        Toast.makeText(this, "Payment Successful! Payment Id: $p0", Toast.LENGTH_SHORT).show()
        if (p0 != null) {
            addorderdata(p0)
        }
        
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        Toast.makeText(this, "Payment Failed! Error: $p1", Toast.LENGTH_SHORT).show()
    }
    private fun addorderdata(id:String){

        val uuid = UUID.randomUUID().toString().replace("-", "")
        val timestamp = System.currentTimeMillis()

        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        val userChildCollectionRef = orders.document(currentUserID!!).collection("Orders")
        val data = hashMapOf(
            "OrderDate" to "${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}/${Calendar.getInstance().get(Calendar.MONTH)+1}/${Calendar.getInstance().get(Calendar.YEAR)}",
            "OrderedBike" to "${intent.getStringExtra("Model")}",
            "Payment_id" to "${id}",
            "Image_Url" to "${intent.getStringExtra("URL")}",
            "Ordered_For" to "$myVariable"
        )

        val documentId = "$timestamp - $uuid"
        userChildCollectionRef.document(documentId!!).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Ordered Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Don't Worry,Not Placed\nPlease Call Customer Care", Toast.LENGTH_SHORT).show()
            }


    }
}