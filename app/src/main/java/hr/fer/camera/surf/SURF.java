package hr.fer.camera.surf;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SURF implements Serializable {

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("nonfree");
    }


    public SURF() {
    }

    public static List<Point> points = new ArrayList<>();
    private ImageView imageView;
    private Bitmap inputImage;


    public boolean detect(List<Bitmap> bitmaps) {
        try {
            FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
            DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
            DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
            Mat objectImage = new Mat();
            Mat sceneImage = new Mat();
            Mat outputImageFromObjectImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
            Mat outputImageFromSceneImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
            Scalar newKeypointColor = new Scalar(0, 0, 255);
            Scalar matchestColor = new Scalar(0, 255, 0);
            LinkedList<Point> objectPoints = new LinkedList<>();
            LinkedList<Point> scenePoints = new LinkedList<>();
            MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
            MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();


            System.out.println("Started....");
            System.out.println("Loading images...");

            Utils.bitmapToMat(bitmaps.get(0), objectImage);
            Utils.bitmapToMat(bitmaps.get(1), sceneImage);
            Imgproc.cvtColor(objectImage, objectImage, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.cvtColor(sceneImage, sceneImage, Imgproc.COLOR_RGBA2GRAY);

            Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);

            MatOfKeyPoint objectKeyPoints = detectKeyPointsOnImage(objectImage, featureDetector);
            MatOfKeyPoint objectDescriptors = detectObjectDescriptors(objectImage, objectKeyPoints, descriptorExtractor);

            drawKeyPointsOnImage(objectImage, objectKeyPoints, outputImageFromObjectImage, newKeypointColor);
            drawKeyPointsOnImage(objectImage, objectKeyPoints, outputImage, newKeypointColor);

            //Should be removed

            MatOfKeyPoint sceneKeyPoints = detectSceneKeyPoints(featureDetector, sceneImage);
            MatOfKeyPoint sceneDescriptors = detectSceneDescriptors(featureDetector, descriptorExtractor, sceneImage, sceneKeyPoints);

            drawKeyPointsOnImage(sceneImage, sceneKeyPoints, outputImageFromSceneImage, newKeypointColor);

            Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);

            List<MatOfDMatch> matches = matchObjectAndSceneImage(descriptorMatcher, objectDescriptors, sceneDescriptors);
            LinkedList<DMatch> goodMatchesList = calculateGoodMatchesList(matches);


            if (!checkIfObjectIsFound(goodMatchesList)) {
                return false; //if there are no good matches return (because nothing has been found
            }

            //------------------------------------------------------------------

            List<KeyPoint> objKeypointlist = objectKeyPoints.toList();
            List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

            for (int i = 0; i < goodMatchesList.size(); i++) {
                objectPoints.addLast(objKeypointlist.get(goodMatchesList.get(i).queryIdx).pt);
                scenePoints.addLast(scnKeypointlist.get(goodMatchesList.get(i).trainIdx).pt);
            }

            objMatOfPoint2f.fromList(objectPoints);
            scnMatOfPoint2f.fromList(scenePoints);

            Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

            Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
            Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

            obj_corners.put(0, 0, new double[]{0, 0});
            obj_corners.put(1, 0, new double[]{objectImage.cols(), 0});
            obj_corners.put(2, 0, new double[]{objectImage.cols(), objectImage.rows()});
            obj_corners.put(3, 0, new double[]{0, objectImage.rows()});

            System.out.println("Transforming object corners to scene corners...");
            Core.perspectiveTransform(obj_corners, scene_corners, homography);


            Mat img = new Mat();
            Utils.bitmapToMat(bitmaps.get(1), img);

            points = new ArrayList<>();
            points.add(new Point(scene_corners.get(0, 0)));
            points.add(new Point(scene_corners.get(1, 0)));
            points.add(new Point(scene_corners.get(1, 0)));
            points.add(new Point(scene_corners.get(2, 0)));
            points.add(new Point(scene_corners.get(2, 0)));
            points.add(new Point(scene_corners.get(3, 0)));
            points.add(new Point(scene_corners.get(3, 0)));
            points.add(new Point(scene_corners.get(0, 0)));


            Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 255), 10);
            Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 255), 10);
            Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 255), 10);
            Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 255), 10);

            System.out.println("Drawing matches image...");
            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(goodMatchesList);

            Features2d.drawMatches(objectImage, objectKeyPoints, sceneImage, sceneKeyPoints, goodMatches, matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);

            Highgui.imwrite("outputImage.jpg", outputImage);
            Highgui.imwrite("matchoutput.jpg", matchoutput);
            Highgui.imwrite("img.jpg", img);

            Bitmap endBitmap = convertOutputToBitmap(img);
            Bitmap outputBitmap = convertOutputToBitmap(outputImage);
            Bitmap sceneBitmap = convertOutputToBitmap(sceneImage);

            System.out.println("Processing finished!");

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;

    }


    private MatOfKeyPoint detectKeyPointsOnImage(Mat objectImage, FeatureDetector featureDetector) {
        MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
        System.out.println("Detecting key points...");
        featureDetector.detect(objectImage, objectKeyPoints);
        System.out.println(Arrays.toString(objectKeyPoints.toArray()));
        return objectKeyPoints;
    }


    private MatOfKeyPoint detectObjectDescriptors(Mat objectImage, MatOfKeyPoint objectKeyPoints, DescriptorExtractor descriptorExtractor) {
        MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
        System.out.println("Computing descriptors...");
        descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
        return objectDescriptors;
    }

    private MatOfKeyPoint detectSceneKeyPoints(FeatureDetector featureDetector, Mat sceneImage) {
        MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
        System.out.println("Detecting key points in background image...");
        featureDetector.detect(sceneImage, sceneKeyPoints);
        return sceneKeyPoints;
    }


    private MatOfKeyPoint detectSceneDescriptors(FeatureDetector featureDetector, DescriptorExtractor descriptorExtractor, Mat sceneImage, MatOfKeyPoint sceneKeyPoints) {
        MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
        System.out.println("Computing descriptors in background image...");
        descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);
        return sceneDescriptors;
    }

    private List<MatOfDMatch> matchObjectAndSceneImage(DescriptorMatcher descriptorMatcher, MatOfKeyPoint objectDescriptors, MatOfKeyPoint sceneDescriptors) {
        List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
        System.out.println("Matching object and scene images...");
        descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);
        return matches;
    }

    private void drawKeyPointsOnImage(Mat objectImage, MatOfKeyPoint objectKeyPoints, Mat outputImage, Scalar newKeypointColor) {
        System.out.println("Drawing key points on object image...");
        Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);
    }

    private LinkedList<DMatch> calculateGoodMatchesList(List<MatOfDMatch> matches) {
        System.out.println("Calculating good match list...");
        LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

        float nndrRatio = 0.7f;

        for (int i = 0; i < matches.size(); i++) {
            MatOfDMatch matofDMatch = matches.get(i);
            DMatch[] dmatcharray = matofDMatch.toArray();
            DMatch m1 = dmatcharray[0];
            DMatch m2 = dmatcharray[1];

            if (m1.distance <= m2.distance * nndrRatio) {
                goodMatchesList.addLast(m1);

            }
        }

        return goodMatchesList;
    }

    private boolean checkIfObjectIsFound(LinkedList<DMatch> goodMatchesList) {
        if (goodMatchesList.size() >= 100) {
            System.out.println("Object Found!!!");
            return true;
        }
        return false;
    }

    private Bitmap convertOutputToBitmap(Mat img) {
        Bitmap bmp;
        try {
            //Imgproc.cvtColor(img, img, Imgproc.COLOR_GRAY2RGBA);
            bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
            return bmp;
        } catch (CvException e) {
            Log.e("Exception", e.getLocalizedMessage());
        }
        return null;
    }

    public List<MatOfKeyPoint> getAllObjectsKeypoints(List<Bitmap> bitmaps){
        List<MatOfKeyPoint> allDescriptorsKeyPoints = new ArrayList<MatOfKeyPoint>();
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        for (Bitmap bitmap : bitmaps){
            Mat objectImage = new Mat();
            Utils.bitmapToMat(bitmap, objectImage);
            MatOfKeyPoint objectKeyPoints = detectKeyPointsOnImage(objectImage, featureDetector);
            allDescriptorsKeyPoints.add(objectKeyPoints);
        }
        return allDescriptorsKeyPoints;
    }

}