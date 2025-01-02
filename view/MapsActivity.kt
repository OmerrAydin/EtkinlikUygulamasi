package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.PolyUtil
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.databinding.ActivityMapsBinding
import com.omeraydin.etkinlikprojesi.model.EventForMaps
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    var markerHolderList = mutableListOf<Marker>()

    private lateinit var userLocation: LatLng
    var goingLocation: Marker? = null

    private var routePolyline: Polyline? = null

    var locationCheck = true

    var zoom = 17f
    var distance = 1f

    var bottomSheetDialogRoute: BottomSheetDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        register_launcher()
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val event = intent.getSerializableExtra("event") as? EventForMaps
        if(event != null){
            // BottomSheetDialog oluştur
            bottomSheetDialogRoute = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottum_sheet_map_route, null)
            bottomSheetDialogRoute?.setContentView(bottomSheetView)
            bottomSheetDialogRoute?.setCancelable(false)
            bottomSheetDialogRoute?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            bottomSheetDialogRoute?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            bottomSheetDialogRoute?.setCanceledOnTouchOutside(false)
            bottomSheetDialogRoute?.findViewById<TextView>(R.id.tvr_btn_stopRoute)?.setOnClickListener {
                goingLocation = null
                routePolyline?.remove()
                bottomSheetDialogRoute?.dismiss()
                zoom = 12f
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,zoom))
            }
            bottomSheetDialogRoute?.show()

            val location = LatLng(event.latitude,event.longitude)
            val locationMarker = mMap.addMarker(MarkerOptions().position(location).title("Başlık: ${event.head}"))
            if(locationMarker != null){
                goingLocation = locationMarker
            }
        }

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                for (x in markerHolderList){
                    if(x.title == "Güncel Konumunuz"){
                        x.remove()
                    }
                }
                val currentLocation = LatLng(location.latitude,location.longitude)
                val marker = mMap.addMarker(MarkerOptions().position(currentLocation)
                    .title("Güncel Konumunuz")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                if(marker != null){
                    markerHolderList.add(marker)
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,zoom))
                userLocation = currentLocation

                if(locationCheck && goingLocation != null && event != null){
                    goingLocation?.let {
                        locationCheck = false
                        val latLongGoingLocation = LatLng(event.latitude,event.longitude)
                        get_directions_for_event(event,latLongGoingLocation, it)
                    }
                }

                goingLocation?.let { getDirections(it){direction ->
                    bottomSheetDialogRoute?.findViewById<TextView>(R.id.tvr_txt_reminderDistinct)?.text = direction
                } }
            }
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,"Konumunuza Erişebilmemiz İçin İzin Vermeniz Gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"){
                    //izin iste
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{
                //izin isteyeceğiz
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            //izin verilmiş
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,300,1f,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,12f))
            }
        }
    }

    private fun get_directions_for_event(event: EventForMaps, location: LatLng, locationMarker: Marker){
        println(userLocation)
        println(location)
        drawRoute(userLocation,location)
        getDirections(locationMarker){direciton ->
            bottomSheetDialogRoute?.findViewById<TextView>(R.id.tvr_txt_reminderDistinct)?.text = direciton
            bottomSheetDialogRoute?.findViewById<TextView>(R.id.tvr_txt_goingLocation)?.text = event.location
        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_map_api)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=driving"+
                "&key=$apiKey"

        // URL üzerinden HTTP isteği yap
        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            println(response)
            // JSON yanıtını işle
            val jsonResponse = JSONObject(response)
            val routes = jsonResponse.getJSONArray("routes")
            println("2: ${routes}")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val overviewPolyline = route.getJSONObject("overview_polyline")
                val encodedPolyline = overviewPolyline.getString("points")

                // Polyline verisini çöz ve haritada göster
                val polyline = PolyUtil.decode(encodedPolyline)
                routePolyline = mMap.addPolyline(PolylineOptions().addAll(polyline).color(Color.BLUE).width(10f))
            }
        }, { error ->
            Toast.makeText(this, "Rota alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
        })

        requestQueue.add(stringRequest)
    }

    fun getDirections(marker: Marker, callback: (String) -> Unit) {
        val origin = userLocation  // Başlangıç noktası (örneğin, cihazın konumu)
        val destination = marker.position // Tıklanan marker'ın konumu

        val apiKey = getString(R.string.google_map_api)

        // Google Maps Directions API kullanarak rota hesaplama işlemi
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=${apiKey}"

        // Volley RequestQueue oluşturuluyor
        val queue = Volley.newRequestQueue(this)

        // JsonObjectRequest oluşturuluyor
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                // Başarıyla yanıt alındığında burası çalışır
                val direction = handleResponse(response)
                callback(direction)
            },
            Response.ErrorListener { error ->
                // Hata durumunda burası çalışır
                Toast.makeText(this, "Yol tarifi alınırken hata oluştu", Toast.LENGTH_LONG).show()
                callback("Hesaplama Hatası")
            }
        )

        // İsteği ekliyoruz
        queue.add(jsonObjectRequest)
    }

    private fun handleResponse(response: JSONObject): String {
        try {
            val routes = response.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                val leg = legs.getJSONObject(0)
                val distance = leg.getJSONObject("distance").getString("text")
                val duration = leg.getJSONObject("duration").getString("text")
                return "Mesafe: $distance, Süre: $duration"
            } else {
                Toast.makeText(this, "Yol tarifi bulunamadı", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        return "Ölçüm Hatası"
    }


    private fun register_launcher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                if(ContextCompat.checkSelfPermission(this@MapsActivity,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,10f))
                    }
                }
            }else{
                Toast.makeText(this@MapsActivity,"Konum İznini Reddettiniz!",Toast.LENGTH_SHORT).show()
            }
        }
    }
}