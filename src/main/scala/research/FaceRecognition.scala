package research


import org.bytedeco.javacpp.Loader
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.objdetect.CascadeClassifier

import java.io.File

object FaceRecognition extends SandboxApp {
  val classifier = new CascadeClassifier;
  classifier.load(Loader.extractResource("lbpcascade_frontalface.xml", null, "classifier", ".xml").getAbsolutePath)
  val faceRecognizer = LBPHFaceRecognizer.create()

  override def process(path: String): Unit = {

  }

  allFilePaths.foreach(process)
}
