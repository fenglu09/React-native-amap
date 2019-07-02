#import <React/RCTBridgeModule.h>
#import <MAMapKit/MAGeometry.h>
#import <AMapFoundationKit/AMapFoundationKit.h>
#pragma ide diagnostic ignored "OCUnusedClassInspection"

@interface AMapUtils : NSObject <RCTBridgeModule>
@end

@implementation AMapUtils

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(distance:(double)lat1
                      lng1:(double)lng1
                      lat2:(double)lat2
                      lng2:(double)lng2
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject ) {
    resolve(@(MAMetersBetweenMapPoints(
            MAMapPointForCoordinate(CLLocationCoordinate2DMake(lat1, lng1)),
            MAMapPointForCoordinate(CLLocationCoordinate2DMake(lat2, lng2))
    )));
}


/** add by david  2019-05-08 start */
//gps坐标转换成高德坐标
RCT_EXPORT_METHOD(getCoordinate:
                  (NSArray *)array
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject ) {
    
    @try {
        NSInteger count =array.count;
        NSMutableArray *mutableArray = [NSMutableArray arrayWithCapacity:count];
        
        for (NSInteger i=0; i<count; i++) {
            NSMutableDictionary *dic = array[i];
            NSString *lat = dic[@"lat"];
            NSString *log = dic[@"long"];
            CLLocationDegrees latitude = [lat doubleValue];
            CLLocationDegrees longitude = [log doubleValue];
            CLLocationCoordinate2D amapcoord = AMapCoordinateConvert(CLLocationCoordinate2DMake(latitude,longitude), AMapCoordinateTypeGPS);
            
            //            NSLog(@"amapcoord=%@", amapcoord);
            //            [dic setObject:latitude  forKey:@"lat"];
            NSNumber *latNum = [NSNumber numberWithDouble:amapcoord.latitude];
            NSNumber *longNum = [NSNumber numberWithDouble:amapcoord.longitude];
            [dic setObject:latNum forKey:@"lat"];
            [dic setObject:longNum forKey:@"long"];
            [mutableArray addObject:dic];
        }
        resolve(mutableArray);
    } @catch (NSException *exception) {
        
    }
}
/** add by david  2019-05-08 end */

@end
