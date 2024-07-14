package research

import org.bytedeco.javacpp.Loader
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.objdetect.CascadeClassifier

import java.io.File


object FaceRecognition extends App {
  def getAllFilesInDirectory(directoryPath: String): List[String] = {
    val directory = new File(directoryPath)
    directory.listFiles.filter(_.isFile)
      .filter(file =>
        file.getName.toLowerCase.endsWith(".jpg") || file.getName.toLowerCase.endsWith(".jpeg"))
      .map(_.getAbsolutePath).toList
  }

  val classifier = new CascadeClassifier;
  classifier.load(Loader.extractResource("/lbpcascade_frontalface.xml", null, "classifier", ".xml").getAbsolutePath)
  val faceRecognizer = LBPHFaceRecognizer.create()

  val dirPath = "/home/andrei/dropbox/media/_catalog/Tessa/"
  val allFiles = getAllFilesInDirectory(dirPath)
  allFiles.foreach(println)
}
