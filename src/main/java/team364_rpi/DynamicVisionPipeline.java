package team364_rpi;

import java.util.ArrayList;
import java.util.List;

import edu.wpi.first.vision.VisionPipeline;
import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgproc.*;

public class DynamicVisionPipeline implements VisionPipeline {

  // Outputs
  private Mat hsvThresholdOutput = new Mat();
  private ArrayList<MatOfPoint> findContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<MatOfPoint> filterContoursOutput = new ArrayList<MatOfPoint>();
  private ArrayList<RotatedRect> rotatedRectsOutput = new ArrayList<RotatedRect>();
  private ArrayList<Rect> rectsOutput = new ArrayList<Rect>();
  private ArrayList<Target> findTargetsOutput = new ArrayList<Target>();
  private static long lastFrameTime = 0;
  private Target lastTarget = new Target();
  // private static int lCenterX = 0;
  // private static int lCenterY = 0;
  // private static int lwidth = 0;
  // private static int lheight = 0;


  // Inputs
  private int searchConfigNumber = 0;

  public void setSearchConfigNumber (int inSearchConfigNumber) {
    // // Set up parameters depending on our target (default TAPE):
    // if (inSearchConfigNumber == 1) { // BALL selected
    //   searchConfigNumber = 1;
    //   setupSearchForBall();
    // } else if (inSearchConfigNumber == 2) { // DISK selected
    //   searchConfigNumber = 2;
    //   setupSearchForDisk();
    // } else { // DEFAULT/TAPE selected
    //   // default
    //   searchConfigNumber = 0;
      setupSearchForTape();
    //}
  }

  public int searchConfigNumber() { return searchConfigNumber;}

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
  double[] filterContoursSolidity = { 80.0, 100.0 };
  double filterContoursMaxVertices = 1000000.0;
  double filterContoursMinVertices = 0.0;
  double filterContoursMinRatio = 0.0;
  double filterContoursMaxRatio = 1000.0;

  // HSV Variables (w/defaults)
  double[] hsvThresholdHue = {0.0, 180.0};
  double[] hsvThresholdSaturation = {0.0, 116.0};
  double[] hsvThresholdValue = {169.0, 255.0};

  private void setupSearchForTape() {
    // Tape Fliter Values
    filterContoursMinArea = 100.0;
    filterContoursMinPerimeter = 0.0;
    filterContoursMinWidth = 0.0;
    filterContoursMaxWidth = 999.0;
    filterContoursMinHeight = 0.0;
    filterContoursMaxHeight = 1000.0;
    filterContoursSolidity = new double[] { 80.0, 100.0 };
    filterContoursMaxVertices = 1000000.0;
    filterContoursMinVertices = 0.0;
    filterContoursMinRatio = 0.0;
    filterContoursMaxRatio = 1000.0;

    // Tape HSV Values
    hsvThresholdHue = new double[] { 0.0, 180.0};
    hsvThresholdSaturation = new double[] { 0.0, 116.0 };
    hsvThresholdValue = new double[] { 169.0, 255.0 };

  }

