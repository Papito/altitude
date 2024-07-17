package software.altitude.core.service

import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn.blobFromImage
import org.opencv.dnn.Dnn.readNetFromCaffe
import org.opencv.dnn.Net
import org.opencv.imgcodecs.Imgcodecs
import research.DeepNetFaceDetection.loadResourceAsFile
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

import java.io.File

object FaceService {
  val confidenceThreshold = 0.37
  val minFaceSize = 40 // minimum acceptable size of face region in pixels
  private val modelConfiguration: File = loadResourceAsFile("/opencv/deploy.prototxt")
  private val modelBinary: File = loadResourceAsFile("/opencv/res10_300x300_ssd_iter_140000.caffemodel")
  val inWidth = 300
  val inHeight = 300
  val inScaleFactor = 1.0
  val meanVal = new Scalar(104.0, 177.0, 123.0, 128)
  val net: Net = readNetFromCaffe(modelConfiguration.getCanonicalPath, modelBinary.getCanonicalPath)
}

class FaceService(val app: Altitude) extends BaseService[Face] {
  Loader.load(classOf[opencv_java])

  override protected val dao: FaceDao = app.DAO.face

  def detectFaces(data: Array[Byte]): List[Face] = {
    val image: Mat = Imgcodecs.imdecode(new MatOfByte(data: _*), Imgcodecs.IMREAD_ANYCOLOR)

    if (image.empty) {
      logger.warn("No data in image")
      return List()
    }

    val inputBlob = blobFromImage(
      image,
      FaceService.inScaleFactor,
      new Size(FaceService.inWidth, FaceService.inHeight),
      FaceService.meanVal, false, false, CvType.CV_32F)

    // Set the network input
    FaceService.net.setInput(inputBlob)

    // Make forward pass, compute output
    val detections = FaceService.net.forward()

    // Decode detected face locations
    val di = detections.reshape(1, detections.total().asInstanceOf[Int] / 7)

    val faceRegions = {
      for (idx <- 0 until di.rows()) yield {
        val confidence = di.get(idx, 2)(0)

        if (confidence > FaceService.confidenceThreshold) {
          logger.info("Found a face with confidence value of " + confidence)
          val x1 = (di.get(idx, 3)(0) * image.size().width).toInt
          val y1 = (di.get(idx, 4)(0) * image.size().height).toInt
          val x2 = (di.get(idx, 5)(0) * image.size().width).toInt
          val y2 = (di.get(idx, 6)(0) * image.size().height).toInt

          val rect = new Rect(new Point(x1, y1), new Point(x2, y2))

          if (rect.width < FaceService.minFaceSize || rect.height < FaceService.minFaceSize) {
            logger.warn("Face region too small")
            None
          } else {
              Option(rect)
          }
        } else {
          None
        }
      }
    }.flatten

    logger.info(s"Number of face regions found: ${faceRegions.size}")

    faceRegions.map { region =>
      Face(x1 = region.x, y1 = region.y, x2 = region.x + region.width, y2 = region.y + region.height)
    }.toList
  }
}
