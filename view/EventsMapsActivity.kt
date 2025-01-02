package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
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
import com.omeraydin.etkinlikprojesi.databinding.ActivityEventsMapsBinding
import com.omeraydin.etkinlikprojesi.model.AcceptedEvent
import com.omeraydin.etkinlikprojesi.model.EventForMaps
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import org.json.JSONObject

class EventsMapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener, OnMapClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityEventsMapsBinding

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    var bottomSheetDialogRoute: BottomSheetDialog? = null

    var makerHolderList = mutableListOf<Marker>()

    private var routePolyline: Polyline? = null

    private lateinit var userLocation: LatLng
    var goingLocation: Marker? = null
    var zoom = 12f
    var distance = 30f

    var eventList: ArrayList<EventForMaps> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventsMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        register_launcher()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapClickListener(this)

        val events = intent.getSerializableExtra("generatedList") as? ArrayList<EventForMaps>
        if(events != null){
            eventList = events

            for (event in eventList){
                val position = LatLng(event.latitude, event.longitude)
                val marker = mMap.addMarker(MarkerOptions().position(position).title(event.head))

                // Marker'ı custom tag ile ilişkilendiriyoruz
                marker?.tag = event // Marker'a ait verileri tag ile ilişkilendiriyoruz
                mMap.setOnMarkerClickListener(this)
            }
        }

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                for (x in makerHolderList){
                    if(x.title == "Güncel Konumunuz"){
                        x.remove()
                    }
                }
                val currentLocation = LatLng(location.latitude,location.longitude)
                val marker = mMap.addMarker(MarkerOptions()
                    .position(currentLocation)
                    .title("Güncel Konumunuz")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                goingLocation?.let { getDirections(it){direction ->
                    bottomSheetDialogRoute?.findViewById<TextView>(R.id.tvr_txt_reminderDistinct)?.text = direction
                } }
                if(marker != null){
                    makerHolderList.add(marker)
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,zoom))
                userLocation = currentLocation
            }
        }

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,300,distance,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,zoom))
            }
        }
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        val markerData = p0.tag as? EventForMaps
        if (markerData != null) {
            // Tıklanan marker'ın verileri
            val goLocation = LatLng(markerData.latitude,markerData.longitude)

            val message = "Başlık: ${markerData.head}\nKısa Açıklama: ${markerData.shortExplanation}\nKonum: ${markerData.location}\n${markerData.latitude},${markerData.longitude}"
            val alertDialog = AlertDialog.Builder(
                this,
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Etkinlik Bilgisi")
                .setMessage(message)
                // Positive Button
                .setPositiveButton("Yol Tarifi") { dialog, _ ->
                    drawRoute(userLocation,goLocation)
                    goingLocation = p0
                    zoom = 17f
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,zoom))
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

                    getDirections(p0){direciton ->
                        bottomSheetView.findViewById<TextView>(R.id.tvr_txt_reminderDistinct).text = direciton
                        bottomSheetView.findViewById<TextView>(R.id.tvr_txt_goingLocation).text = markerData.location
                        bottomSheetView.findViewById<TextView>(R.id.tvr_btn_stopRoute).setOnClickListener {
                            goingLocation = null
                            routePolyline?.remove()
                            bottomSheetDialogRoute?.dismiss()
                            zoom = 12f
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,zoom))
                        }
                    }
                    bottomSheetDialogRoute?.show()
                }
                // Negative Button
                .setNegativeButton("Google Maps") { dialog, _ ->
                    zoom = 12f
                    openGoogleMapsDirections(p0)
                    //dialog.dismiss()
                }
                .setNeutralButton("Detaylar"){ dialog,_ ->
                    // BottomSheetDialog oluştur
                    val bottomSheetDialog = BottomSheetDialog(this)
                    val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_map_details, null)
                    bottomSheetDialog.setContentView(bottomSheetView)

                    // Menüyü işle
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailHead).text = "Başlık: ${markerData.head}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailCreaterName).text = "Oluşturan İsim: ${markerData.createrName}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailCreaterMail).text = "Oluşturan Mail: ${markerData.createrMail}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailShortExplanation).text = "Kısa Açıklama: ${markerData.shortExplanation}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailExplanation).text = "Açıklama: ${markerData.explanation}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailLocation).text = "Konum: ${markerData.location}"
                    bottomSheetView.findViewById<TextView>(R.id.tvDetailDatetime).text = "Tarih: ${markerData.date} / ${markerData.clock}"

                    // Menüyü göster
                    bottomSheetDialog.show()
                }
                .setOnDismissListener {
                    // Dialog kapatıldığında çalışır

                }
                .create().show()
        }
        //println("MARKERA TIKLANDI")
        return true
    }

    fun getDirections(marker: Marker, callback: (String) -> Unit) {
        val origin = userLocation  // Başlangıç noktası (örneğin, cihazın konumu)
        val destination = marker.position // Tıklanan marker'ın konumu

        // Google Maps Directions API kullanarak rota hesaplama işlemi
        val apiKey = getString(R.string.google_map_api)

        println(apiKey)
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

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val apiKey: String = getString(R.string.google_map_api)

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=driving"+
                "&key=${apiKey}"

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

    private fun openGoogleMapsDirections(marker: Marker) {
        // Kullanıcının konumunu al (Örnek olarak bir sabit konum kullanıyorum)
        val originLat = userLocation.latitude
        val originLng = userLocation.latitude

        // Marker'ın konumunu al
        val destinationLat = marker.position.latitude
        val destinationLng = marker.position.longitude

        // Google Maps için URL şeması
        val gmmIntentUri = Uri.parse("google.navigation:q=$destinationLat,$destinationLng&mode=d")

        // Google Maps uygulamasını başlat
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.google.android.apps.maps"))
        startActivity(mapIntent)
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
                if(ContextCompat.checkSelfPermission(this@EventsMapsActivity,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,300,distance,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastLocationLatLong = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLong,zoom))
                    }
                }
            }else{
                Toast.makeText(this@EventsMapsActivity,"Konum İznini Reddettiniz!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapClick(p0: LatLng) {
        //binding.txtDistinct.text = ""
    }
}