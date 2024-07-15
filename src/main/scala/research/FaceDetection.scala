package research

import org.opencv.core.{Mat, MatOfRect}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier

import java.io.File
import org.bytedeco.javacpp.{BytePointer, DoublePointer, IntPointer, Loader}
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_face._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_face._
import org.bytedeco.opencv.global.opencv_imgcodecs._
// import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_imgcodecs.imread

import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java

object FaceDetection extends App {
  Loader.load(classOf[opencv_java])
  //  nu.pattern.OpenCV.loadShared()
  // System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
  val img = imread("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg", IMREAD_GRAYSCALE)
  Imgcodecs.imread("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg")
//  def getAllFilesInDirectory(directoryPath: String): List[String] = {
//    val directory = new File(directoryPath)
//    directory.listFiles.filter(_.isFile)
//      .filter(file =>
//        file.getName.toLowerCase.endsWith(".jpg") || file.getName.toLowerCase.endsWith(".jpeg"))
//      .map(_.getAbsolutePath).toList
//  }

  private val faceDetector = new CascadeClassifier()

  faceDetector.load("/home/andrei/projects/altitude/src/main/scala/research/data/haarcascade_frontalface_alt_tree.xml");

//
  val image: Mat =  Imgcodecs.imread("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg")
//
//  // Detect faces in the image.
//  // MatOfRect is a special container class for Rect.
  private val faceDetections = new MatOfRect()
  faceDetector.detectMultiScale(image, faceDetections)
  println(String.format("Detected %s faces", faceDetections.toList.size()))

//  val dirPath = "/home/andrei/dropbox/media/_catalog/Tessa/"
//  val allFiles = getAllFilesInDirectory(dirPath)
//  allFiles.foreach(println)
}
