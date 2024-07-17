package software.altitude.core.service

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.opencv.global.opencv_dnn.blobFromImage
import org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_ANYCOLOR
import org.bytedeco.opencv.global.opencv_imgcodecs.imdecode
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_dnn.Net
import org.opencv.core.CvType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import research.DeepNetFaceDetection.loadResourceAsFile
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

import java.io.File

object FaceService {
  private val confidenceThreshold = 0.37
  private val minFaceSize = 40 // minimum acceptable size of face region in pixels
  private val modelConfiguration: File = loadResourceAsFile("/opencv/deploy.prototxt")
  private val modelBinary: File = loadResourceAsFile("/opencv/res10_300x300_ssd_iter_140000.caffemodel")
  private val inWidth = 300
  private val inHeight = 300
  private val inScaleFactor = 1.0
  private val meanVal = new Scalar(104.0, 177.0, 123.0, 128)
  private val net: Net = readNetFromCaffe(modelConfiguration.getCanonicalPath, modelBinary.getCanonicalPath)

  def detectFaces(data: Array[Byte]): List[Face] = {
    val logger: Logger = LoggerFactory.getLogger(getClass)

    val image: Mat = imdecode(new Mat(new BytePointer(data:_*)), IMREAD_ANYCOLOR)

    if (image.empty) {
      logger.warn("No data in image")
      return List()
    }

    val inputBlob: Mat = blobFromImage(
      image,
      FaceService.inScaleFactor,
      new Size(FaceService.inWidth, FaceService.inHeight),
      FaceService.meanVal, false, false, CvType.CV_32F)

    // Set the network input
    FaceService.net.setInput(inputBlob)

    // Make forward pass, compute output
    val detections = FaceService.net.forward()

    // Decode detected face locations
    val di = detections.createIndexer().asInstanceOf[FloatIndexer]

    val faceRegions = {
      for (i <- 0 until detections.size(2)) yield {
        val confidence = di.get(0, 0, i, 2)

        if (confidence > FaceService.confidenceThreshold) {
          logger.info("Found a face with confidence value of " + confidence)
          val x1 = (di.get(0, 0, i, 3) * image.size().width).toInt
          val y1 = (di.get(0, 0, i, 4) * image.size().height).toInt
          val x2 = (di.get(0, 0, i, 5) * image.size().width).toInt
          val y2 = (di.get(0, 0, i, 6) * image.size().height).toInt
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

class FaceService(val app: Altitude) extends BaseService[Face] {
  override protected val dao: FaceDao = app.DAO.face

}
