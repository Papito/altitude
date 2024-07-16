package research

import org.apache.commons.io.FileUtils
import org.opencv.core.{Core, Mat, MatOfByte, MatOfRect, Point, Scalar}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.opencv.imgproc.Imgproc

import java.io.File

object FaceDetection extends App {
  Loader.load(classOf[opencv_java])

  val file = new File("/home/andrei/dropbox/media/_catalog/Tessa/DSC_0495.jpg")

  private val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
  val image: Mat = Imgcodecs.imdecode(new MatOfByte(fileByteArray: _*), Imgcodecs.IMREAD_GRAYSCALE)

  private val faceDetector: CascadeClassifier = new CascadeClassifier("haarcascade_frontalface_default.xml");
  faceDetector.load(Loader.extractResource("haarcascade_frontalface_default.xml", null, "classifier", ".xml").getAbsolutePath)

  private val faceDetections: MatOfRect = new MatOfRect()
  faceDetector.detectMultiScale(image, faceDetections)
  println(String.format("Detected %s faces", faceDetections.toList.size()))


  for ( rect <- faceDetections.toArray) yield {
    Imgproc.rectangle(image,
      new Point(rect.x, rect.y),
      new Point(rect.x + rect.width, rect.y + rect.height),
      new Scalar(0, 255, 0))
  }

  // Save the visualized detection.
  val filename = "/home/andrei/faceDetection.png"
  println(String.format("Writing %s", filename))
  Imgcodecs.imwrite(filename, image)
//  val dirPath = "/home/andrei/dropbox/media/_catalog/Tessa/"
//  val allFiles = getAllFilesInDirectory(dirPath)
//  allFiles.foreach(println)
}
