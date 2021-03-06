package cn.qiuxiang.react.amap3d.maps

import android.content.Context
import android.graphics.Point
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.*
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter

class AMapView(context: Context) : TextureMapView(context) {
    private val eventEmitter: RCTEventEmitter = (context as ThemedReactContext).getJSModule(RCTEventEmitter::class.java)
    private val markers = HashMap<String, AMapMarker>()
    private val polylines = HashMap<String, AMapPolyline>()
    private var locationType = MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER
    private val locationStyle by lazy {
        val locationStyle = MyLocationStyle()
        locationStyle.myLocationType(locationType)
        locationStyle
    }

    init {
        super.onCreate(null)

        map.setOnMapClickListener { latLng ->
            for (marker in markers.values) {
                marker.active = false
            }

            val event = Arguments.createMap()
            event.putDouble("latitude", latLng.latitude)
            event.putDouble("longitude", latLng.longitude)
            emit(id, "onPress", event)
        }

        map.setOnMapLongClickListener { latLng ->
            val event = Arguments.createMap()
            event.putDouble("latitude", latLng.latitude)
            event.putDouble("longitude", latLng.longitude)
            emit(id, "onLongPress", event)
        }

        map.setOnMyLocationChangeListener { location ->
            val event = Arguments.createMap()
            event.putDouble("latitude", location.latitude)
            event.putDouble("longitude", location.longitude)
            event.putDouble("accuracy", location.accuracy.toDouble())
            emit(id, "onLocation", event)
        }

        map.setOnMarkerClickListener { marker ->
           // add by david at 2019-7-9 start
            val event = Arguments.createMap()
            event.putString("index", marker.title)
            emit(markers[marker.id]?.id, "onPress",event)
            // add by david at 2019-7-9 end
             // emit(markers[marker.id]?.id, "onPress")
            false
        }

        map.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                emit(markers[marker.id]?.id, "onDragStart")
            }

            override fun onMarkerDrag(marker: Marker) {
                emit(markers[marker.id]?.id, "onDrag")
            }