  /**
   * This is the primary method that runs the entire pipeline and updates the
   * outputs.
   */
  @Override
  public void process(Mat source0) {

    System.out.println("Time since frame: "+ (System.currentTimeMillis()-lastFrameTime));
    lastFrameTime = System.currentTimeMillis();
    System.out.println("Last target info: " + lastTarget.width + " " + lastTarget.height);

    Mat subSource = new Mat();

    if (lastTarget.width != 0 && lastTarget.height !=0) {

      int rowStart = (int) (lastTarget.centerY - lastTarget.height/2 - 50);
      if (rowStart <= 0) rowStart = 0;
      int rowEnd = (int) (lastTarget.centerY + lastTarget.height/2 + 50);
      if (rowEnd >= source0.height()-1) rowEnd = source0.height()-1;

      int colStart = (int) (lastTarget.centerX - lastTarget.width/2 - 50);
      if (colStart <= 0) colStart = 0;
      int colEnd = (int) (lastTarget.centerX + lastTarget.width/2 + 50);
      if (colEnd >= source0.width()-1) colEnd = source0.width()-1;

      System.out.println("rs: "+ rowStart+" re: "+ rowEnd + " cs: "+ colStart+" ce: "+colEnd);
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
    findTargets(rotRectsToCategorize, rectsToCategorize, findTargetsOutput);

    //System.out.println("findtarget size:" + findTargetsOutput.size());
    try {
      lastTarget = findTargetsOutput.get(0);
    } catch (Exception ex) {
      lastTarget = new Target();
    }
    //lastTarget = findTargetsOutput.size()>0 ? findTargetsOutput.get(0) : null;
  }

  public Mat hsvThresholdOutput() { return hsvThresholdOutput; }
  public ArrayList<MatOfPoint> findContoursOutput() { return findContoursOutput; }
  public ArrayList<MatOfPoint> filterContoursOutput() { return filterContoursOutput; }
  public ArrayList<RotatedRect> rotatedRectsOutput() { return rotatedRectsOutput; }
  public ArrayList<Target> findTargetsOutput() { return findTargetsOutput; }

  private void hsvThreshold(Mat input, double[] hue, double[] sat, double[] val, Mat out) {
    // Convert the whole image from BGR to HSV
    Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2HSV);
    // Check if each individual pixel falls within HSV range
    Core.inRange(out, new Scalar(hue[0], sat[0], val[0]), new Scalar(hue[1], sat[1], val[1]), out);
  }

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

  public void generateRects(ArrayList<MatOfPoint> inputContours, ArrayList<RotatedRect> outputRotatedRects, ArrayList<Rect> outputRects) {
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

  public boolean isLeftSided(RotatedRect r) {
    // in a left-side rectangle, the longer side is considered the "width"
    return r.size.width > r.size.height;
  }

  public void findTargets(ArrayList<RotatedRect> inputRotatedRects, ArrayList<Rect> inputRects, ArrayList<Target> outputTargets){

    outputTargets.clear();

    // find a left-side rectangle:
    for (int i = 0; i < inputRects.size(); i++){
      RotatedRect leftRect = inputRotatedRects.get(i);
      if (isLeftSided(leftRect)) {

        for (int j = 0; j < inputRects.size(); j++){
          RotatedRect rightRect = inputRotatedRects.get(j);
          if (!isLeftSided(rightRect)) {

            // TARGET CHECK 1: Found a right-side rect.. is it close? (i.e., is it less than 2 "widths" away?)
            // "width" (which is the length of the tape) is a poor man's way of determining this.
            if (leftRect.center.x + leftRect.size.width * 3 > rightRect.center.x && leftRect.center.x < rightRect.center.x) {
              
              // TARGET CHECK 2: Are our centerpoints aligned vertically?
              double leftHalfHeight = inputRects.get(i).height / 2;
              if (rightRect.center.y < leftRect.center.y + leftHalfHeight
                  && rightRect.center.y > leftRect.center.y - leftHalfHeight) {

                // We *think* Found a target! Let's figure out some stuff about it.
                Target foundTarget = new Target();
                foundTarget.centerX = (leftRect.center.x + rightRect.center.x)/2;
                foundTarget.centerY = (leftRect.center.y + rightRect.center.y)/2;
                foundTarget.height = (inputRects.get(i).height + inputRects.get(j).height)/2;
                foundTarget.width = (inputRects.get(j).x - inputRects.get(i).x);

                // empirical calculations for distance and faceAngle
                foundTarget.distance = 6000/foundTarget.height; //1890 / foundTarget.height;

                double y = foundTarget.width/foundTarget.height;
                foundTarget.faceAngle = (0.00689 - Math.sqrt(0.00121148-0.000608 * y))*-3289;
                if (inputRects.get(i).height > inputRects.get(j).height) foundTarget.faceAngle = -1 * foundTarget.faceAngle;

                System.out.println("Target Found, h: " + foundTarget.height + " w/h: "+ foundTarget.width/foundTarget.height + 
                " d: " + foundTarget.distance + " Ang: "+foundTarget.faceAngle);

                outputTargets.add(foundTarget);
              }
            }
          }
        }
      }
    }
  }
}

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