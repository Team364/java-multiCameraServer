package team364_rpi;

import java.util.ArrayList;
import java.util.List;
import edu.wpi.first.vision.VisionPipeline;
import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgproc.*;

public class DynamicVisionPipeline implements VisionPipeline {

  // Inputs
  private int searchConfigNumber = 0;

  // Outputs
  private Mat hsvThresholdOutput = new Mat();
  private ArrayList<MatOfPoint> findContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<MatOfPoint> filterContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<RotatedRect> rotatedRectsOutput = new ArrayList<RotatedRect>();
  private ArrayList<Rect> rectsOutput = new ArrayList<Rect>();
  private ArrayList<Target> findTargetsOutput = new ArrayList<Target>();

  // Getters
  public Mat hsvThresholdOutput() { return hsvThresholdOutput; }
  public ArrayList<MatOfPoint> findContoursOutput() { return findContoursOutput; }
  public ArrayList<MatOfPoint> filterContoursOutput() { return filterContoursOutput; }
  public ArrayList<RotatedRect> rotatedRectsOutput() { return rotatedRectsOutput; }
  public ArrayList<Rect> rectsOutput() { return rectsOutput; }
  public int searchConfigNumber() { return searchConfigNumber; }

  // Setters
  public void setSearchConfigNumber (int inSearchConfigNumber) {
    setupSearchForTape();
  }

  static {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  // Contour Variables (w/defaults)
  double filterContoursMinArea = 100.0;
  double filterContoursMinPerimeter = 0.0;
  double filterContoursMinWidth = 0.0;
  double filterContoursMaxWidth = 999.0;
  double filterContoursMinHeight = 0.0;
  double filterContoursMaxHeight = 1000.0;
  double[] filterContoursSolidity = { 0.0, 100.0 };
  double filterContoursMaxVertices = 1000000.0;
  double filterContoursMinVertices = 0.0;
  double filterContoursMinRatio = 0.0;
  double filterContoursMaxRatio = 1000.0;

  // // HSV Variables (w/defaults)
  double[] hsvThresholdHue = {71.22302158273381, 100.13651877133105};
  double[] hsvThresholdSaturation = {22.93165467625899, 107.04778156996588};
  double[] hsvThresholdValue = {240.78237410071944, 255.0};


  private void setupSearchForTape() {
    // Tape Fliter Values
    filterContoursMinArea = 100.0;
    filterContoursMinPerimeter = 0.0;
    filterContoursMinWidth = 0.0;
    filterContoursMaxWidth = 999.0;
    filterContoursMinHeight = 0.0;
    filterContoursMaxHeight = 1000.0;
    filterContoursSolidity = new double[] { 0.0, 100.0 };
    filterContoursMaxVertices = 1000000.0;
    filterContoursMinVertices = 0.0;
    filterContoursMinRatio = 0.0;
    filterContoursMaxRatio = 1000.0;

    // Tape HSV Values
    hsvThresholdHue = new double[] {0.0, 255.0};
    hsvThresholdSaturation = new double[] {0.0, 255.0};
    hsvThresholdValue = new double[] {142.55215827338128, 255.0};

  }

  /**
   * This is the primary method that runs the entire pipeline and updates the
   * outputs.
   */
  @Override
  public void process(Mat source0) {

    // Step HSV_Threshold0:
    Mat hsvThresholdInput = source0;
    hsvThreshold(hsvThresholdInput, hsvThresholdHue, hsvThresholdSaturation, hsvThresholdValue, hsvThresholdOutput);

    // Step Find_Contours0:
    Mat findContoursInput = hsvThresholdOutput;
    boolean findContoursExternalOnly = false;
    findContours(findContoursInput, findContoursExternalOnly, findContoursOutput);

    // Step Filter_Contours0:
    ArrayList<MatOfPoint> filterContoursContours = findContoursOutput;
    filterContours(filterContoursContours, filterContoursMinArea, filterContoursMinPerimeter, filterContoursMinWidth,
        filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity,
        filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio,
        filterContoursOutput);

    // Step Find RotatedRects in Contours:
    ArrayList<MatOfPoint> rotatedRectContours = filterContoursOutput;
    generateRects(rotatedRectContours, rotatedRectsOutput, rectsOutput);

    // Step Find targets based on visible rectangles
    ArrayList<RotatedRect> rotatedRectsToCategorize = rotatedRectsOutput;
    ArrayList<Rect> rectsToCategorize = rectsOutput;
    findTargets(rotatedRectsToCategorize, rectsToCategorize, findTargetsOutput);

  }

  /**
	 * Segment an image based on hue, saturation, and value ranges.
	 *
	 * @param input The image on which to perform the HSL threshold.
	 * @param hue The min and max hue
	 * @param sat The min and max saturation
	 * @param val The min and max value
	 * @param out The image in which to store the output.
	 */
  private void hsvThreshold(Mat input, double[] hue, double[] sat, double[] val, Mat out) {
    Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2HSV);
    Core.inRange(out, new Scalar(hue[0], sat[0], val[0]), new Scalar(hue[1], sat[1], val[1]), out);
  }

