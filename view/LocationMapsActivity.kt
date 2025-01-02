package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.databinding.ActivityLocationMapsBinding
import java.util.Locale

class LocationMapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityLocationMapsBinding

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    var eventLocationAddress = ""
    val eventLocationCoordinatList: ArrayList<Double> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLocationMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        register_launcher()

        binding.btnSaveLocation.isEnabled = false

        binding.btnSaveLocation.setOnClickListener {/*
            val intent = Intent(this@LocationMapsActivity, MainActivity::class.java)
            intent.putExtra("adress", eventLocationAddress)
            startActivity(intent)*/

            val resultIntent = Intent()
            //println("maps: ${eventLocationCoordinatList[0]} , ${eventLocationCoordinatList[1]}")
            if(eventLocationAddress != ""){
                resultIntent.putExtra("address", eventLocationAddress)
                resultIntent.putExtra("coordinatLatitude", eventLocationCoordinatList[0])
                resultIntent.putExtra("coordinatLongitude", eventLocationCoordinatList[1])
                setResult(Activity.RESULT_OK, resultIntent) // Sonuç olarak veri gönderiyoruz
            }
            finish()

        }

        binding.btnSearch.setOnClickListener {
            val address = binding.etxtLocationForSearch.text.toString()
            val location = searchLocation(address)
            if(location != null){
                eventLocationCoordinatList.clear()
                val country = location.countryName //ülke
                val thoroughfare = location.thoroughfare //sokak
                val subThoroughfare = location.subThoroughfare //daire no
                val district = location.subAdminArea
                val city = location.adminArea
                val latitude = location.latitude
                val longitude = location.longitude
                eventLocationCoordinatList.add(latitude)
                eventLocationCoordinatList.add(longitude)
                eventLocationAddress = "${thoroughfare}, No:${subThoroughfare}, ${district}/${city}:${country.uppercase()}"
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapLongClickListener(this)

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                mMap.clear()
                val userLocation = LatLng(location.latitude,location.longitude)
                mMap.addMarker(MarkerOptions().position(userLocation).title("Güncel Konumunuz"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
            }
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,"Konumunuza Erişebilmemiz İçin İzin Vermeniz Gerekiyor",
                    Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"){
                    //izin iste
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{
                //izin isteyeceğiz
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            //izin verilmiş
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,300,100f,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,16f))

                val geocoder = Geocoder(this,Locale.getDefault())
                val liste = geocoder.getFromLocation(lastLocation.latitude,lastLocation.longitude,1)
                val location = liste?.first()
                if(location != null){
                    val country = location.countryName //ülke
                    val thoroughfare = location.thoroughfare //sokak
                    val subThoroughfare = location.subThoroughfare //daire no
                    val district = location.subAdminArea
                    val city = location.adminArea
                    val latitude = location.latitude
                    val longitude = location.longitude
                    eventLocationCoordinatList.add(latitude)
                    eventLocationCoordinatList.add(longitude)
                    eventLocationAddress = "${thoroughfare}, No:${subThoroughfare}, ${district}/${city}:${country.uppercase()}"
                }

            }
            binding.btnSaveLocation.isEnabled = true
        }

    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        val selectedLocation = LatLng(p0.latitude,p0.longitude)
        mMap.addMarker(MarkerOptions().position(selectedLocation).title("Seçilen Konum"))

        //Geocoder Direkt adres almak için
        val geocoder = Geocoder(this,Locale.getDefault())
        var adres = ""

        try {
            val liste = geocoder.getFromLocation(p0.latitude,p0.longitude,1)
            val location = liste?.first()
            if(location != null){
                eventLocationCoordinatList.clear()
                eventLocationAddress = ""
                val country = location.countryName //ülke
                val thoroughfare = location.thoroughfare //sokak
                val subThoroughfare = location.subThoroughfare //daire no
                val district = location.subAdminArea
                val city = location.adminArea
                val latitude = location.latitude
                val longitude = location.longitude
                eventLocationCoordinatList.add(latitude)
                eventLocationCoordinatList.add(longitude)
                eventLocationAddress = "${thoroughfare}, No:${subThoroughfare}, ${district}/${city}:${country.uppercase()}"
            }
        }catch (e: Exception){
            println(e.localizedMessage)
        }
    }

    private fun searchLocation(address: String) : Address? {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(address, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val location = addressList[0]
                val latLng = LatLng(location.latitude, location.longitude)

                // Marker ekleyip haritayı konuma taşı
                mMap.clear() // Önceki marker'ları temizle
                mMap.addMarker(MarkerOptions().position(latLng).title(address))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                return location
            } else {
                Toast.makeText(this, "Konum bulunamadı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
            Toast.makeText(this, "Konum arama sırasında hata oluştu", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    private fun register_launcher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                if(ContextCompat.checkSelfPermission(this@LocationMapsActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,10f))
                    }
                }
            }else{
                Toast.makeText(this@LocationMapsActivity,"Konum İznini Reddettiniz!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}