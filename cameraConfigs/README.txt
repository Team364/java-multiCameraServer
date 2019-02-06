=======================
How to implement a static config for the camera
=======================

1) Set your parameters using the frcvision.local interface
2) Save parameters by using Right-Click->Save Link As... on "Source Config JSON"
3) Copy and paste parameters into the provided file called "configReadByJAR.json" 
***DO NOT REPLACE THE EXTRA STUFF IN THERE***
4) Set rPI to Writable using frcvision.local interface
5) Copy the new JSON to the pi:
    - cd to the cameraConfigs directory
    - Run "scp configReadByJAR.JSON pi@frcvision.local:/home/pi"
      (or use WinSCP if you're a Windows user)
    - When prompted, enter "raspberry" as password
5) Voila! The file should be automatically read by java-multiCameraServer-all.jar