  /**
	 * Finds contours in input image.
   * 
	 * @param input The image in which to find contours.
	 * @param externalOnly Whether contour retreival should be external only.
	 * @param contours The list in which to store found contours.
	 */
  private void findContours(Mat input, boolean externalOnly, List<MatOfPoint> contours) {
    Mat hierarchy = new Mat();
    contours.clear();
    int mode;
    if (externalOnly) {
      mode = Imgproc.RETR_EXTERNAL;
    } else {
      mode = Imgproc.RETR_LIST;
    }
    int method = Imgproc.CHAIN_APPROX_SIMPLE;
    Imgproc.findContours(input, contours, hierarchy, mode, method);
  }

  /**
	 * Filters out contours that do not meet certain criteria.
   * 
	 * @param inputContours is the input list of contours
	 * @param minArea is the minimum area of a contour that will be kept
	 * @param minPerimeter is the minimum perimeter of a contour that will be kept
	 * @param minWidth minimum width of a contour
	 * @param maxWidth maximum width
	 * @param minHeight minimum height
	 * @param maxHeight maximimum height
	 * @param solidity the minimum and maximum solidity of a contour
	 * @param maxVertexCount maximum vertex Count
	 * @param minVertexCount minimum vertex Count of the contours
	 * @param minRatio minimum ratio of width to height
	 * @param maxRatio maximum ratio of width to height
   * @param output is the the output list of contours
	 */
  private void filterContours(List<MatOfPoint> inputContours, double minArea, double minPerimeter, double minWidth,
      double maxWidth, double minHeight, double maxHeight, double[] solidity, double maxVertexCount,
      double minVertexCount, double minRatio, double maxRatio, List<MatOfPoint> output) {
    final MatOfInt hull = new MatOfInt();
    output.clear();

    // operation
    for (int i = 0; i < inputContours.size(); i++) {

      final MatOfPoint contour = inputContours.get(i);

      final Rect bb = Imgproc.boundingRect(contour);
      if (bb.width < minWidth || bb.width > maxWidth)
        continue;
      if (bb.height < minHeight || bb.height > maxHeight)
        continue;
      final double area = Imgproc.contourArea(contour);
      if (area < minArea)
        continue;
      if (Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) < minPerimeter)
        continue;
      Imgproc.convexHull(contour, hull);
      MatOfPoint mopHull = new MatOfPoint();
      mopHull.create((int) hull.size().height, 1, CvType.CV_32SC2);

      for (int j = 0; j < hull.size().height; j++) {
        int index = (int) hull.get(j, 0)[0];
        double[] point = new double[] { contour.get(index, 0)[0], contour.get(index, 0)[1] };
        mopHull.put(j, 0, point);
      }

      final double solid = 100 * area / Imgproc.contourArea(mopHull);
      if (solid < solidity[0] || solid > solidity[1])
        continue;
      if (contour.rows() < minVertexCount || contour.rows() > maxVertexCount)
        continue;
      final double ratio = bb.width / (double) bb.height;
      if (ratio < minRatio || ratio > maxRatio)
        continue;
      output.add(contour);
    }

  }

   /**
	 * Generates one rect and one min area rects per input contour.
   * 
	 * @param inputContours is the input list of contours
	 * @param outputRotatedRects is the list of rotated rects generated
	 * @param outputRects is the list of regular rects genereated; should be same size as outputRotatedRects
	 */
  public void generateRects(ArrayList<MatOfPoint> inputContours, ArrayList<RotatedRect> outputRotatedRects, ArrayList<Rect> outputRects) {
    
    outputRects.clear();
    outputRotatedRects.clear();

    RotatedRect rotR = new RotatedRect();
    Rect r = new Rect();
    for ( int i = 0; i < inputContours.size(); i++ ){

      // EDIT THE LINE BELOW THIS TO FIX MAT STUFF
      MatOfPoint2f curContour2f = new MatOfPoint2f(inputContours.get(i).toArray());
      rotR = Imgproc.minAreaRect(curContour2f);
      r = Imgproc.boundingRect(inputContours.get(i));
      
      outputRects.add(r);
      outputRotatedRects.add(rotR);

      System.out.println(i + ": Angle: " + rotR.angle + " h: " + r.height + " w: " +r.width+  " ctr: " + rotR.center + " size: " + rotR.size + "\n");
    }
  }

   /**
	 * Looks for FRC targets in the rects passed, based on angles and sizes.
   * 
	 * @param inputRotatedRects is the input list of rotated rects
	 * @param inputRects is the input list of regular rects
	 * @param outputTargets is the list of Targets found
	 */
  public void findTargets(ArrayList<RotatedRect> inputRotatedRects, ArrayList<Rect> inputRects, ArrayList<Target> outputTargets){

    outputTargets.clear();

    // find a left-side rectangle:
    // ArrayList<RotatedRect> rightLeaningRects = new ArrayList<RotatedRect>();
    for (int i = 0; i < inputRects.size(); i++){
      RotatedRect leftRect = inputRotatedRects.get(i);
      // in a left-side rectangle, the longer side is considered the "width"
      if (leftRect.size.width > leftRect.size.height) {
        //System.out.println("Found a left");
        // For each left-side rect, are there any nearby right-side rects?
        for (int j = 0; j < inputRects.size(); j++){
          RotatedRect rightRect = inputRotatedRects.get(j);
          if (rightRect.size.width < rightRect.size.height) {
            //System.out.println("Found a right");

            // Found a right-side rect.. is it close? (i.e., is it less than 2 "widths" away?)
            // "width" (which is the length of the tape) is a poor man's way of determining this.
            if (leftRect.center.x + leftRect.size.width * 3 > rightRect.center.x) {
              // Found a target! Let's figure out some stuff about it.
              Target foundTarget = new Target();
              foundTarget.centerX = leftRect.center.x + (rightRect.center.x - leftRect.center.x)/2;
              foundTarget.centerY = leftRect.center.y; // this may be inaccurate.
              foundTarget.distance = 0; // TODO: Actually do this...
              foundTarget.faceAngle = 90; // TODO: Actually do this...
              foundTarget.height = inputRects.get(i).height;

              //System.out.println("Found a target! x:"+foundTarget.centerX+" y:"+foundTarget.centerY);
              System.out.println("Target Found, height: " + foundTarget.height);
            }
          }
        }
      }
    }
  }


  
}

