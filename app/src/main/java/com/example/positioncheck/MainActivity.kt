package com.example.positioncheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 1
    private val FASTEST_INTERVAL: Long = 1
    lateinit var mLastLocation: Location
    internal lateinit var mLoc: LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10

    lateinit var btnStart: Button
    lateinit var btnStop: Button
    lateinit var txtLat: TextView
    lateinit var txtLong: TextView
    lateinit var txtTime: TextView
    lateinit var txtStat: TextView
    lateinit var edtRange: EditText
    var range : Double = 0.0
    var setOld=false
    var start=false
    lateinit var oldLocation:Location


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.positioncheck.R.layout.activity_main)
        mLoc = LocationRequest()
        btnStart = findViewById(com.example.positioncheck.R.id.btnStart)
        btnStop = findViewById(com.example.positioncheck.R.id.btnStop)
        txtLat = findViewById(com.example.positioncheck.R.id.txtLat);
        txtLong = findViewById(com.example.positioncheck.R.id.txtLong);
        txtTime = findViewById(com.example.positioncheck.R.id.txtTime);
        txtStat = findViewById(com.example.positioncheck.R.id.txtStat);
        edtRange = findViewById(com.example.positioncheck.R.id.edtRange);

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
        if (checkPermissionForLocation( this)){
            startLocationUpdates()
        }

        btnStart.setOnClickListener {
            if (edtRange.text.toString().length<=0){
                Toast.makeText(this, "Area range can't be zero or empty", Toast.LENGTH_SHORT).show()
            } else{
                range= edtRange.text.toString().toDouble()
                if ( range < 1.0 ||edtRange.text.toString().length<=0){
                    Toast.makeText(this, "Area range can't be zero or empty", Toast.LENGTH_SHORT).show()
                } else{
                    btnStart.isEnabled = false
                    btnStop.isEnabled = true
                    edtRange.isEnabled= false
                    start= true
                }
            }

        }

        btnStop.setOnClickListener {
            stoplocationUpdates()
            txtTime.text = "Updates Stoped"
            setOld=false
            start= false
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            edtRange.isEnabled= true
            reset(this)

        }

    }

    fun reset(context: Activity) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        if (context is Activity) {
            (context as Activity).finish()
        }
        Runtime.getRuntime().exit(0)
    }
    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Allow access to GPS?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()


    }


    protected fun startLocationUpdates() {
                // Create the location request to start receiving updates

        mLoc!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLoc!!.setInterval(INTERVAL)
        mLoc!!.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLoc!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLoc, mLocationCallback,
            Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
            if (setOld == false && start==true){
                setOldLoc(locationResult.lastLocation)
                setOld=true

            }
        }
    }
    fun setOldLoc(location: Location){
        oldLocation= location
    }
    @SuppressLint("ResourceAsColor")
    fun onLocationChanged(location: Location) {
        // New location has now been determined

        mLastLocation = location
        val date: Date = Calendar.getInstance().time
        val sdf = SimpleDateFormat("hh:mm:ss a")
        txtTime.text = "Updated at : " + sdf.format(date)
        txtLat.text = "LATITUDE : " + mLastLocation.latitude
        txtLong.text = "LONGITUDE : " + mLastLocation.longitude

        if (setOld== true){
            var distance=calculateDistance(oldLocation,mLastLocation).toFloat()
            if ( distance.toDouble() <= range){
                txtStat.setText("You are still inside the area.\n Distance from starting point:"+distanceText(distance))
            } else{
                txtStat.setText("You are leaving the area.\n Distance from starting point:"+distanceText(distance))
                txtStat.setTextColor(R.color.colorred)

            }
        }

    }
    fun calculateDistance(oldloc: Location, newLoc:Location): Double {

        val lat1=oldloc.latitude
        val lng1= oldloc.longitude
        val lat2=newLoc.latitude
        val lng2=newLoc.longitude
//        Location.distanceBetween(lat1, lng1, lat2, lng2, results)

        val _LatRes = lat1 - lat2
        val _longRes = lng1 - lng2
        val R = 6371000f; // Radius of the earth in m
        val dLat = _LatRes * Math.PI / 180f
        val dLon = _longRes * Math.PI / 180f
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(lat1 * Math.PI / 180f) * Math.cos(lat2 * Math.PI / 180f) * Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2f * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        val dist= R*c
        // distance in meter
        return dist
    }
    fun distanceText(distance: Float): String {
        val distanceString: String

        if (distance < 1000)
            if (distance < 1)
                distanceString = String.format(Locale.US, "%dm", 1)
            else
                distanceString = String.format(Locale.US, "%dm", Math.round(distance))
        else if (distance > 10000)
            if (distance < 1000000)
                distanceString = String.format(Locale.US, "%dkm", Math.round(distance / 1000))
            else
                distanceString = "FAR"
        else
            distanceString = String.format(Locale.US, "%.2fkm", distance / 1000)

        return distanceString
    }

    private fun stoplocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                btnStart.isEnabled = false
                btnStop.isEnabled = true
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkPermissionForLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // Show the permission request
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

}
