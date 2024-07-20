package software.altitude.core.service

import org.apache.commons.io.FilenameUtils
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
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.Util.loadResourceAsFile
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

import java.io.File

// YUNET version of this: https://gist.github.com/papito/769dd7e4b820bcacce2ac89d385e91ce
object FaceService {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val dnnConfigurationFile: File = loadResourceAsFile("/opencv/deploy.prototxt")
  private val dnnInWidth = 300
  private val dnnInHeight = 300
  private val dnnModelFile: File = loadResourceAsFile(s"/opencv/res10_${dnnInWidth}x${dnnInHeight}_ssd_iter_140000.caffemodel")

  private val sfaceModelFile = loadResourceAsFile("/opencv/face_recognition_sface_2021dec.onnx")
  private val yunetModelFile = loadResourceAsFile("/opencv/face_detection_yunet_2022mar.onnx")
//  private val embedderModelFile = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7")

  private val dnnConfidenceThreshold = 0.37
  private val minFaceSize = 50 // minimum acceptable size of face region in pixels
  private val dnnInScaleFactor = 1.0
  private val dnnMeanVal = new Scalar(104.0, 177.0, 123.0, 128)
  private val yunetConfidenceThreshold = 0.74f

  private val dnnNet: Net = readNetFromCaffe(dnnConfigurationFile.getCanonicalPath, dnnModelFile.getCanonicalPath)

  //  private val embedderNet: Net = readNetFromTorch(embedderModelFile.getCanonicalPath)

  private val sfaceRecognizer = FaceRecognizerSF.create(sfaceModelFile.getCanonicalPath, "")

  private val yuNet = FaceDetectorYN.create(yunetModelFile.getCanonicalPath, "", new Size())
  yuNet.setScoreThreshold(yunetConfidenceThreshold)

