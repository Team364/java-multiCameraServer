
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

import team364_rpi.*;

public final class Main {

  // internals
  private static Object imgLock = new Object();
  private static ArrayList<Target> latestTargets = new ArrayList<Target>();
  private static ArrayList<Number> distance = new ArrayList<Number>();
  private static ArrayList<Number> faceAngle = new ArrayList<Number>();
  private static ArrayList<Number> width = new ArrayList<Number>();
  private static ArrayList<Number> height = new ArrayList<Number>();
  private static ArrayList<Number> centerX = new ArrayList<Number>();
  private static ArrayList<Number> centerY = new ArrayList<Number>();

  private static boolean foundTargets = false;

  private Main() { }

  /**
   * Main.
   */
  public static void main(String... args) {

    // setup and start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    NetworkTable visionTable = ntinst.getTable("visionParameters");

    // Input entries
    // NetworkTableEntry searchConfigNumber = visionTable.getEntry("searchConfigNumber"); // 0: tape, 1: ball, 2: disk
    
    // Output entries
    NetworkTableEntry visibleTargets_distance = visionTable.getEntry("visibleTargets.distance");
    NetworkTableEntry visibleTargets_faceAngle = visionTable.getEntry("visibleTargets.faceAngle");
    NetworkTableEntry visibleTargets_width = visionTable.getEntry("visibleTargets.width");
    NetworkTableEntry visibleTargets_height = visionTable.getEntry("visibleTargets.height");
    NetworkTableEntry visibleTargets_centerX = visionTable.getEntry("visibleTargets.centerX");
    NetworkTableEntry visibleTargets_centerY = visionTable.getEntry("visibleTargets.centerY");
    NetworkTableEntry visibleTargets_foundTargets = visionTable.getEntry("visibleTargets.foundTargets");

    System.out.println("Setting up NetworkTables client.");
    //ntinst.startClientTeam(364);

    // Read default camera config file
    if (!CameraStuff.readConfigFile()) {
      return;
    }

    // Start cameras
    List<VideoSource> cameras = new ArrayList<>();
    for (CameraStuff.CameraConfig cameraConfig : CameraStuff.cameraConfigs) {
      cameras.add(CameraStuff.startCamera(cameraConfig));
    }

    // Start image processing on camera 0 if present
    if (cameras.size() >= 1) {
      VisionThread visionThread = new VisionThread(cameras.get(0), new DynamicVisionPipeline(), processingPipeline -> {
        synchronized (imgLock) {
            // Setup pipeline to process TAPE, BALL, or DISK depending on NetworkTable input
            // TODO: Make this work... if we need it. Right now it's set permanently to TAPE.
            processingPipeline.setSearchConfigNumber(0); //(int)searchConfigNumber.getDouble(0));

            // Read out the latest output from the pipeline
            latestTargets = processingPipeline.findTargetsOutput();
          }
        });
      visionThread.start();
    }

    // Loop forever and ever and ever
    for (;;) {
      try {
        distance.clear();
        faceAngle.clear();
        width.clear();
        height.clear();
        centerX.clear();
        centerY.clear();

        // Populate data arrays with our latest target information
        // We have to LOCK to make sure 2nd thread doesn't change
        // latestTargets while we're reading from it
        synchronized (imgLock) {
          foundTargets = latestTargets.size() > 0;

          for (int i = 0; i < latestTargets.size(); i++) {
            Target t = latestTargets.get(i);
          
            distance.add(t.distance);
            faceAngle.add(t.faceAngle);
            width.add(t.width);
            height.add(t.height);
            centerX.add(t.centerX);
            centerY.add(t.centerY);
          }
        }

        // Write data arrays to NetworkTables
        visibleTargets_foundTargets.setBoolean(foundTargets);
        visibleTargets_distance.setDoubleArray(distance.stream().mapToDouble(i -> (double)i).toArray());
        visibleTargets_faceAngle.setDoubleArray(faceAngle.stream().mapToDouble(i -> (double)i).toArray());
        visibleTargets_width.setDoubleArray(width.stream().mapToDouble(i -> (double)i).toArray());
        visibleTargets_height.setDoubleArray(height.stream().mapToDouble(i -> (double)i).toArray());
        visibleTargets_centerX.setDoubleArray(centerX.stream().mapToDouble(i -> (double)i).toArray());
        visibleTargets_centerY.setDoubleArray(centerY.stream().mapToDouble(i -> (double)i).toArray());

        // Rest (in milliseconds)
        Thread.sleep(17); // 0.016 seconds (~60/sec), about 2x as fast as FPS

      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
