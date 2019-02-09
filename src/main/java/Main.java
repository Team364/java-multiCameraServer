
/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;
import java.util.List;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.ArrayList;
import java.util.List;
import edu.wpi.first.vision.VisionPipeline;

import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.cscore.VideoMode.PixelFormat;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;


import team364_rpi.*;
import team364_rpi.Camera.CameraConfig;

public final class Main {

  // internals
  //private static Object imgLock = new Object();
  //private static ArrayList<RotatedRect> latestRects = new ArrayList<RotatedRect>();
  //private static boolean noTargets = true;

  // private static ArrayList<Number> angle = new ArrayList<Number>();
  // private static ArrayList<Number> width = new ArrayList<Number>();
  // private static ArrayList<Number> height = new ArrayList<Number>();
  // private static ArrayList<Number> centerX = new ArrayList<Number>();
  // private static ArrayList<Number> centerY = new ArrayList<Number>();

  private Main() { }

  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
}
  /**
   * Main.
   */
  public static void main(String... args) {
    //Camera myCam = new Camera();
      try {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        //Object imglock = new Object();

        CameraConfig newConfig = new CameraConfig();
        newConfig.name = "cam0";
        newConfig.path = "/dev/video0";

        UsbCamera camera = new UsbCamera(newConfig.name, newConfig.path);
        //camera.setResolution(1024, 576);
        //camera.setPixelFormat(PixelFormat.kMJPEG);
        camera.setFPS(10);

        camera.setVideoMode(PixelFormat.kYUYV, 1024, 576, 10);//640x480, 1024x576, 1920x1080

        final DynamicVisionPipeline pipeline = new DynamicVisionPipeline();

        //VideoCapture capture = new VideoCapture(0);
        //VideoWriter writer = new VideoWriter();
        
        // Mat mat = new Mat();
        
        //CameraServer inst = CameraServer.getInstance();
        //readConfigFile();
        //VideoSource cam = prepareCamera(cameraConfigs.get(0));
        //inst.startAutomaticCapture(cam);

        CameraServer.getInstance().addCamera(camera);
        //CameraServer.getInstance().startAutomaticCapture(camera);
        
        Mat output = new Mat();
        
        // new Thread(() -> {
           CvSink cvSink = CameraServer.getInstance().getVideo();


        //   while(!Thread.interrupted()) {
        //     Mat source = new Mat();

        //       //capture.read(source);
        //       //writer.write(source);
        //       //cvSource.
        //       //System.out.println("Matrix size: "+source.size());
              
        //       //long start = System.currentTimeMillis();
        //       //cvSink.grabFrame(source);
        //       //long start = System.currentTimeMillis();

        //       if (cvSink.grabFrame(source) == 0) continue;
        //       //System.out.println("grab time: "+ (System.currentTimeMillis() - start));
              
        //       // //Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2GRAY);
        //       //synchronized(imglock){
        //       pipeline.process(source);
        //       //}
  
        //       //start = System.currentTimeMillis();
        //       //cvSource.putFrame(source);
        //       //cvSource.putFrame(output);
        //       //System.out.println("put time: "+ (System.currentTimeMillis() - start));
        //   }
        // }).start();

        // new Thread(() -> {
        CvSource cvSource = CameraServer.getInstance().putVideo("Blur", 640, 480);
        
        //   Mat output = new Mat();

        //   while(!Thread.interrupted()) {
        //     //synchronized (imglock){
        //       output = pipeline.getOutput();
        //       //if (pipeline.getOutput(output) == 0) continue;
        //       //System.out.println(cvSource.)
        //     //}
        //     //cvSource.putFrame(output);
        //   }
        // }).start();

        while (true){
          long start = System.currentTimeMillis();
          if (cvSink.grabFrame(output)==0) continue;
          System.out.println("grab frame: " +(System.currentTimeMillis()-start));
          cvSource.putFrame(output);
          //Thread.sleep(5);
        }

        //Thread.sleep(10);
      } catch (Exception e) {
        //TODO: handle exception
      }
    }
  }




  //   // // setup and start NetworkTables
  //   // NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
  //   // NetworkTable visionTable = ntinst.getTable("visionParameters");

  //   // // Input entries
  //   // NetworkTableEntry searchConfigNumber = visionTable.getEntry("searchConfigNumber"); // 0: tape, 1: ball, 2: disk
    
  //   // // Output entries
  //   // NetworkTableEntry visibleTargets_angle = visionTable.getEntry("visibleTargets.angle");
  //   // NetworkTableEntry visibleTargets_width = visionTable.getEntry("visibleTargets.width");
  //   // NetworkTableEntry visibleTargets_height = visionTable.getEntry("visibleTargets.height");
  //   // NetworkTableEntry visibleTargets_centerX = visionTable.getEntry("visibleTargets.centerX");
  //   // NetworkTableEntry visibleTargets_centerY = visionTable.getEntry("visibleTargets.centerY");
  //   // NetworkTableEntry visibleTargets_noTarget = visionTable.getEntry("visibleTargets.noTarget");

  //   // System.out.println("Setting up NetworkTables client for team " + 364);
  //   // ntinst.startClientTeam(364);

  //   // TODO: Delete the code below - we don't need to pass a config file for the camera via cmd line.
  //   // Pass config file to the camera handler
  //   // if (args.length > 0) {
  //   //   System.out.println("args[0]: " + args[0]);
  //   //   CameraStuff.setConfigFile(args[0]);
  //   // } else { System.out.println("No args provided. ");}

  //   // read config file
  //   if (!CameraStuff.readConfigFile()) {
  //     return;
  //   }

  //   // start cameras
  //   List<VideoSource> cameras = new ArrayList<>();
  //   for (CameraStuff.CameraConfig cameraConfig : CameraStuff.cameraConfigs) {
  //     cameras.add(CameraStuff.startCamera(cameraConfig));
  //   }

  //   // start image processing on camera 0 if present
  //   if (cameras.size() >= 1) {
  //     VisionThread visionThread = new VisionThread(cameras.get(0), new DynamicVisionPipeline(), processingPipeline -> {
  //       synchronized (imgLock) {
  //           // Setup pipeline to process TAPE, BALL, or DISK depending on NetworkTable input
  //           processingPipeline.setSearchConfigNumber(0);//(int)searchConfigNumber.getDouble(0));

  //           // Read out the latest output from the pipeline
  //           latestRects = processingPipeline.rotatedRectsOutput();
  //         }
  //       });
  //     visionThread.start();
  //   }

  //   // loop forever and ever ane ever
  //   for (;;) {
  //     try {
  //       angle.clear();
  //       width.clear();
  //       height.clear();
  //       centerX.clear();
  //       centerY.clear();

  //       // We have to LOCK to make sure 2nd thread doesn't change
  //       // outputRects while we're reading from it
  //       synchronized (imgLock) {
  //         if(latestRects.size() > 0){
  //           noTargets = false;
  //         }else{
  //           noTargets = true;
  //         }
  //         for (int i = 0; i < latestRects.size(); i++) {
  //           RotatedRect r = latestRects.get(i);
  //           angle.add(r.angle);
  //           width.add(r.size.width);
  //           height.add(r.size.height);
  //           centerX.add(r.center.x);
  //           centerY.add(r.center.y);
  //         }
  //       }

  //       // Write to NetworkTable
  //       // visibleTargets_angle.setDoubleArray(angle.stream().mapToDouble(i -> (double)i).toArray());
  //       // visibleTargets_width.setDoubleArray(width.stream().mapToDouble(i -> (double)i).toArray());
  //       // visibleTargets_height.setDoubleArray(height.stream().mapToDouble(i -> (double)i).toArray());
  //       // visibleTargets_centerX.setDoubleArray(centerX.stream().mapToDouble(i -> (double)i).toArray());
  //       // visibleTargets_centerY.setDoubleArray(centerY.stream().mapToDouble(i -> (double)i).toArray());
  //       // visibleTargets_noTarget.setBoolean(noTargets);

  //       // Rest (in milliseconds)
  //       Thread.sleep(17); // 0.016 seconds (~60/sec), about 2x as fast as FPS

  //     } catch (InterruptedException ex) {
  //       return;
  //     }
  //   }