  def detectFacesWithDnnNet(image: Mat): List[Rect] = {
    val inputBlob = blobFromImage(
      image,
      FaceService.dnnInScaleFactor,
      new Size(FaceService.dnnInWidth, FaceService.dnnInHeight),
      FaceService.dnnMeanVal, false, false, CvType.CV_32F)

    FaceService.dnnNet.setInput(inputBlob)

    val detections = FaceService.dnnNet.forward()

    // Decode detected face locations
    val di = detections.reshape(1, detections.total().asInstanceOf[Int] / 7)

    val faceRegions = {
      for (idx <- 0 until di.rows()) yield {
        val confidence = di.get(idx, 2)(0)

        if (confidence > FaceService.dnnConfidenceThreshold) {
          // logger.info("Found a face with confidence value of " + confidence)
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
    }.flatten.toList

    logger.info(s"Number of face regions found: ${faceRegions.size}")

    faceRegions
  }

  def detectFacesWithYunet(image: Mat): List[Mat] = {
    if (image.empty) {
      logger.warn("No data in image")
      return List()
    }

    val detectionResults = new Mat()

    val boundingBoxSize = 650

    val scaleFactor = determineImageScale(image.width(), image.height(), boundingBoxSize, boundingBoxSize) match {
        case scale if scale < 1.0 => scale
        case _ => 1.0
    }

    val srcMat: Mat = if (scaleFactor < 1.0) {
      val resized = new Mat()
      Imgproc.resize(image, resized, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR)
      resized
    } else {
        image.clone()
    }

    yuNet.setInputSize(srcMat.size())
    yuNet.detect(srcMat, detectionResults)
    val numOfFaces = detectionResults.rows()

    logger.info(s"Number of face regions found: $numOfFaces")

    val ret: List[Option[Mat]] = (for (idx <- 0 until numOfFaces) yield {
      val detection = detectionResults.row(idx)

      // update the original detection matrix to account for the scaling factor
      if (scaleFactor < 1.0) {
        for (col <- 0 until detection.cols()) {
          val originalValue = detection.get(0, col)(0)
          detection.put(0, col, originalValue / scaleFactor)
        }
      }

      val detectionRect = faceDetectToRect(detection)

      if (detectionRect.height < minFaceSize || detectionRect.width < minFaceSize) {
        logger.warn("Face region too small")
        None
      } else {
        Option(detection)
      }
    }).toList

    ret.flatten
  }

  def getFaceEmbedding(faceImageMat: Mat): Array[Float] = {
    val faceBlob = blobFromImage(faceImageMat, 1.0 / 255, new Size(96, 96), new Scalar(0, 0, 0), true, false)

    //    embedderNet.setInput(faceBlob)
    //    // 128-dimensional embeddings
    //    val embedderMat = embedderNet.forward()
    //    val floatMat = new MatOfFloat()
    //    embedderMat.assignTo(floatMat, CvType.CV_32F)
    //    floatMat.toArray

    new Array[Float](128)
  }

  def isFaceSimilar(image1: Mat, image2: Mat, detectMat1: Mat, detectMat2: Mat): Boolean = {
    val face1Rect = faceDetectToRect(detectMat1)
    val face2Rect = faceDetectToRect(detectMat2)
    writeDebugOpenCvMat(image1.submat(face1Rect), "face1-1.jpg")
    writeDebugOpenCvMat(image2.submat(face2Rect), "face2-1.jpg")

    val alignedFace1 = new Mat
    val alignedFace2 = new Mat
    sfaceRecognizer.alignCrop(image1, detectMat1, alignedFace1)
    sfaceRecognizer.alignCrop(image2, detectMat2, alignedFace2)
    writeDebugOpenCvMat(alignedFace1, "face1-2.jpg")
    writeDebugOpenCvMat(alignedFace2, "face2-2.jpg")

    var feature1 = new Mat
    var feature2 = new Mat
    sfaceRecognizer.feature(alignedFace1, feature1)
    feature1 = feature1.clone
    sfaceRecognizer.feature(alignedFace2, feature2)
    feature2 = feature2.clone

    val cosScore = sfaceRecognizer.`match`(feature1, feature2, FaceRecognizerSF.FR_COSINE)
    logger.info(s"Similarity cosine score: $cosScore")
    //    val L2Score = sfaceRecognizer.`match`(feature1, feature2, FaceRecognizerSF.FR_NORM_L2)
    //    logger.info(s"Similarity L2 score: $L2Score")
    //    println("L2: " + L2Score)
    cosScore >= 0.363
  }

  def faceDetectToRect(detectedFace: Mat): Rect = {
    val x = detectedFace.get(0, 0)(0).asInstanceOf[Int]
    val y = detectedFace.get(0, 1)(0).asInstanceOf[Int]
    val w = detectedFace.get(0, 2)(0).asInstanceOf[Int]
    val h = detectedFace.get(0, 3)(0).asInstanceOf[Int]
    new Rect(x, y, w, h)
  }

  def faceDetectToMat(image: Mat, detectedFace: Mat): Mat = {
    val faceRect = faceDetectToRect(detectedFace)
    image.submat(faceRect)
  }

  def matFromBytes(data: Array[Byte]): Mat = {
    Imgcodecs.imdecode(new MatOfByte(data: _*), Imgcodecs.IMREAD_ANYCOLOR)
  }

  private def determineImageScale(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Double = {
    val scaleX = targetWidth.toDouble / sourceWidth
    val scaleY = targetHeight.toDouble / sourceHeight
    Math.min(scaleX, scaleY)
  }

  private def writeDebugOpenCvMat(mat: Mat, fileName: String): Unit = {
    val outputDir = System.getenv().get("OUTPUT")

    if (outputDir == null) {
      logger.warn("OUTPUT environment variable not set for debug image writing")
      return
    }

    val outputPath = FilenameUtils.concat(outputDir, fileName)
    println(String.format("Writing %s", outputPath))
    Imgcodecs.imwrite(outputPath, mat)
  }

}

class FaceService(val app: Altitude) extends BaseService[Face] {
  Loader.load(classOf[opencv_java])

  override protected val dao: FaceDao = app.DAO.face

}
