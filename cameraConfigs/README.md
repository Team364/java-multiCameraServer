## How to implement a static config for the camera

1. Set your parameters using the frcvision.local interface
1. Save parameters by using Right-Click->Save Link As... on "Source Config JSON"
1. Copy and paste parameters into the provided file called "configReadByJAR.json" 
**DO NOT REPLACE THE EXTRA STUFF IN THERE**
1. Set rPI to Writable using frcvision.local interface
1. Copy the new JSON to the pi:
    1. cd to the cameraConfigs directory
    1. Run 
    ```scp configReadByJAR.JSON pi@frcvision.local:/home/pi```
      (or use WinSCP if you're a Windows user)
    1. When prompted, enter "raspberry" as password
1. Voila! The file should be automatically read by java-multiCameraServer-all.jar

![Fusion Logo](http://www.fusion364.com/img/fusionlogo.png)

![Step 1](../images/step1.png =250x)
![Step 2](../images/step2.png =250x)
![Step 3](../images/step3.png =250x)
