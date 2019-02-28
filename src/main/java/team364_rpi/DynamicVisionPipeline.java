package team364_rpi;

import java.util.ArrayList;
import java.util.List;
import edu.wpi.first.vision.VisionPipeline;
import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgproc.*;

public class DynamicVisionPipeline implements VisionPipeline {

  static {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  // Outputs
  private Mat hsvThresholdOutput = new Mat();
  private ArrayList<MatOfPoint> findContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<MatOfPoint> filterContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<RotatedRect> rotatedRectsOutput = new ArrayList<RotatedRect>();
  private ArrayList<Rect> rectsOutput = new ArrayList<Rect>();
  private ArrayList<Target> findTargetsOutput = new ArrayList<Target>();

  // Public getters
  public Mat hsvThresholdOutput() { return hsvThresholdOutput; }
  public ArrayList<MatOfPoint> findContoursOutput() { return findContoursOutput; }
  public ArrayList<MatOfPoint> filterContoursOutput() { return filterContoursOutput; }
  public ArrayList<RotatedRect> rotatedRectsOutput() { return rotatedRectsOutput; }
  public ArrayList<Rect> rectsOutput() { return rectsOutput; }
  public ArrayList<Target> findTargetsOutput() { return findTargetsOutput; }

  // Internals
  private long lastFrameTime = 0;
  private Target lastTarget = new Target();
  private int windowWidth = 150;

  // Contour Variables (w/defaults)
  double filterContoursMinArea = 100.0;
  double filterContoursMinPerimeter = 0.0;
  double filterContoursMinWidth = 0.0;
  double filterContoursMaxWidth = 999.0;
  double filterContoursMinHeight = 0.0;
  double filterContoursMaxHeight = 1000.0;
  double[] filterContoursSolidity = { 80.0, 100.0 };
  double filterContoursMaxVertices = 1000000.0;
  double filterContoursMinVertices = 0.0;
  double filterContoursMinRatio = 0.0;
  double filterContoursMaxRatio = 1000.0;

  // HSV Variables (w/defaults)
  double[] hsvThresholdHue = {0.0, 180.0};
  double[] hsvThresholdSaturation = {0.0, 116.0};
  double[] hsvThresholdValue = {169.0, 255.0};

   /**
   * This is the main method that processes the frame and finds visible targets.
   * @param source0 input frame to be processed in Mat() form.
   * @return Nothing.
   */
  @Override
  public void process(Mat source0) {

    long curFrameTime = System.currentTimeMillis();
    System.out.println("Time since frame: "+ (curFrameTime - lastFrameTime));
    lastFrameTime = System.currentTimeMillis();

    System.out.println("Last target info: " + lastTarget.width + " " + lastTarget.height);

    Mat subSource = new Mat();
    int rowStart = 0, rowEnd = 0, colStart = 0, colEnd = 0;
    if (lastTarget.width != 0 && lastTarget.height !=0) {

      rowStart = (int)(lastTarget.centerY - lastTarget.height/2) - windowWidth;
      if (rowStart <= 0) rowStart = 0;
      rowEnd = (int)(lastTarget.centerY + lastTarget.height/2) + windowWidth;
      if (rowEnd >= source0.height()-1) rowEnd = source0.height()-1;

      colStart = (int)(lastTarget.centerX - lastTarget.width/2) - windowWidth;
      if (colStart <= 0) colStart = 0;
      colEnd = (int)(lastTarget.centerX + lastTarget.width/2) + windowWidth;
      if (colEnd >= source0.width()-1) colEnd = source0.width()-1;

      //System.out.println("rs: "+ rowStart+" re: "+ rowEnd + " cs: "+ colStart+" ce: "+colEnd);
      subSource = source0.submat(rowStart, rowEnd, colStart, colEnd);
    } else subSource = source0; 

    // Step 1: HSV_Threshold0:
    Mat hsvThresholdInput = subSource;
    hsvThreshold(hsvThresholdInput, hsvThresholdHue, hsvThresholdSaturation, hsvThresholdValue, hsvThresholdOutput);

    // Step 2: Find_Contours0:
    Mat findContoursInput = hsvThresholdOutput;
    boolean findContoursExternalOnly = false;
    findContours(findContoursInput, findContoursExternalOnly, findContoursOutput);

    // Step 3: Filter_Contours0:
    ArrayList<MatOfPoint> filterContoursContours = findContoursOutput;
    filterContours(filterContoursContours, filterContoursMinArea, filterContoursMinPerimeter, filterContoursMinWidth,
        filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity,
        filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio,
        filterContoursOutput);

    // Step 4: Find RotatedRects in Contours:
    ArrayList<MatOfPoint> rotatedRectContours = filterContoursOutput;
    generateRects(rotatedRectContours, rotatedRectsOutput, rectsOutput);

    // Step 5: Find targets based on visible rectangles
    ArrayList<RotatedRect> rotRectsToCategorize = rotatedRectsOutput;
    ArrayList<Rect> rectsToCategorize = rectsOutput;
    findTargets(rowStart, colStart, curFrameTime, rotRectsToCategorize, rectsToCategorize, findTargetsOutput);

    // Save the most likely target for faster processing in the next iteration
    try {
      lastTarget = new Target();
      for (Target curTarget : findTargetsOutput) {
        if (lastTarget.distance == 0 || curTarget.distance < lastTarget.distance)
          lastTarget = curTarget;
      }
    } catch (Exception ex) {
      lastTarget = new Target();
    }
  }

   /**
   * Changes the input Mat to HSV and filters pixels based on HSV thresholds.
   * @param input input frame to be processed in Mat() form.
   * @param hue tuple containing min and max hue thresholds
   * @param sat tuple containing min and max saturation thresholds
   * @param val tuple containing min and max value thresholds
   * @param out filtered frame in Mat() form
   * @return Nothing.
   */
  private void hsvThreshold(Mat input, double[] hue, double[] sat, double[] val, Mat out) {
    // Convert the whole image from BGR to HSV
    Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2HSV);
    // Check if each individual pixel falls within HSV range
    Core.inRange(out, new Scalar(hue[0], sat[0], val[0]), new Scalar(hue[1], sat[1], val[1]), out);
  }

