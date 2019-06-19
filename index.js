import MapView from "./js/maps/MapView";
import Marker from "./js/maps/Marker";
import Polyline from "./js/maps/Polyline";
import Polygon from "./js/maps/Polygon";
import Circle from "./js/maps/Circle";
import MultiPoint from "./js/maps/MultiPoint";
import MapUtils from './js/Utils'
import RNLocation from "./js/location/index";
import POISearch from './js/poiSearch/POISearch';

MapView.Marker = Marker;
MapView.Polyline = Polyline;
MapView.Polygon = Polygon;
MapView.Circle = Circle;
MapView.MultiPoint = MultiPoint;

export default MapView;
export {
  MapView,
  Marker,
  Polyline,
  Polygon,
  Circle,
  MultiPoint,
  RNLocation,
  POISearch,
  MapUtils,
};