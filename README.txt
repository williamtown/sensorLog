sensorLog version 0.0.0.1
1. functions-record the android internal sensors data:
  -GNSS: "GNSSLogger original text(*.txt)" & rinex ver2.11 (*.obs)
         now only the pseudorange and C/N0 observation were logged in the rinex file.
  -sensors: gyros, accelerometers,and the orientaions(*.sns). the sensors records were synchronized with both the smartphone internal clock and the GPS time.
  -example datalog please refer to the folder "gnss_log_wen_example/"

2. this android app is developed based on the Google "GNSSLogger ver2.0.0.1" and another release version "GNSSLoggerR". 
This code is licensed under the Apache Software License 2.0.
many modules of the GNSSLogger were removed to simplified the app, i.e. the map and plot UI. only keep the core log function.
although GNSSLogger also release the function to record the rinex format GNSS observations and the Sensors data (after ver3.0.0.0), the original code was not open to the public since that version.
therefore, this app can be taken as a substitute one if somebody wanna to see the code work to log the GNSS internal sensors.

GNSSLogger: https://github.com/google/gps-measurement-tools
GNSSLoggerR: https://github.com/superhang/GNSSLogger


3. this app is using a string buffer, normally a 1 minute time during test can be accepted to store the observation file. otherwise, the log text file maybe empty since the buffer was not full filled.

4. there are two fragments, the first one is the "settings" and the other one "log". trigger the switches at the "setting" fragment and slide to the "log" fragment, click the button"start log" to start the logging.

if you have some suggestions or give support this contineous development, plz send information the author:
yanwenlin@jsnu.edu.cn


*********************************************************************************************************************
sensorLog version 0.0.0.1
1. 功能-记录安卓智能手机的内置传感器数据
   -GNSS: "GNSSLogger 文本(*.txt)" 和 rinex ver2.11 (*.obs)文本。目前这个版本的rinex文件只记录伪距和载噪比
   -传感器：加速度计、陀螺仪和方向(*.sns)。传感器数据同时和手机内部时钟和GPS时间对准，保证了数据时间系统的统一性。
   -样本记录文件在"gnss_log_wen_example/"
2. 这个APP基于Google "GNSSLogger ver2.0.0.1" ，同时借鉴了一个修改后的版本 "GNSSLoggerR"的部分代码. 遵守Apache Software License 2.0。
   GNSSLogger的很多模块在这个版本中被移除，比如地图和数据绘制等功能，只保留了核心的数据记录功能。
   GNSSLogger在3.0.0.0版本以后同样也支持rinex文件和传感器数据的记录，但3.0以后的版本的代码还未开源。因此，此代码可以当作 GNSSLogger在3.0的替代。
GNSSLogger: https://github.com/google/gps-measurement-tools
GNSSLoggerR: https://github.com/superhang/GNSSLogger

3. app使用了string buffer，通常需要1分钟左右的观测才能成功记录数据，否则，记录文件可能为空。
4. 使用方法：在"settings"界面中打开开关，然后滑向"log"界面点击"start log" 开始记录数据。

如果对此软件的修改有建议，或有意支持该app的继续开发，请与作者联系：yanwenlin@jsnu.edu.cn
*********************************************************************************************************************


relative works:
[1] Wenlin Yan, Algorithm and Application of the Iterative Running Mean Filter on the Pseudo-range Differential Positioning based on the Smartphone GNSS Observations(2022, in press)
[2] Yan, W., Zhang, Q., Zhang, Y., Wang, A., Zhao, C. The Validation and Performance Assessment of the Android Smartphone Based GNSS/INS Coupled Navigation System[C]. China Satellite Navigation Conference (CSNC 2021). Singapore: Springer Singapore, 2021: 310-319
[3] Yan, W., Zhang, Q., Wang, L., Mao, Y., Wang, A., Zhao, C. A Modified Kalman Filter for Integrating the Different Rate Data of Gyros and Accelerometers Retrieved from Android Smartphones in 
