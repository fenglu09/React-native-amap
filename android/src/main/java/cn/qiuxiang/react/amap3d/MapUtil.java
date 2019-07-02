package cn.qiuxiang.react.amap3d;

import android.content.Context;
import android.util.Log;

import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.model.LatLng;
import com.facebook.react.bridge.ReadableArray;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapUtil {

    public static String calculateCoordinate(ReadableArray obj, Context context) {
        try {
            int num = obj.size();
            JSONArray array = new JSONArray(obj.toString());
            CoordinateConverter converter = new CoordinateConverter(context);
            converter.from(CoordinateConverter.CoordType.GPS);

            for (int i = 0; i < num; i++) {
                JSONObject item = array.getJSONObject(i);
                LatLng p = new LatLng(item.getDouble("lat"), item.getDouble("long"));
                converter.coord(p);
                LatLng desLatLng = converter.convert();
                item.put("lat", desLatLng.latitude);
                item.put("long", desLatLng.longitude);
            }
            return array.toString();

        } catch (Exception e) {
            Log.d("aaa", e.toString());
        }
        return null;
    }
}
