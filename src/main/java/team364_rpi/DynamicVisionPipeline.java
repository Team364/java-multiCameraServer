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
  private int searchConfigNumber = 0;
  public double xValue;

  public void setSearchConfigNumber (int inSearchConfigNumber) {
    // Set up parameters depending on our target (default TAPE):
    if (inSearchConfigNumber == 1) { // BALL selected
      searchConfigNumber = 1;
      setupSearchForBall();
    } else if (inSearchConfigNumber == 2) { // DISK selected
      searchConfigNumber = 2;
      setupSearchForDisk();
    } else { // DEFAULT/TAPE selected
      // default
      searchConfigNumber = 0;
      setupSearchForTape();
    }
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
  double[] filterContoursSolidity = { 0.0, 100.0 };
  double filterContoursMaxVertices = 1000000.0;
  double filterContoursMinVertices = 0.0;
  double filterContoursMinRatio = 0.0;
  double filterContoursMaxRatio = 1000.0;

  // // HSV Variables (w/defaults)
  // private double[] hsvThresholdHue = {71.22302158273381, 100.13651877133105};
  // private double[] hsvThresholdSaturation = {22.93165467625899,
  // 107.04778156996588};
  // private double[] hsvThresholdValue = {240.78237410071944, 255.0};
  double[] hsvThresholdHue = { 0.0, 180.0 };
  double[] hsvThresholdSaturation = { 0, 255.0 };
  double[] hsvThresholdValue = { 0, 255.0 };

  public void setHsvThresholdParameters(double[] inHsvThresholdHue, double[] inHsvThresholdSaturation,
      double[] inHsvThresholdValue) {

    hsvThresholdHue = inHsvThresholdHue;
    hsvThresholdSaturation = inHsvThresholdSaturation;
    hsvThresholdValue = inHsvThresholdValue;
  }

  public void setFilterContours(double inFilterContoursMinArea, double inFilterContoursMinPerimeter,
      double inFilterContoursMinWidth, double inFilterContoursMaxWidth, double inFilterContoursMinHeight,
      double inFilterContoursMaxHeight, double[] inFilterContoursSolidity, double inFilterContoursMaxVertices,
      double inFilterContoursMinVertices, double inFilterContoursMinRatio, double inFilterContoursMaxRatio) {

    filterContoursMinArea = inFilterContoursMinArea;
    filterContoursMinPerimeter = inFilterContoursMinPerimeter;
    filterContoursMinWidth = inFilterContoursMinWidth;
    filterContoursMaxWidth = inFilterContoursMaxWidth;
    filterContoursMinHeight = inFilterContoursMinHeight;
    filterContoursMaxHeight = inFilterContoursMaxHeight;
    filterContoursSolidity = inFilterContoursSolidity;
    filterContoursMaxVertices = inFilterContoursMaxVertices;
    filterContoursMinVertices = inFilterContoursMinVertices;
    filterContoursMinRatio = inFilterContoursMinRatio;
    filterContoursMaxRatio = inFilterContoursMaxRatio;
  }

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
    hsvThresholdHue = new double[] { 71.22302158273381, 100.13651877133105 };
    hsvThresholdSaturation = new double[] { 22.93165467625899, 107.04778156996588 };
    hsvThresholdValue = new double[] { 240.78237410071944, 255.0 };

  }

  private void setupSearchForBall() {
    // ball Filter Values
    filterContoursMinArea = 180.0;
    filterContoursMinPerimeter = 200.0;
    filterContoursMinWidth = 0.0;
    filterContoursMaxWidth = 1000.0;
    filterContoursMinHeight = 0.0;
    filterContoursMaxHeight = 1000.0;
    filterContoursSolidity = new double[] { 0.0, 100.0 };
    filterContoursMaxVertices = 1000000.0;
    filterContoursMinVertices = 0.0;
    filterContoursMinRatio = 0.0;
    filterContoursMaxRatio = 1000.0;

    // ball HSV Values
    hsvThresholdHue = new double[] { 0.16709213274541646, 23.428933171859523 };
    hsvThresholdSaturation = new double[] { 105.48561151079136, 255.0 };
    hsvThresholdValue = new double[] { 6.879496402877698, 255.0 };
  }

  private void setupSearchForDisk() {
    // Disk Filter Values
    filterContoursMinArea = 0.0;
    filterContoursMinPerimeter = 200.0;
    filterContoursMinWidth = 0.0;
    filterContoursMaxWidth = 1000.0;
    filterContoursMinHeight = 0.0;
    filterContoursMaxHeight = 100.0;
    filterContoursSolidity = new double[] { 0.0, 100.0 };
    filterContoursMaxVertices = 1000000.0;
    filterContoursMinVertices = 0.0;
    filterContoursMinRatio = 0.0;
    filterContoursMaxRatio = 1000.0;

    // Disk HSV Values
    hsvThresholdHue = new double[] { 22.66187050359712, 37.16723549488056 };
    hsvThresholdSaturation = new double[] { 121.53776978417264, 255.0 };
    hsvThresholdValue = new double[] { 133.00359712230215, 255.0 };
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
  }

  public Mat hsvThresholdOutput() {
    return hsvThresholdOutput;
  }

  public ArrayList<MatOfPoint> findContoursOutput() {
    return findContoursOutput;
  }

  public ArrayList<MatOfPoint> filterContoursOutput() {
    return filterContoursOutput;
  }

  private void hsvThreshold(Mat input, double[] hue, double[] sat, double[] val, Mat out) {
    Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2HSV);
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

}