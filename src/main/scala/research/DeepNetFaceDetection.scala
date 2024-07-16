package research

import org.apache.commons.io.FileUtils
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.opencv.core._
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import research.HaarFaceDetection.{allFilePaths, writeResult}

import java.io.File

object DeepNetFaceDetection extends SandboxApp {
  override def process(path: String): Unit = {
  }

  allFilePaths.foreach(process)
}
