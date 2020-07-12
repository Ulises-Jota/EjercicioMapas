package com.example.ejemplomapas

import com.google.android.gms.maps.model.LatLng

// Las clases que siguen forman la estructura para mapear el JSon

class Response {
    var routes: ArrayList<Routes>? = null
}

class Routes {
    var legs: ArrayList<Legs>? = null
}

class Legs {
    var steps: ArrayList<Steps>? = null
}

class Steps {
    var end_location: LatLon? = null
    var start_location: LatLon? = null
}

class LatLon {
    var lat: Double = 0.0
    var lng: Double = 0.0

    fun toLatLng(): LatLng{
        return LatLng(lat, lng)
    }
}