// BALL and DISK setup functions, not currently used.

// private void setupSearchForBall() {
//   // ball Filter Values
//   filterContoursMinArea = 180.0;
//   filterContoursMinPerimeter = 200.0;
//   filterContoursMinWidth = 0.0;
//   filterContoursMaxWidth = 1000.0;
//   filterContoursMinHeight = 0.0;
//   filterContoursMaxHeight = 1000.0;
//   filterContoursSolidity = new double[] { 0.0, 100.0 };
//   filterContoursMaxVertices = 1000000.0;
//   filterContoursMinVertices = 0.0;
//   filterContoursMinRatio = 0.0;
//   filterContoursMaxRatio = 1000.0;

//   // ball HSV Values
//   hsvThresholdHue = new double[] { 0.16709213274541646, 23.428933171859523 };
//   hsvThresholdSaturation = new double[] { 105.48561151079136, 255.0 };
//   hsvThresholdValue = new double[] { 6.879496402877698, 255.0 };
// }

// private void setupSearchForDisk() {
//   // Disk Filter Values
//   filterContoursMinArea = 0.0;
//   filterContoursMinPerimeter = 200.0;
//   filterContoursMinWidth = 0.0;
//   filterContoursMaxWidth = 1000.0;
//   filterContoursMinHeight = 0.0;
//   filterContoursMaxHeight = 100.0;
//   filterContoursSolidity = new double[] { 0.0, 100.0 };
//   filterContoursMaxVertices = 1000000.0;
//   filterContoursMinVertices = 0.0;
//   filterContoursMinRatio = 0.0;
//   filterContoursMaxRatio = 1000.0;

//   // Disk HSV Values
//   hsvThresholdHue = new double[] { 22.66187050359712, 37.16723549488056 };
//   hsvThresholdSaturation = new double[] { 121.53776978417264, 255.0 };
//   hsvThresholdValue = new double[] { 133.00359712230215, 255.0 };
// }

// public void setSearchConfigNumber (int inSearchConfigNumber) {
//   // Set up parameters depending on our target (default TAPE):
//   if (inSearchConfigNumber == 1) { // BALL selected
//     searchConfigNumber = 1;
//     setupSearchForBall();
//   } else if (inSearchConfigNumber == 2) { // DISK selected
//     searchConfigNumber = 2;
//     setupSearchForDisk();
//   } else { // DEFAULT/TAPE selected
//     // default
//     searchConfigNumber = 0;
//     setupSearchForTape();
//   }
// }