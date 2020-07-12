package com.example.ejemplomapas

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class Mapa(mapa: GoogleMap, context: Context, var markerClickListener: GoogleMap.OnMarkerClickListener, var markerDragListener: GoogleMap.OnMarkerDragListener) {

    var mMap: GoogleMap? = null
    private var context: Context? = null
    var miPosicion: LatLng? = null

    // Marcadores de mapa
    private var marcadorGolden: Marker? = null
    private var marcadorPiramides: Marker? = null
    private var marcadorPisa: Marker? = null

    // Marcadores que crearía el usuario
    private var listaMarcadores: ArrayList<Marker>? = null


    init {
        this.mMap = mapa
        this.context = context
    }

    fun dibujarFormas() {
        // Para dibujar líneas
        var coordenadasLineas = PolylineOptions()
            .add(LatLng(-37.2933335004842, -59.15446858853101))
            .add(LatLng(-37.29082969335706, -59.15007680654525))
            .add(LatLng(-37.28929327956208, -59.149976558983326))
            .add(LatLng(-37.30016726683867, -59.14594251662492))
            .pattern(arrayListOf<PatternItem>(Dot(), Gap(20F)))
            .color(Color.RED)
            .width(20F)

        mMap?.addPolyline(coordenadasLineas)

        // Para dibujar un polígono
        var coordenadasPoligono = PolygonOptions()
            .add(LatLng(-37.29082969335706, -59.15007680654525))
            .add(LatLng(-37.28929327956208, -59.149976558983326))
            .add(LatLng(-37.30016726683867, -59.14594251662492))
            .strokePattern(arrayListOf<PatternItem>(Dash(15F), Gap(20F)))
            .strokeColor(Color.GREEN)
            .fillColor(Color.YELLOW)
            .strokeWidth(10F)

        mMap?.addPolygon(coordenadasPoligono)

        // Para dibujar un círculo
        var coordenadasCirculo = CircleOptions()
            .center(LatLng(-37.2933335004842, -59.15446858853101))
            .radius(80.0)
            .strokeColor(Color.WHITE)
            .fillColor(Color.WHITE)
            .strokePattern(arrayListOf<PatternItem>(Dot(), Gap(25f)))

        mMap?.addCircle(coordenadasCirculo)
    }

    fun cambiarEstiloMapa() {
        // Escoger qué tipo de mapa se quiere mostrar
        // mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Escoger qué estilo de mapa se quiere mostrar, desde un recurso JSon. Se guarda en una
        // variable booleana para validar en caso de que no se pueda cargar.
        val exitoCambioMapa = mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.context, R.raw.estilo_mapa))
        if (!exitoCambioMapa!!){
            // Mencionar que hubo un problema al cambiar el estilo de mapa
            Toast.makeText(this.context, "Hubo un inconveniente al cargar el estilo de mapa retro", Toast.LENGTH_SHORT).show()
        }
    }

    fun crearListeners() {
        // Quedará implementado en onMarkerClick()
        mMap?.setOnMarkerClickListener(markerClickListener)
        // Quedará implementado en onMarkerDrag(), onMarkerDragStart() y onMarkerDragEnd()
        mMap?.setOnMarkerDragListener(markerDragListener)
    }

    fun marcadoresEstaticos() {
        val goldenGate = LatLng(37.8199286, -122.4782551)
        val piramidesGiza = LatLng(29.9772962, 31.1324955)
        val torrePisa = LatLng(43.722952, 10.396597)

        marcadorGolden = mMap?.addMarker(
            MarkerOptions()
                .position(goldenGate)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.diving_mask_icon))
                // .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .snippet("Punto de buceo") // Descripción breve que aparece debajo del title
                .alpha(1F) // Define opacidad, de 0 a 1.
                .title("Golden Gate"))
        marcadorGolden?.tag = 0

        marcadorPiramides = mMap?.addMarker(
            MarkerOptions()
                .position(piramidesGiza)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .alpha(0.6f)
                .title("Pirámides de Giza"))
        marcadorPiramides?.tag = 0

        marcadorPisa = mMap?.addMarker(
            MarkerOptions()
                .position(torrePisa)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .alpha(0.9f)
                .title("Torre de Pisa"))
        marcadorPisa?.tag = 0
    }

    fun prepararMarcadores() {
        listaMarcadores = ArrayList()

        // Agrega marcadores al array según el usuario vaya agregándolos al hacer un click largo
        // en diferentes posiciones del mapa.
        mMap!!.setOnMapLongClickListener {
                location: LatLng? ->
            listaMarcadores?.add(mMap!!.addMarker(
                MarkerOptions()
                    .position(location!!)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .alpha(0.8f)
                    .title("On Long Click Marker")))

            // Drag and drop habilitado
            listaMarcadores?.last()?.isDraggable = true

            // En caso de tener una cuenta para utilizar la API Directions de GoogleRoutes
            /*val coordUltimoMarcador = LatLng(listaMarcadores?.last()?.position!!.latitude, listaMarcadores?.last()?.position!!.longitude)

            val origen = "origin=${miPosicion?.latitude},${miPosicion?.longitude}&"

            val destino = "destination=${coordUltimoMarcador.latitude},${coordUltimoMarcador.longitude}&"

            val parametros = "${origen}${destino}sensor=false&mode=driving"
            // Llama al método para realizar la solicitud HTTP a googleapi y obtener un json con la respuesta.
            cargarURL("http://maps.googleapis.com/maps/api/directions/json?$parametros&key=${R.string.google_maps_key}")*/
        }
    }

    // Solicitud HTTP para obtener un JSON con los datos del movimiento en el mapa. Pide una url por argumento.
    /*private fun cargarURL(url: String) {
        val queue = Volley.newRequestQueue(this)
        val solicitud = StringRequest(Request.Method.GET, url, Response.Listener<String>{
            response ->
            Log.d("HTTP", response)
        }, Response.ErrorListener {})

        queue.add(solicitud)
    }*/

    // Toma el string con la respuesta y lo convierte en un objeto de tipo Response.
    fun obtenerCoordenadas(json: String): PolylineOptions{
        val gson = Gson()
        val objeto = gson.fromJson(json, com.example.ejemplomapas.Response::class.java)

        val puntos = objeto.routes?.get(0)!!.legs?.get(0)!!.steps!!

        var coordenadas = PolylineOptions()

        for (punto in puntos){
            coordenadas.add(punto.start_location?.toLatLng())
            coordenadas.add(punto.end_location?.toLatLng())
        }
        coordenadas
            .color(Color.BLUE)
            .width(20F)
        return coordenadas
    }

    fun configurarMiUbicación(){
        // Botón para detectar ubicación actual
        mMap?.isMyLocationEnabled = true
        mMap?.uiSettings?.isMyLocationButtonEnabled = true
    }

    fun anadirMarcadorMiPosicion(){
        mMap?.addMarker(MarkerOptions().position(miPosicion!!).title("Estoy aquí"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(miPosicion))
    }
}