   /**
   * Looks for contours in input frame.
   * @param input input frame to be processed in Mat() form.
   * @param externalOnly flag to find only the external-most contour
   * @param contours list of found contours
   * @return Nothing.
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
   * Filters contours out of a list based on property bounds
   * @param inputContours input list of contours
   * @param minArea minimum desired area
   * @param minPerimeter minimum desired perimeter
   * @param minWidth minimum desired width
   * @param minVertexCount minimum desired number of vertices
   * @param minRatio minimum desired ratio
   * @param maxRatio maximum desired ratio
   * @param output filtered list of contours
   * @return Nothing.
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
   * Generates straight rectangles and rotated rectangles around input contours. The same index in outputRotatedRects and outputRects
   * corresponds to the same input contour.
   * @param inputContours list of contours to process
   * @param outputRotatedRects generated rotated rectangles
   * @param outputRects generated straight rectangles
   * @return Nothing.
   */
  private void generateRects(ArrayList<MatOfPoint> inputContours, ArrayList<RotatedRect> outputRotatedRects, ArrayList<Rect> outputRects) {
    outputRects.clear();
    outputRotatedRects.clear();

    RotatedRect rotR = new RotatedRect();
    Rect r = new Rect();
    for ( int i = 0; i < inputContours.size(); i++ ){

      MatOfPoint2f curContour2f = new MatOfPoint2f(inputContours.get(i).toArray());
      rotR = Imgproc.minAreaRect(curContour2f);
      r = Imgproc.boundingRect(inputContours.get(i));
      
      outputRects.add(r);
      outputRotatedRects.add(rotR);
    }
  }

  /**
   * Determines whether a given rectangle is angled like the left tape in FRC 2019 targets.
   * @param r
   * @return whether the rectangle is left-sided
   */
  private boolean isLeftSided(RotatedRect r) {
    // in a left-side rectangle, the longer side is considered the "width"
    return r.size.width > r.size.height;
  }

  /**
   * Finds FRC 2019 targets based on rotation and location of given rectangles.
   * @param frameRowStart vertical offset for coordinates
   * @param frameColStart horizontal offset for coordinates
   * @param timeStamp time in milliseconds at which the frame was captured
   * @param inputRotatedRects rotated rectangles drawn around potential tapes
   * @param inputRects straight rectangles drawn around potential tapes
   * @param outputTargets found targets (includes location and size information)
   */
  private void findTargets(int frameRowStart, int frameColStart, long timeStamp, ArrayList<RotatedRect> inputRotatedRects, ArrayList<Rect> inputRects, ArrayList<Target> outputTargets){
    outputTargets.clear();

    // find a left-side rectangle:
    for (int i = 0; i < inputRects.size(); i++){

      RotatedRect leftRect = inputRotatedRects.get(i);
      if (isLeftSided(leftRect)) {

        for (int j = 0; j < inputRects.size(); j++){

          RotatedRect rightRect = inputRotatedRects.get(j);
          if (!isLeftSided(rightRect)) {

            // TARGET CHECK 1: Found a right-side rect.. is it close? (i.e., is it less than 3 "widths" away?)
            // "width" (which is the length of the tape) is a poor man's way of determining this.
            if (leftRect.center.x + leftRect.size.width * 3 > rightRect.center.x && leftRect.center.x < rightRect.center.x) {
              
              // TARGET CHECK 2: Are our centerpoints aligned vertically?
              double halfH = inputRects.get(i).height / 2;
              if (rightRect.center.y < leftRect.center.y + halfH && rightRect.center.y > leftRect.center.y - halfH) {

                // We *think* we found a target! Let's figure out some stuff about it.
                Target foundTarget = new Target();
                foundTarget.timeStamp = timeStamp;
                foundTarget.centerX = frameColStart + (leftRect.center.x + rightRect.center.x)/2.0;
                foundTarget.centerY = frameRowStart + (leftRect.center.y + rightRect.center.y)/2.0;
                foundTarget.height = (inputRects.get(i).height + inputRects.get(j).height)/2.0;
                foundTarget.width = inputRects.get(j).x - inputRects.get(i).x;

                // Empirical calculations for distance and faceAngle
                foundTarget.distance = 6000.0/foundTarget.height;
                foundTarget.faceAngle = 0.00689 - Math.sqrt(0.00121148-0.000608 * foundTarget.width/foundTarget.height)*-3289.0;
                if (inputRects.get(i).height > inputRects.get(j).height) foundTarget.faceAngle = -1 * foundTarget.faceAngle;

                System.out.println("Target Found, t: " + foundTarget.timeStamp +
                                  " x: " + foundTarget.centerX + 
                                  " y: " + foundTarget.centerY +
                                  " h: " + foundTarget.height + 
                                  " w/h: " + foundTarget.width/foundTarget.height + 
                                  " d: " + foundTarget.distance + 
                                  " Ang: " + foundTarget.faceAngle);

                outputTargets.add(foundTarget);
              }
            }
          }
        }
      }
    }
  }
}