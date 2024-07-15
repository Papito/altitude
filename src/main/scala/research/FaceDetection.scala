package research

import org.opencv.core.{Mat, MatOfRect}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java

object FaceDetection extends App {
  Loader.load(classOf[opencv_java])
  //  nu.pattern.OpenCV.loadShared()
  // System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)

  val image: Mat = Imgcodecs.imread("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg", 0)
  private val faceDetector: CascadeClassifier = new CascadeClassifier("haarcascade_frontalcatface.xml");
  faceDetector.load(Loader.extractResource("/haarcascade_frontalcatface.xml", null, "classifier", ".xml").getAbsolutePath)

  private val faceDetections: MatOfRect = new MatOfRect()
  faceDetector.detectMultiScale(image, faceDetections)
  println(String.format("Detected %s faces", faceDetections.toList.size()))

//  val dirPath = "/home/andrei/dropbox/media/_catalog/Tessa/"
//  val allFiles = getAllFilesInDirectory(dirPath)
//  allFiles.foreach(println)
}
