package com.example.ejemplomapas

import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener {

    private val permisoFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val permisoCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION

    private val CODIGO_SOLICITUD_PERMISO = 100

    // La API FusedLocationProviderClient combina diferentes tecnologías, como GPS y WiFi,
    // para proveer la ubicación que necesita la app.
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // LocationRequest define los parámetros para la calidad del servicio al momento de que
    // FusedLocationProviderClient requiere la ubicación.
    private var locationRequest: LocationRequest? = null

    // LocationCallback recibe las notificaciones de FusedLocationProviderClient cuando
    // la ubicación del dispositivo ha cambiado o cuando no puede ser determinada.
    private var callback: LocationCallback? = null

    private var mapa: Mapa? = null
    private val markerListener = this
    private val dragListener = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = FusedLocationProviderClient(this)
        inicializarLocationRequest()

        // Para obtener la ubicación actual. El objeto callback y, por lo tanto, el método onLocationResult(),
        // se llama cuando lo invoca fusedLocationClient con el método requestLocationUpdates(). El objeto callback
        // define qué hacer cada vez que llega una nueva actualización de la posición, definida por locationRequest.
        callback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                mapa!!.configurarMiUbicación()

                for (ubicacion in locationResult?.locations!!){
                    Toast.makeText(applicationContext, "${ubicacion.latitude} - ${ubicacion.longitude}", Toast.LENGTH_LONG).show()
                    // Agregar un marcador en la ubicación traída por locationResult?.locations!!
                    mapa!!.miPosicion = LatLng(ubicacion.latitude, ubicacion.longitude)
                    mapa?.anadirMarcadorMiPosicion()
                }
            }
        }
    }

    private fun inicializarLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest?.interval = 10000
        locationRequest?.fastestInterval = 5000
        locationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mapa = Mapa(googleMap, applicationContext, markerListener, dragListener)

        // Validar que estén los permisos dados.
        if (validarPermisosUbicacion()) {
            obtenerUbicacion()
        } else {
            pedirPermisos()
        }

        mapa?.cambiarEstiloMapa()

        mapa?.marcadoresEstaticos()

        mapa?.crearListeners()

        mapa?.prepararMarcadores()

        mapa?.dibujarFormas()

        var coordenadas = mapa?.obtenerCoordenadas(fakeResponse)

        mapa?.mMap?.addPolyline(coordenadas)
    }

    // Implementación de la interface GoogleMap.OnMarkerDragListener
    // Los siguientes métodos permiten actualizar la ubicación de los marcadores arrastrados.

    override fun onMarkerDragEnd(marcador: Marker?) {
        Toast.makeText(this, "El marcador dejó de moverse", Toast.LENGTH_SHORT).show()
        Log.d("MARCADOR FINAL", "${marcador?.position?.latitude}, ${marcador?.position?.longitude}")
    }

    override fun onMarkerDragStart(marcador: Marker?) {
        Toast.makeText(this, "Empezando a mover el marcador", Toast.LENGTH_SHORT).show()
        Log.d("MARCADOR INICIAL", "${marcador?.position?.latitude}, ${marcador?.position?.longitude}")
    }

    // Mientras se arrastra al marcador, se va modificando el título de la activity para mostrar las coordenadas.
    override fun onMarkerDrag(marcador: Marker?) {
        title = "${marcador?.position?.latitude} - ${marcador?.position?.longitude}"
    }

    // Implementación de la interface GoogleMap.OnMarkerClickListener
    // Define los eventos de click en el marcador. En este caso, cuenta la cantidad de clicks
    // que se la da a cada marcador.
    override fun onMarkerClick(marcador: Marker?): Boolean {
        var numeroClicks = marcador?.tag as? Int
        if (numeroClicks != null){
            numeroClicks++
            marcador?.tag = numeroClicks
            Toast.makeText(this, "Se han dado $numeroClicks clicks", Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun validarPermisosUbicacion(): Boolean {
        val hayUbicacionPrecisa = ActivityCompat.checkSelfPermission(this, permisoFineLocation) == PackageManager.PERMISSION_GRANTED
        val hayUbicacionOrdinaria = ActivityCompat.checkSelfPermission(this, permisoCoarseLocation) == PackageManager.PERMISSION_DENIED

        return hayUbicacionPrecisa && hayUbicacionOrdinaria
    }

    private fun obtenerUbicacion() {
        fusedLocationClient?.requestLocationUpdates(locationRequest, callback, null)
    }

    private fun pedirPermisos() {
        val deboProveerContexto = ActivityCompat.shouldShowRequestPermissionRationale(this, permisoFineLocation)

        if (deboProveerContexto){
            // Mandar mensaje con explicación adicional
            solicitudPermiso()
        } else {
            solicitudPermiso()
        }
    }

    private fun solicitudPermiso(){
        requestPermissions(arrayOf(permisoCoarseLocation, permisoFineLocation), CODIGO_SOLICITUD_PERMISO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CODIGO_SOLICITUD_PERMISO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtenerUbicacion()
                } else {
                    Toast.makeText(this, "No diste permiso para acceder a la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun detenerActualizacionUbicacion(){
        fusedLocationClient?.removeLocationUpdates(callback)
    }

    override fun onStart() {
        super.onStart()

        if (validarPermisosUbicacion()) {
            obtenerUbicacion()
        } else {
            pedirPermisos()
        }
    }

    override fun onPause() {
        super.onPause()
        detenerActualizacionUbicacion()
    }
}

// Simula la respuesta que debería haber arrojado la solicitud HTTP en caso de haber podido usar la API key.
val fakeResponse: String = "{\n" +
        "  \"geocoded_waypoints\" : [\n" +
        "    {\n" +
        "      \"geocoder_status\" : \"OK\",\n" +
        "      \"place_id\" : \"ChIJlWVUTSr50YURQ7EdijWsFdc\",\n" +
        "      \"types\" : [ \"street_address\" ]\n" +
        "    },\n" +
        "    {\n" +
        "      \"geocoder_status\" : \"OK\",\n" +
        "      \"place_id\" : \"ChIJ-ezMntf40YURKxwCIlaUlAI\",\n" +
        "      \"types\" : [ \"street_address\" ]\n" +
        "    }\n" +
        "  ],\n" +
        "  \"routes\" : [\n" +
        "    {\n" +
        "      \"bounds\" : {\n" +
        "        \"northeast\" : {\n" +
        "          \"lat\" : 19.4434305,\n" +
        "          \"lng\" : -99.1401891\n" +
        "        },\n" +
        "        \"southwest\" : {\n" +
        "          \"lat\" : 19.4362849,\n" +
        "          \"lng\" : -99.14795579999999\n" +
        "        }\n" +
        "      },\n" +
        "      \"copyrights\" : \"Map data c2018 Google, INEGI\",\n" +
        "      \"legs\" : [\n" +
        "        {\n" +
        "          \"distance\" : {\n" +
        "            \"text\" : \"1.9 km\",\n" +
        "            \"value\" : 1856\n" +
        "          },\n" +
        "          \"duration\" : {\n" +
        "            \"text\" : \"9 mins\",\n" +
        "            \"value\" : 555\n" +
        "          },\n" +
        "          \"end_address\" : \"Calle Guerrero 110, Buenavista, 06350 Buenavista,\",\n" +
        "          \"end_location\" : {\n" +
        "            \"lat\" : 19.4431737,\n" +
        "            \"lng\" : -99.14795579999999\n" +
        "          },\n" +
        "          \"start_address\" : \"Av. Hidalgo 268, Centro Histórico, Guerrero\",\n" +
        "          \"start_location\" : {\n" +
        "            \"lat\" : 19.4365156,\n" +
        "            \"lng\" : -99.1428954\n" +
        "          },\n" +
        "          \"steps\" : [\n" +
        "            {\n" +
        "              \"distance\" : {\n" +
        "                \"text\" : \"0.2 km\",\n" +
        "                \"value\" : 249\n" +
        "              },\n" +
        "              \"duration\" : {\n" +
        "                \"text\": \"2 mins\",\n" +
        "                \"value\": 117\n" +
        "              },\n" +
        "              \"end_location\" : {\n" +
        "                \"lat\" : -37.30016726683867,\n" +
        "                \"lng\" : -59.14594251662492\n" +
        "              },\n" +
        "              \"html_instructions\" : \"Head \\u003cb\\u003eeast\",\n" +
        "              \"polyline\" : {\n" +
        "                \"points\" : \"gesuBbzb|Q@AI]Gm@HcAVeCTiCFq@\"\n" +
        "              },\n" +
        "              \"start_location\" : {\n" +
        "                \"lat\" : -37.2933335004842,\n" +
        "                \"lng\" : -59.15446858853101\n" +
        "              },\n" +
        "              \"travel_mode\" : \"DRIVING\"\n" +
        "            },\n" +
        "            {\n" +
        "              \"distance\" : {\n" +
        "                \"text\" : \"0.3 km\",\n" +
        "                \"value\" : 266\n" +
        "              },\n" +
        "              \"duration\" : {\n" +
        "                \"text\": \"1 min\",\n" +
        "                \"value\": 68\n" +
        "              },\n" +
        "              \"end_location\" : {\n" +
        "                \"lat\" : 19.4362849,\n" +
        "                \"lng\" : -99.14056119999999\n" +
        "              },\n" +
        "              \"html_instructions\" : \"Head \\u003cb\\u003eeast\",\n" +
        "              \"polyline\" : {\n" +
        "                \"points\" : \"gesuBbzb|Q@AI]Gm@HcAVeCTiCFq@\"\n" +
        "              },\n" +
        "              \"start_location\" : {\n" +
        "                \"lat\" : 19.4365156,\n" +
        "                \"lng\" : -99.1428954\n" +
        "              },\n" +
        "              \"travel_mode\" : \"DRIVING\"\n" +
        "            },\n" +
        "            {\n" +
        "              \"distance\" : {\n" +
        "                \"text\" : \"0.3 km\",\n" +
        "                \"value\" : 266\n" +
        "              },\n" +
        "              \"duration\" : {\n" +
        "                \"text\": \"1 min\",\n" +
        "                \"value\": 68\n" +
        "              },\n" +
        "              \"end_location\" : {\n" +
        "                \"lat\" : 19.4362849,\n" +
        "                \"lng\" : -99.14056119999999\n" +
        "              },\n" +
        "              \"html_instructions\" : \"Head \\u003cb\\u003eeast\",\n" +
        "              \"polyline\" : {\n" +
        "                \"points\" : \"gesuBbzb|Q@AI]Gm@HcAVeCTiCFq@\"\n" +
        "              },\n" +
        "              \"start_location\" : {\n" +
        "                \"lat\" : 19.4365156,\n" +
        "                \"lng\" : -99.1428954\n" +
        "              },\n" +
        "              \"travel_mode\" : \"DRIVING\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"overview_polyline\" : {\n" +
        "            \"points\" : \"gesuBbzb|Q@AI]Gm@HcAVeCTiCFq@|\"\n" +
        "          },\n" +
        "          \"summary\" : \"Calle Mina\",\n" +
        "          \"warnings\" : [],\n" +
        "          \"waypoint_order\" : []\n" +
        "        }\n" +
        "      ],\n" +
        "      \"status\" : \"OK\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"