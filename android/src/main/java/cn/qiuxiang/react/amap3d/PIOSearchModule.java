package cn.qiuxiang.react.amap3d;

import androidx.annotation.Nullable;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;

/**
 * Created by Jason on 2017/11/13.
 */

public class PIOSearchModule extends ReactContextBaseJavaModule implements PoiSearch.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener{
//    searchPoiByCenterCoordinate
    private Promise mPromise;
    private Promise mGeocodePromise;
    private final ReactApplicationContext mReactContext;

    public PIOSearchModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return "POISearch";
    }


    @ReactMethod
    public  void searchPoiByCenterCoordinate(@Nullable ReadableMap options, Promise promise) {
        mPromise = promise;

        if (options == null) {
            promise.resolve(null);
            return;
        }

        /** modified by david at 2019-03-19 start */
        // 签到 公司分页查询
        int page = 0;
        if (options.hasKey("page")) {
            page = options.getInt("page");
        }
        /** modified by david at 2019-03-19 end */


        String types = "";
        String keywords = "";
        if (options.hasKey("types")) {
            types = options.getString("types");
        }
        if (options.hasKey("keyword")) {
            keywords = options.getString("keyword");
        }

        PoiSearch.Query query = new PoiSearch.Query(keywords, types, "");
        query.setPageSize(options.getInt("offset"));// 设置每页最多返回多少条poiitem
        query.setPageNum(page);//设置查询页码
        PoiSearch poiSearch = new PoiSearch(mReactContext, query);
        poiSearch.setOnPoiSearchListener(this);
        if (options.hasKey("radius")) {
            ReadableMap latlng = options.getMap("coordinate");

            PoiSearch.SearchBound bound = new PoiSearch.SearchBound(new LatLonPoint(
               latlng.getDouble("latitude"), latlng.getDouble("longitude")), options.getInt("radius"));
            poiSearch.setBound(bound);
        }
        poiSearch.searchPOIAsyn();

    }

    /**
     *  根据经纬度获取地址
     * @param options
     * @param promise
     */
    @ReactMethod
    public void getAddressByLatlng(@Nullable ReadableMap options, Promise promise) {
        mGeocodePromise = promise;

        if (options == null) {
            promise.resolve(null);
            return;
        }

        if(options.hasKey("latitude") && options.hasKey("longitude")) {
            int radius = 1000;
            if (options.hasKey("radius")) {
                radius = options.getInt("radius");
            }
            LatLonPoint point = new LatLonPoint(options.getDouble("latitude"), options.getDouble("longitude"));

            RegeocodeQuery query = new RegeocodeQuery(point, radius, GeocodeSearch.AMAP);

            GeocodeSearch geocodeSearch = new GeocodeSearch(mReactContext);
            geocodeSearch.setOnGeocodeSearchListener(this);
            geocodeSearch.getFromLocationAsyn(query);
        } else {
            promise.resolve(null);
        }

    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        if (i == 1000) {
            ArrayList<PoiItem> list = poiResult.getPois();
            WritableArray result = Arguments.createArray();
            for(PoiItem poiItem : list) {
                result.pushMap(toMap(poiItem));
            }
            mPromise.resolve(result);
        } else {
            mPromise.reject(String.valueOf(i), "查询失败");
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {


    }

    // 根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        if (i == 1000) {
            // 搜索成功
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();

            WritableMap result = Arguments.createMap();
            result.putString("address", regeocodeAddress.getFormatAddress());
            result.putString("province", regeocodeAddress.getProvince());
            result.putString("city", regeocodeAddress.getCity());
            result.putString("district", regeocodeAddress.getDistrict());
            mGeocodePromise.resolve(result);
        } else {
            mGeocodePromise.reject(String.valueOf(i), "获取地址失败");
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    public WritableMap toMap(PoiItem poiItem) {
        WritableMap retMap = Arguments.createMap();

        retMap.putString("uid", poiItem.getPoiId());
        retMap.putString("name", poiItem.toString());
        retMap.putString("type", poiItem.getTypeDes());
        retMap.putString("typecode", poiItem.getTypeCode());
        LatLonPoint latlng = poiItem.getLatLonPoint();
        retMap.putDouble("latitude", latlng.getLatitude());
        retMap.putDouble("longitude", latlng.getLongitude());
        retMap.putString("address", poiItem.getSnippet());
        retMap.putInt("distance", poiItem.getDistance());
        retMap.putString("province", poiItem.getProvinceName());
        retMap.putString("city", poiItem.getCityName());
        retMap.putString("district", poiItem.getAdName());
        retMap.putString("tel", poiItem.getTel());
        return retMap;
    }
}
