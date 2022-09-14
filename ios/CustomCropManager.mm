#import "CustomCropManager.h"
#import <React/RCTLog.h>

@implementation CustomCropManager

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(crop:(NSDictionary *)points imageUri:(NSString *)imageUri maxWidth:(NSString *)maxWidth callback:(RCTResponseSenderBlock)callback)
{
    NSString *parsedImageUri = [imageUri stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    NSURL *fileURL = [NSURL fileURLWithPath:parsedImageUri];
    CIImage *ciImage = [CIImage imageWithContentsOfURL:fileURL];
    ciImage = [ciImage imageByApplyingOrientation:kCGImagePropertyOrientationRightMirrored];
    
    float xScale = ciImage.extent.size.width / [points[@"width"] floatValue];
    float yScale = ciImage.extent.size.height / [points[@"height"] floatValue];
    
    CGPoint newLeft = CGPointMake([points[@"topLeft"][@"x"] floatValue] * xScale, [points[@"topLeft"][@"y"] floatValue] * yScale);
    CGPoint newRight = CGPointMake([points[@"topRight"][@"x"] floatValue] * xScale, [points[@"topRight"][@"y"] floatValue] * yScale);
    CGPoint newBottomLeft = CGPointMake([points[@"bottomLeft"][@"x"] floatValue] * xScale, [points[@"bottomLeft"][@"y"] floatValue] * yScale);
    CGPoint newBottomRight = CGPointMake([points[@"bottomRight"][@"x"] floatValue] * xScale, [points[@"bottomRight"][@"y"] floatValue] * yScale);
    
    NSMutableDictionary *rectangleCoordinates = [[NSMutableDictionary alloc] init];
    
    rectangleCoordinates[@"inputTopLeft"] = [CIVector vectorWithCGPoint:newLeft];
    rectangleCoordinates[@"inputTopRight"] = [CIVector vectorWithCGPoint:newRight];
    rectangleCoordinates[@"inputBottomLeft"] = [CIVector vectorWithCGPoint:newBottomLeft];
    rectangleCoordinates[@"inputBottomRight"] = [CIVector vectorWithCGPoint:newBottomRight];
    
    ciImage = [ciImage imageByApplyingFilter:@"CIPerspectiveCorrection" withInputParameters:rectangleCoordinates];
    
    CIContext *context = [CIContext contextWithOptions:nil];
    CGImageRef cgimage = [context createCGImage:ciImage fromRect:[ciImage extent]];
    UIImage *image = [UIImage imageWithCGImage:cgimage];
    
    NSData *imageToEncode = UIImagePNGRepresentation(image);
    
    NSString *dir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory,NSUserDomainMask, YES) firstObject];
    NSString *storageFolder = @"RNRectangleScanner";
    dir = [dir stringByAppendingPathComponent:storageFolder];
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error = nil;
    if(![fileManager createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:&error]){
        NSLog(@"Failed to create directory \"%@\". Error: %@", dir, error);
    }
    
    NSString *croppedFilePath = [dir stringByAppendingPathComponent:[NSString stringWithFormat:@"cropped_img_%i.png",(int)[NSDate date].timeIntervalSince1970]];
    [imageToEncode writeToFile:croppedFilePath atomically:YES];
    
    callback(@[[NSNull null], @{@"image": croppedFilePath}]);
}

- (CGPoint)cartesianForPoint:(CGPoint)point height:(float)height {
    return CGPointMake(point.x, height - point.y);
}

@end
