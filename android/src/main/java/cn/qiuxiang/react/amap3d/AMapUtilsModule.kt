package cn.qiuxiang.react.amap3d

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.facebook.react.bridge.*

@Suppress("unused")
class AMapUtilsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return "AMapUtils"
    }

    @ReactMethod
    fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double, promise: Promise) {
        promise.resolve(AMapUtils.calculateLineDistance(LatLng(lat1, lng1), LatLng(lat2, lng2)))
    }

    /** add by david  at 2019-05-08 start */
    @ReactMethod
    fun getCoordinate(data: ReadableArray, promise: Promise) {
        promise.resolve(MapUtil.calculateCoordinate(data,currentActivity))
    }
    /** add by david  at 2019-05-08 end */

}