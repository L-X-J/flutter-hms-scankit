//
//  ScanViewController.m
//  flutter_hms_scankit
//
//  Created by mac on 2022/6/29.
//

#import "ScanViewController.h"
#import "SGQRCode.h"
#import <AVFoundation/AVFoundation.h>

@interface ScanViewController ()
{
    SGScanCode *scanCode;
}
@property (nonatomic, strong) SGScanView *scanView;
@property (nonatomic, strong) UIButton *flashlightBtn;
@property (nonatomic, strong) UILabel *promptLabel;
@property (nonatomic, assign) BOOL isSelectedFlashlightBtn;
@property (nonatomic, strong) UIView *bottomView;
@property (nonatomic, strong) AVCaptureDevice *device;
@end

@implementation ScanViewController

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    /// 二维码开启方法
    [scanCode startRunningWithBefore:nil completion:nil];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.scanView startScanning];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [self.scanView stopScanning];
    [scanCode stopRunning];
}

- (void)dealloc {
    NSLog(@"WCQRCodeVC - dealloc");
    [self removeScanningView];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor blackColor];
    scanCode = [SGScanCode scanCode];
    [self setupQRCodeScan];
    self.device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    [self.view addSubview:self.flashlightBtn];
    [self.view addSubview:self.scanView];
    [self setupNavigationBar];
}

- (void)setupQRCodeScan {

    [scanCode scanWithController:self resultBlock:^(SGScanCode *scanCode, NSString *result) {
        if (result) {
            [scanCode stopRunning];
            [self removeScanningView];
            self.successResult(result);
            [self dismissViewControllerAnimated:YES completion:nil];
        }
    }];
    
}

- (SGScanView *)scanView {
    if (!_scanView) {
        _scanView = [[SGScanView alloc] initWithFrame:CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height)];
        _scanView.cornerColor = [UIColor colorWithRed:253/255.0 green:111/255.0 blue:48/255.0 alpha:1];
        _scanView.scanLineName = @"";
    }
    return _scanView;
}
- (void)removeScanningView {
    [self.scanView stopScanning];
    [self.scanView removeFromSuperview];
    self.scanView = nil;
}
- (void)flashlightBtn_action:(UIButton *)button {
    if (button.selected == NO) {
        [button setImage:[self resourceBundleOfImageName:@"lightoff"] forState:UIControlStateNormal];
        if ([self.device hasTorch]) {
        [self.device lockForConfiguration:nil];
         // 开启手电筒
        [self.device setTorchMode:AVCaptureTorchModeOn];
         // 解除独占访问硬件设备
        [self.device unlockForConfiguration];
        button.selected = YES;
        }
    } else {
        [button setImage:[self resourceBundleOfImageName:@"lighton"] forState:UIControlStateNormal];
        button.selected = NO;
        [self.device lockForConfiguration:nil];
        [self.device setTorchMode:AVCaptureTorchModeOff];
        [self.device unlockForConfiguration];
    }
}

- (void)setupNavigationBar {
    
    UIButton * albumButton = [[UIButton alloc]initWithFrame:CGRectMake(self.view.bounds.size.width-90-50,40+self.view.bounds.size.height*2/3,50,50)];
    [self.view addSubview:albumButton];
    [albumButton setImage:[self resourceBundleOfImageName:@"album"] forState:UIControlStateNormal];
    [albumButton addTarget:self action:@selector(rightBarButtonItenAction) forControlEvents:UIControlEventTouchUpInside];
    
    UIButton * backButton = [[UIButton alloc]initWithFrame:CGRectMake(20,40,30,30)];
    [self.view addSubview:backButton];
    [backButton setImage:[self resourceBundleOfImageName:@"back"] forState:UIControlStateNormal];
    [backButton addTarget:self action:@selector(backButton) forControlEvents:UIControlEventTouchUpInside];
    
    UIButton *flashlightBtn = [UIButton buttonWithType:(UIButtonTypeCustom)];
    [self.view addSubview:flashlightBtn];
    CGFloat flashlightBtnW = 50;
    CGFloat flashlightBtnH = 50;
    // CGFloat flashlightBtnX = 0.5 * (self.view.frame.size.width - flashlightBtnW);
    CGFloat flashlightBtnX = 90;
    CGFloat flashlightBtnY = 40+self.view.bounds.size.height*2/3;
     flashlightBtn.frame = CGRectMake(flashlightBtnX, flashlightBtnY, flashlightBtnW, flashlightBtnH);
    [flashlightBtn setImage:[self resourceBundleOfImageName:@"lighton"] forState:UIControlStateNormal];
    flashlightBtn.selected = NO;
    [  flashlightBtn addTarget:self action:@selector(flashlightBtn_action:) forControlEvents:UIControlEventTouchUpInside];
}
-(void)backButton
{
    [self dismissViewControllerAnimated:NO completion:nil];
}
- (void)rightBarButtonItenAction {
    __weak typeof(self) weakSelf = self;
    [scanCode readWithResultBlock:^(SGScanCode *scanCode, NSString *result) {
       
        if (result == nil) {
          } else {
              self.successResult(result);
              [self dismissViewControllerAnimated:YES completion:nil];
     }
        
    }];
    
    if (scanCode.albumAuthorization == YES) {
        [self.scanView stopScanning];
    }
    [scanCode albumDidCancelBlock:^(SGScanCode *scanCode) {
        [weakSelf.scanView startScanning];
    }];
}

- (UIImage *)resourceBundleOfImageName:(NSString *)imageName {
    NSBundle *mainBundle = [NSBundle bundleForClass:[self class]];
    NSBundle *resourceBundle = [NSBundle bundleWithPath:[mainBundle pathForResource:@"flutter_hms_scankit" ofType:@"bundle"]];
    if (@available(iOS 13.0, *)) {
        UIImage *image = [UIImage imageNamed:imageName inBundle:resourceBundle withConfiguration:nil];
        return image;
     } else {
        UIImage *image = [UIImage imageNamed:imageName inBundle:resourceBundle compatibleWithTraitCollection:nil];
        return  image;
        // Fallback on earlier versions
    }
}

/**
 获取当前App使用语言
 */
- (NSString *)language {
    
    NSString *language = [[NSLocale preferredLanguages] objectAtIndex:0];
    NSArray  *array = [language componentsSeparatedByString:@"-"];
    NSString *currentLanguage = array[0];
    
    if (currentLanguage.length > 0 && [currentLanguage isEqualToString:@"zh"]) {
        //中文
        return @"zh_lang";
    }else if (currentLanguage.length > 0 && [currentLanguage isEqualToString:@"ja"]) {
        //日文
        return @"ja_lang";
    }else if (currentLanguage.length > 0 && [currentLanguage isEqualToString:@"ko"]) {
        //韩文
        return @"ko_lang";
    }else {
        //英文
        return @"en_lang";
    }
}
/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
