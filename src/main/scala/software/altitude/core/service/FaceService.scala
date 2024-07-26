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
import org.opencv.dnn.Dnn.{blobFromImage, readNetFromCaffe, readNetFromTorch}
import org.opencv.dnn.Net
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

object FaceService {
  private val dnnInWidth = 300
  private val dnnInHeight = 300

  private val dnnConfidenceThreshold = 0.37
  private val minFaceSize = 50 // minimum acceptable size of face region in pixels
  private val dnnInScaleFactor = 1.0
  private val dnnMeanVal = new Scalar(104.0, 177.0, 123.0, 128)
  private val yunetConfidenceThreshold = 0.855f

  val cosineSimilarityThreshold = 0.363

  def faceDetectToRect(detectedFace: Mat): Rect = {
    val origX = detectedFace.get(0, 0)(0).asInstanceOf[Int]
    val origY = detectedFace.get(0, 1)(0).asInstanceOf[Int]

    val x = Math.max(0, origX)
    val y = Math.max(0, origY)

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

  def writeDebugOpenCvMat(mat: Mat, fileName: String): Unit = {
    val outputDir = System.getenv().get("OUTPUT")

    if (outputDir == null) {
      println("OUTPUT environment variable not set for debug image writing")
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

  private val sfaceRecognizer = FaceRecognizerSF.create("src/main/resources/opencv/face_recognition_sface_2021dec.onnx", "")

  private val dnnNet: Net = readNetFromCaffe(
    "src/main/resources/opencv/deploy.prototxt",
    s"src/main/resources/opencv/res10_${FaceService.dnnInWidth}x${FaceService.dnnInHeight}_ssd_iter_140000.caffemodel")

  private val embedder = readNetFromTorch("src/main/resources/opencv/openface_nn4.small2.v1.t7", true)

  private val yuNet = FaceDetectorYN.create("src/main/resources/opencv/face_detection_yunet_2022mar.onnx", "", new Size())
  yuNet.setScoreThreshold(FaceService.yunetConfidenceThreshold)
  yuNet.setNMSThreshold(0.2f)

  def detectFacesWithDnnNet(image: Mat): List[Rect] = {
    val inputBlob = blobFromImage(
      image,
      FaceService.dnnInScaleFactor,
      new Size(FaceService.dnnInWidth, FaceService.dnnInHeight),
      FaceService.dnnMeanVal, false, false, CvType.CV_32F)

    dnnNet.setInput(inputBlob)

    val detections = dnnNet.forward()

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

    val boundingBoxSize = 640

    // println("OG Image size: " + image.size())
    val scaleFactor = FaceService.determineImageScale(image.width(), image.height(), boundingBoxSize, boundingBoxSize) match {
      case scale if scale < 1.0 => scale
      case _ => 1.0
    }
    // println("Scale factor: " + scaleFactor)

    val srcMat: Mat = if (scaleFactor < 1.0) {
      val resized = new Mat()
      Imgproc.resize(image, resized, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR_EXACT)
      resized
    } else {
      image.clone()
    }

    // println("Resized Image size: " + srcMat.size())

    yuNet.setInputSize(srcMat.size())
    yuNet.detect(srcMat, detectionResults)
    val numOfFaces = detectionResults.rows()

    // logger.info(s"Number of face regions found: $numOfFaces")

    val ret: List[Option[Mat]] = (for (idx <- 0 until numOfFaces) yield {
      val detection = detectionResults.row(idx)

      // println("Detection: " + detection.dump())
      // update the original detection matrix to account for the scaling factor
      if (scaleFactor < 1.0) {
        for (col <- 0 until detection.cols()) {
          val originalValue = detection.get(0, col)(0)
          detection.put(0, col, originalValue / scaleFactor)
        }
      }
      // println("Scaled detection: " + detection.dump())

      val detectionRect = FaceService.faceDetectToRect(detection)

      if (detectionRect.height < FaceService.minFaceSize || detectionRect.width < FaceService.minFaceSize) {
        logger.warn("Face region too small")
        None
      } else {
        Option(detection)
      }
    }).toList

    ret.flatten
  }

  def train(mat: Mat): Unit = {
  }

  def alignCropFaceFromDetection(image: Mat, detection: Mat): Mat = {
    val alignedFace = new Mat
    sfaceRecognizer.alignCrop(image, detection, alignedFace)
    alignedFace
  }

  def getFacialFeatures(image: Mat): Mat = {
    val feature = new Mat
    sfaceRecognizer.feature(image, feature)
    feature
  }

  def getEmbeddings(trainingImage: Mat): Array[Float] = {
    val alignedFaceBlob = getAlignedFaceBlob(trainingImage)
    embedder.setInput(alignedFaceBlob)
    val embeddingsMat = embedder.forward
    val embeddings = new Array[Float](128)
    embeddingsMat.get(0, 0, embeddings)
    embeddings
  }

  def getHistEqualizedGrayScImage(cropAlignedFace: Mat): Mat = {
    val greyAlignedImage = new Mat()
    Imgproc.cvtColor(cropAlignedFace, greyAlignedImage, Imgproc.COLOR_BGR2GRAY)
    Imgproc.equalizeHist(greyAlignedImage, greyAlignedImage)
    greyAlignedImage
  }

  private def getAlignedFaceBlob(image: Mat): Mat = {
    val resized = new Mat
    Imgproc.resize(image, resized, new Size(96, 96))
    Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2RGB)
    blobFromImage(resized, 1.0 / 255, new Size(96, 96), new Scalar(0, 0, 0), true, false)
  }

  def isFaceSimilar(image1: Mat, image2: Mat, detectMat1: Mat, detectMat2: Mat): Boolean = {
    // val face1Rect = faceDetectToRect(detectMat1)
    // val face2Rect = faceDetectToRect(detectMat2)
    // writeDebugOpenCvMat(image1.submat(face1Rect), "face1-1.jpg")
    // writeDebugOpenCvMat(image2.submat(face2Rect), "face2-1.jpg")

    val alignedFace1 = alignCropFaceFromDetection(image1, detectMat1)
    val alignedFace2 = alignCropFaceFromDetection(image2, detectMat2)

    // writeDebugOpenCvMat(alignedFace1, "face1-2.jpg")
    // writeDebugOpenCvMat(alignedFace2, "face2-2.jpg")

    val feature1 = getFacialFeatures(alignedFace1).clone()
    val feature2 = getFacialFeatures(alignedFace2).clone()

    getFeatureSimilarityScore(feature1, feature2) >= FaceService.cosineSimilarityThreshold
  }

  def getFeatureSimilarityScore(feature1: Mat, feature2: Mat): Double = {
    sfaceRecognizer.`match`(feature1, feature2, FaceRecognizerSF.FR_COSINE)
  }

}
