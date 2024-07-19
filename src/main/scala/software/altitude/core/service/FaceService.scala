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
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.{FaceDetectorYN, FaceRecognizerSF}
import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.Altitude
import software.altitude.core.Util.loadResourceAsFile
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

import java.io.File

// YUNET version of this: https://gist.github.com/papito/769dd7e4b820bcacce2ac89d385e91ce
object FaceService {
  private val confidenceThreshold = 0.37
  private val minFaceSize = 40 // minimum acceptable size of face region in pixels
  private val modelConfigurationFile: File = loadResourceAsFile("/opencv/deploy.prototxt")
  private val modelFile: File = loadResourceAsFile("/opencv/res10_300x300_ssd_iter_140000.caffemodel")
  private val sfaceModelFile = loadResourceAsFile("/opencv/face_recognition_sface_2021dec.onnx")
  private val yunetModelFilePath = loadResourceAsFile("/opencv/face_detection_yunet_2022mar.onnx")
  private val inWidth = 300
  private val inHeight = 300
  private val inScaleFactor = 1.0
  private val meanVal = new Scalar(104.0, 177.0, 123.0, 128)
  private val dnnNet: Net = readNetFromCaffe(modelConfigurationFile.getCanonicalPath, modelFile.getCanonicalPath)

  private val sfaceRecognizer = FaceRecognizerSF.create(sfaceModelFile.getCanonicalPath, "")
  private val yuNet = FaceDetectorYN.create(yunetModelFilePath.getCanonicalPath, "", new Size())
  yuNet.setScoreThreshold(.9f)

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def detectFacesWithDnnNet(data: Array[Byte]): List[Face] = {
    val image: Mat = matFromBytes(data)

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
    FaceService.dnnNet.setInput(inputBlob)

    // Make forward pass, compute output
    val detections = FaceService.dnnNet.forward()

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

  def detectFacesWithYunet(data: Array[Byte]): List[Face] = {
    val logger: Logger = LoggerFactory.getLogger(getClass)

    val image: Mat = matFromBytes(data)

    if (image.empty) {
      logger.warn("No data in image")
      return List()
    }

    // Set the network input

    val detectionResults = new Mat()
    yuNet.setInputSize(new Size(image.width(), image.height()))
    // Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB)
    yuNet.detect(image, detectionResults)
    val numOfFaces = detectionResults.rows()

    logger.info(s"Number of face regions found: $numOfFaces")

    val faces = for (idx <- 0 until numOfFaces) yield {
      val detectionResult = detectionResults.row(idx)
      val asRect = faceDetectToRect(detectionResult)

      val f = Face(
        x1 = asRect.x,
        y1 = asRect.y,
        x2 = asRect.width,
        y2 = asRect.height)
      f
    }

    faces.toList
  }

  def isFaceSimilar(image1: Mat, image2: Mat, faceMat1: Mat, faceMat2: Mat): Boolean = {
    val alignedFace1 = new Mat
    val alignedFace2 = new Mat
    sfaceRecognizer.alignCrop(image1, faceMat1, alignedFace1)
    sfaceRecognizer.alignCrop(image2, faceMat2, alignedFace2)
//    Imgcodecs.imwrite("/home/andrei/output/face1.jpg", alignedFace1)
//    Imgcodecs.imwrite("/home/andrei/output/face2.jpg", alignedFace2)

    var feature1 = new Mat
    var feature2 = new Mat
    sfaceRecognizer.feature(alignedFace1, feature1)
    feature1 = feature1.clone
    sfaceRecognizer.feature(alignedFace2, feature2)
    feature2 = feature2.clone

    val cosScore = sfaceRecognizer.`match`(feature1, feature2, FaceRecognizerSF.FR_COSINE)
    val L2Score = sfaceRecognizer.`match`(feature1, feature2, FaceRecognizerSF.FR_NORM_L2)
    logger.info(s"Similarity cosine score: $cosScore")
    logger.info(s"Similarity L2 score: $L2Score")
    println(cosScore)
    println(L2Score)
    println()
    cosScore >= 0.363
  }


  private def faceDetectToRect(detectedFace: Mat): Rect = {
    val x = detectedFace.get(0, 0)(0).asInstanceOf[Int]
    val y = detectedFace.get(0, 1)(0).asInstanceOf[Int]
    val w = detectedFace.get(0, 2)(0).asInstanceOf[Int]
    val h = detectedFace.get(0, 3)(0).asInstanceOf[Int]
    new Rect(x, y, x + w, y + h)
  }

  def faceToRect(face: Face): Rect = {
    new Rect(face.x1, face.y1, face.x2, face.y2)
  }

  def faceToMat(face: Face): Mat = {
    // Create a Mat with 1 row and 4 columns
    val mat = new Mat(1, 4, CvType.CV_32F)

    mat.put(0, 0, face.x1.toDouble)
    mat.put(0, 1, face.y1.toDouble)
    mat.put(0, 2, face.x2.toDouble - face.x1.toDouble)
    mat.put(0, 3, face.y2.toDouble - face.y1.toDouble)

    mat
  }

  def matFromBytes(data: Array[Byte]): Mat = {
    Imgcodecs.imdecode(new MatOfByte(data: _*), Imgcodecs.IMREAD_ANYCOLOR)
  }

}

class FaceService(val app: Altitude) extends BaseService[Face] {
  Loader.load(classOf[opencv_java])

  override protected val dao: FaceDao = app.DAO.face

}