            override fun onMarkerDragEnd(marker: Marker) {
                val position = marker.position
                val data = Arguments.createMap()
                data.putDouble("latitude", position.latitude)
                data.putDouble("longitude", position.longitude)
                emit(markers[marker.id]?.id, "onDragEnd", data)
            }
        })

        map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChangeFinish(position: CameraPosition?) {
                emitCameraChangeEvent("onStatusChangeComplete", position)
            }

            override fun onCameraChange(position: CameraPosition?) {
                emitCameraChangeEvent("onStatusChange", position)
            }
        })

        map.setOnInfoWindowClickListener { marker ->
            emit(markers[marker.id]?.id, "onInfoWindowPress")
        }

        map.setOnPolylineClickListener { polyline ->
            emit(polylines[polyline.id]?.id, "onPress")
        }

        map.setOnMultiPointClickListener { item ->
            val slice = item.customerId.split("_")
            val data = Arguments.createMap()
            data.putInt("index", slice[1].toInt())
            emit(slice[0].toInt(), "onItemPress", data)
            false
        }

        map.setInfoWindowAdapter(AMapInfoWindowAdapter(context, markers))
    }

    fun emitCameraChangeEvent(event: String, position: CameraPosition?) {
        position?.let {
            val data = Arguments.createMap()
            data.putDouble("zoomLevel", it.zoom.toDouble())
            data.putDouble("tilt", it.tilt.toDouble())
            data.putDouble("rotation", it.bearing.toDouble())
            data.putDouble("latitude", it.target.latitude)
            data.putDouble("longitude", it.target.longitude)
            if (event == "onStatusChangeComplete") {
                val southwest = map.projection.visibleRegion.latLngBounds.southwest
                val northeast = map.projection.visibleRegion.latLngBounds.northeast
                data.putDouble("latitudeDelta", Math.abs(southwest.latitude - northeast.latitude))
                data.putDouble("longitudeDelta", Math.abs(southwest.longitude - northeast.longitude))
            }
            emit(id, event, data)
        }
    }

    fun emit(id: Int?, name: String, data: WritableMap = Arguments.createMap()) {
        id?.let { eventEmitter.receiveEvent(it, name, data) }
    }

    fun add(child: View) {
        if (child is AMapOverlay) {
            child.add(map)
            if (child is AMapMarker) {
                markers.put(child.marker?.id!!, child)
            }
            if (child is AMapPolyline) {
                polylines.put(child.polyline?.id!!, child)
            }
        }
    }

    fun remove(child: View) {
        if (child is AMapOverlay) {
            child.remove()
            if (child is AMapMarker) {
                markers.remove(child.marker?.id)
            }
            if (child is AMapPolyline) {
                polylines.remove(child.polyline?.id)
            }
        }
    }

    private val animateCallback = object : AMap.CancelableCallback {
        override fun onCancel() {
            emit(id, "onAnimateCancel")
        }

        override fun onFinish() {
            emit(id, "onAnimateFinish")
        }
    }

    fun setLockedPin(args: ReadableArray?) {
        val image = args?.getString(1)
        val latLng =  map.cameraPosition.target
        val screenPosition = map.projection.toScreenLocation(latLng)
        val drawable = context.resources.getIdentifier(image, "drawable", context.packageName)
        val markOptions = MarkerOptions().anchor(0.5f, 1f)
                .icon(BitmapDescriptorFactory.fromResource(drawable))
//        val markOptions = MarkerOptions().anchor(0.5f, 1f).icon(BitmapDescriptorFactory.fromResource(drawable))

        val marker =  map.addMarker(markOptions)
        marker.setPositionByPixels(screenPosition.x, screenPosition.y)
    }

    fun setFitView(args: ReadableArray?) {
        val list = args?.getArray(0)
//        val list = args?.getArray(0).toArrayList()
        val builder = LatLngBounds.builder()

        ArrayList((0 until list!!.size())
                .map { list.getMap(it) }
                .map {
                    builder.include(LatLng(it?.getDouble("latitude")!!, it.getDouble("longitude")))
                }
            )

        val bound = builder.build();
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bound, 50)
        map.moveCamera(cameraUpdate)
    }

    fun animateTo(args: ReadableArray?) {
        val currentCameraPosition = map.cameraPosition
        val target = args?.getMap(0)!!
        val duration = args.getInt(1)

        var coordinate = currentCameraPosition.target
        var zoomLevel = currentCameraPosition.zoom
        var tilt = currentCameraPosition.tilt
        var rotation = currentCameraPosition.bearing

        if (target.hasKey("coordinate")) {
            val json = target.getMap("coordinate")
            coordinate = LatLng(json?.getDouble("latitude")!!, json.getDouble("longitude"))
        }

        if (target.hasKey("zoomLevel")) {
            zoomLevel = target.getDouble("zoomLevel").toFloat()
        }

        if (target.hasKey("tilt")) {
            tilt = target.getDouble("tilt").toFloat()
        }

        if (target.hasKey("rotation")) {
            rotation = target.getDouble("rotation").toFloat()
        }

        val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition(coordinate, zoomLevel, tilt, rotation))
        map.animateCamera(cameraUpdate, duration.toLong(), animateCallback)
    }

    fun setRegion(region: ReadableMap) {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsFromReadableMap(region), 0))
    }

    fun setLimitRegion(region: ReadableMap) {
        map.setMapStatusLimits(latLngBoundsFromReadableMap(region))
    }

    fun setLocationEnabled(enabled: Boolean) {
        map.myLocationStyle = locationStyle
        map.isMyLocationEnabled = enabled
    }

    fun setLocationInterval(interval: Long) {
        locationStyle.interval(interval)
        map.myLocationStyle = locationStyle
    }

    private fun latLngBoundsFromReadableMap(region: ReadableMap): LatLngBounds {
        val latitude = region.getDouble("latitude")
        val longitude = region.getDouble("longitude")
        val latitudeDelta = region.getDouble("latitudeDelta")
        val longitudeDelta = region.getDouble("longitudeDelta")
        return LatLngBounds(
                LatLng(latitude - latitudeDelta / 2, longitude - longitudeDelta / 2),
                LatLng(latitude + latitudeDelta / 2, longitude + longitudeDelta / 2)
        )
    }
}
