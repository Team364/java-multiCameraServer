
/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;
//import java.util.List;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionThread;

import team364_rpi.*;

public final class Main {

  // internals
  private static Object imgLock = new Object();
  private static boolean updateAvailable = false;
  private static ArrayList<Target> latestTargets = new ArrayList<Target>();
  
  private static boolean foundTargets = false;
  private static long timeStamp = 0;
  private static ArrayList<Number> distance = new ArrayList<Number>();
  private static ArrayList<Number> faceAngle = new ArrayList<Number>();
  private static ArrayList<Number> width = new ArrayList<Number>();
  private static ArrayList<Number> height = new ArrayList<Number>();
  private static ArrayList<Number> centerX = new ArrayList<Number>();
  private static ArrayList<Number> centerY = new ArrayList<Number>();


  private Main() { }

  /**
   * Main.
   */
  public static void main(String... args) {

    // setup and start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    NetworkTable visionTable = ntinst.getTable("visionParameters");
    
    // Output entries
    NetworkTableEntry visibleTargets_foundTargets = visionTable.getEntry("visibleTargets.foundTargets");
    NetworkTableEntry visibleTargets_timeStamp = visionTable.getEntry("visibleTargets.timeStamp");
    NetworkTableEntry visibleTargets_distance = visionTable.getEntry("visibleTargets.distance");
    NetworkTableEntry visibleTargets_faceAngle = visionTable.getEntry("visibleTargets.faceAngle");
    NetworkTableEntry visibleTargets_width = visionTable.getEntry("visibleTargets.width");
    NetworkTableEntry visibleTargets_height = visionTable.getEntry("visibleTargets.height");
    NetworkTableEntry visibleTargets_centerX = visionTable.getEntry("visibleTargets.centerX");
    NetworkTableEntry visibleTargets_centerY = visionTable.getEntry("visibleTargets.centerY");

    // System.out.println("Setting up NetworkTables client.");
    // ntinst.startClientTeam(364);

    Camera cam0 = new Camera("/home/pi/configCamera0.json");
    VisionThread visionThread = new VisionThread(cam0.startCamera(), new DynamicVisionPipeline(), processingPipeline -> {
    synchronized (imgLock) {
        // Read out the latest output from the pipeline
        latestTargets = new ArrayList<Target>(processingPipeline.findTargetsOutput());
        updateAvailable = true;
      }
    });
    visionThread.start();

    // Loop forever and ever and ever
    for (;;) {
      try {
        // Populate data arrays with our latest target information
        // We have to LOCK to make sure 2nd thread doesn't change
        // latestTargets while we're reading from it
        synchronized (imgLock) {
          if (updateAvailable) {
            foundTargets = latestTargets.size() > 0;

            distance.clear();
            faceAngle.clear();
            width.clear();
            height.clear();
            centerX.clear();
            centerY.clear();

            for (Target t : latestTargets) {
              timeStamp = t.timeStamp;
              distance.add(t.distance);
              faceAngle.add(t.faceAngle);
              width.add(t.width);
              height.add(t.height);
              centerX.add(t.centerX);
              centerY.add(t.centerY);
            }

            updateAvailable = false;
          }
        }

        // Write data arrays to NetworkTables
        visibleTargets_foundTargets.setBoolean(foundTargets);
        visibleTargets_timeStamp.setNumber(timeStamp);
        visibleTargets_distance.setNumberArray(distance.toArray(new Number[0]));
        visibleTargets_faceAngle.setNumberArray(faceAngle.toArray(new Number[0]));
        visibleTargets_width.setNumberArray(width.toArray(new Number[0]));
        visibleTargets_height.setNumberArray(height.toArray(new Number[0]));
        visibleTargets_centerX.setNumberArray(centerX.toArray(new Number[0]));
        visibleTargets_centerY.setNumberArray(centerY.toArray(new Number[0]));

        // Rest (in milliseconds)
        Thread.sleep(16); // 0.032 seconds (~60/sec), about 4x as fast as FPS

      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
