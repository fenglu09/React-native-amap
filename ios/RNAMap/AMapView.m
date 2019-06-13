#import <React/UIView+React.h>
#import "AMapView.h"
#import "AMapMarker.h"
#import "AMapPolyline.h"
#import "LocationStyle.h"

#pragma ide diagnostic ignored "OCUnusedMethodInspection"

@implementation AMapView {
    NSMutableDictionary *_markers;
    MAUserLocationRepresentation *_locationStyle;
    BOOL _isBoundsInit;
}

- (instancetype)init {
    _isBoundsInit = NO;
    _markers = [NSMutableDictionary new];
    self = [super init];
    return self;
}

- (void)setFrame:(CGRect)frame {
    if (!_isBoundsInit) {
        [super setFrame:frame];
    }
}

- (void)setBounds:(CGRect)bounds {
    _isBoundsInit = YES;
    [super setBounds:bounds];
}

- (void)setShowsTraffic:(BOOL)shows {
    self.showTraffic = shows;
}

- (void)setTiltEnabled:(BOOL)enabled {
    self.rotateCameraEnabled = enabled;
}

- (void)setLocationEnabled:(BOOL)enabled {
    self.showsUserLocation = enabled;
}

- (void)setShowCompass:(BOOL)enabled {
    self.showsCompass = enabled;
}

- (void)setCoordinate:(CLLocationCoordinate2D)coordinate {
    self.centerCoordinate = coordinate;
}

- (void)setTilt:(CGFloat)degree {
    self.cameraDegree = degree;
}

- (void)setRotation:(CGFloat)degree {
    self.rotationDegree = degree;
}

- (void)setLocationStyle:(LocationStyle *)locationStyle {
    if (!_locationStyle) {
        _locationStyle = [MAUserLocationRepresentation new];
    }
    _locationStyle.fillColor = locationStyle.fillColor;
    _locationStyle.strokeColor = locationStyle.strokeColor;
    _locationStyle.lineWidth = locationStyle.strokeWidth;
    _locationStyle.image = locationStyle.image;
    [self updateUserLocationRepresentation:_locationStyle];
}

// 如果在地图未加载的时候调用改方法，需要先将 region 存起来，等地图加载完成再设置
- (void)setRegion:(MACoordinateRegion)region {
    if (self.loaded) {
        super.region = region;
    } else {
        self.initialRegion = region;
    }
}

- (void)didAddSubview:(UIView *)subview {
    if ([subview isKindOfClass:[AMapMarker class]]) {
        AMapMarker *marker = (AMapMarker *) subview;
        marker.mapView = self;
        _markers[[@(marker.annotation.hash) stringValue]] = marker;
        dispatch_async(dispatch_get_main_queue(), ^{
            [self addAnnotation:marker.annotation];
        });
    }
    if ([subview isKindOfClass:[AMapOverlay class]]) {
        [self addOverlay:(id <MAOverlay>) subview];
    }
}

- (void)removeReactSubview:(id <RCTComponent>)subview {
    [super removeReactSubview:subview];
    if ([subview isKindOfClass:[AMapMarker class]]) {
        AMapMarker *marker = (AMapMarker *) subview;
        [self removeAnnotation:marker.annotation];
    }
    if ([subview isKindOfClass:[AMapOverlay class]]) {
        [self removeOverlay:(id <MAOverlay>) subview];
    }
}

- (AMapMarker *)getMarker:(id <MAAnnotation>)annotation {
    return _markers[[@(annotation.hash) stringValue]];
}

/* at by Stephen at 2019-06-05 start*/
// RN 升级到了0.57 版本，iOS版本 AMapCallout 里的点击事件失效，这里通过改变事件的响应链，将事件的响应链转到AMapCallout
-(UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *view = [super hitTest:point withEvent:event];
    NSArray *pinViewArr = self.subviews[1].subviews.copy;
    
    if (!pinViewArr.count) {
        
        return view;
    }
    // pinView(MAPinAnnotationView): 子视图包含UIImageView 和 MACustomCalloutView 的才是我们需要的
    // _pinView(MAPinAnnotationView): 据测试发现不管点那个标注，_pinView 都不会变
    __block NSInteger index = 0; // 获取选中的 pinView 的下标
    [pinViewArr enumerateObjectsUsingBlock:^(__kindof UIView * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        MAPinAnnotationView *pinView = (MAPinAnnotationView *)obj;
        // pivView 子视图有大于1的为选中的，没有选择中的只有一个UIImageView
        if (pinView.subviews.count > 1) {
            index = idx;
            *stop = YES;
        }
    }];
    
    MAPinAnnotationView *pinView = (MAPinAnnotationView *)pinViewArr[index];
    AMapCallout *infoWindow = (AMapCallout *)pinView.customCalloutView.subviews[0];
    
    // pinView.customCalloutView.subviews[0] 为需要的AMapCallout
    
    CGPoint tempPoint = [infoWindow convertPoint:point fromView:self.subviews[1]];
    
    // RN 组件里Marker下的View位置做了多少偏移， 这里需要补偿回来，否则会导致点击的点的位置超出判断的rect位置范围，这样事件的响应链传递不到AMapInfoWindow里
    CGPoint targetPoint = tempPoint;
    
    if (CGRectContainsPoint(infoWindow.bounds, targetPoint)) {
        // 响应链转到AMapCallout
        view = infoWindow;
    }
    return view;
}
/* at by Stephen at 2019-06-05 end*/

@end
