package research

import org.apache.commons.io.FileUtils
import org.opencv.core.{Mat, MatOfByte, MatOfRect, Point, Scalar}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier
import org.bytedeco.javacpp.Loader
import org.opencv.imgproc.Imgproc

import java.io.File

object HaarFaceDetection extends SandboxApp {

  override def process(path: String): Unit = {
    val file = new File(path)
    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = Imgcodecs.imdecode(new MatOfByte(fileByteArray: _*), Imgcodecs.IMREAD_GRAYSCALE)

    val faceDetector: CascadeClassifier = new CascadeClassifier("haarcascade_frontalface_default.xml");
    faceDetector.load(Loader.extractResource("haarcascade_frontalface_default.xml", null, "classifier", ".xml").getAbsolutePath)

    val faceDetections: MatOfRect = new MatOfRect()
    faceDetector.detectMultiScale(image, faceDetections)
    println(String.format("Detected %s faces", faceDetections.toList.size()))

    for ( rect <- faceDetections.toArray) yield {
      Imgproc.rectangle(image,
        new Point(rect.x, rect.y),
        new Point(rect.x + rect.width, rect.y + rect.height),
        new Scalar(0, 255, 0))
    }

    writeResult(file, image)
  }

  allFilePaths.foreach(process)
}
