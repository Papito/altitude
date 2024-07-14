package research

import org.opencv.core.{Mat, MatOfRect}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier

import java.io.File


object FaceDetection extends App {

  def getAllFilesInDirectory(directoryPath: String): List[String] = {
    val directory = new File(directoryPath)
    directory.listFiles.filter(_.isFile)
      .filter(file =>
        file.getName.toLowerCase.endsWith(".jpg") || file.getName.toLowerCase.endsWith(".jpeg"))
      .map(_.getAbsolutePath).toList
  }

  nu.pattern.OpenCV.loadShared()
  System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)

  private val faceDetector = new CascadeClassifier(
    "/home/andrei/projects/altitude/src/main/scala/research/classifiers/haarcascade_frontalface_alt_tree.xml")

  val image: Mat =  Imgcodecs.imread("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg")

  // Detect faces in the image.
  // MatOfRect is a special container class for Rect.
  private val faceDetections = new MatOfRect()
  faceDetector.detectMultiScale(image, faceDetections)
  println(String.format("Detected %s faces", faceDetections.toList.size()))

//  val dirPath = "/home/andrei/dropbox/media/_catalog/Tessa/"
//  val allFiles = getAllFilesInDirectory(dirPath)
//  allFiles.foreach(println)
}
