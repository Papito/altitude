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
  private val dnnModelFile: File = loadResourceAsFile("/opencv/res10_300x300_ssd_iter_140000.caffemodel")
  private val sfaceModelFile = loadResourceAsFile("/opencv/face_recognition_sface_2021dec.onnx")
  private val yunetModelFile = loadResourceAsFile("/opencv/face_detection_yunet_2022mar.onnx")
//  private val embedderModelFile = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7")

  private val dnnConfidenceThreshold = 0.37
  private val minFaceSize = 50 // minimum acceptable size of face region in pixels
  private val dnnInWidth = 300
  private val dnnInHeight = 300
  private val dnnInScaleFactor = 1.0
  private val dnnMeanVal = new Scalar(104.0, 177.0, 123.0, 128)
  private val yunetConfidenceThreshold = 0.80f

  private val dnnNet: Net = readNetFromCaffe(dnnConfigurationFile.getCanonicalPath, dnnModelFile.getCanonicalPath)

  //  private val embedderNet: Net = readNetFromTorch(embedderModelFile.getCanonicalPath)

  private val sfaceRecognizer = FaceRecognizerSF.create(sfaceModelFile.getCanonicalPath, "")

  private val yuNet = FaceDetectorYN.create(yunetModelFile.getCanonicalPath, "", new Size())
  yuNet.setScoreThreshold(yunetConfidenceThreshold)

  def detectFacesWithDnnNet(image: Mat): List[Rect] = {
    if (image.empty) {
      logger.warn("No data in image")
      return List()
    }

    val inputBlob = blobFromImage(
      image,
      FaceService.dnnInScaleFactor,
      new Size(FaceService.dnnInWidth, FaceService.dnnInHeight),
      FaceService.dnnMeanVal, false, false, CvType.CV_32F)

    // Set the network input
    FaceService.dnnNet.setInput(inputBlob)

    // Make forward pass, compute output
    val detections = FaceService.dnnNet.forward()

    // Decode detected face locations
    val di = detections.reshape(1, detections.total().asInstanceOf[Int] / 7)

    val faceRegions = {
      for (idx <- 0 until di.rows()) yield {
        val confidence = di.get(idx, 2)(0)

        if (confidence > FaceService.dnnConfidenceThreshold) {
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

    def determineImageScale(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int) = {
      val scaleX = targetWidth.toDouble / sourceWidth
      val scaleY = targetHeight.toDouble / sourceHeight
      Math.min(scaleX, scaleY)
    }

    val boundingBoxSize = 650

    val scaleFactor = determineImageScale(image.width(), image.height(), boundingBoxSize, boundingBoxSize) match {
        case scale if scale < 1.0 => scale
        case _ => 1.0
    }

    val srcMat: Mat = if (scaleFactor < 1.0) {
      val resized = new Mat()
      Imgproc.resize(image, resized, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_AREA)
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
      val detectionRect = faceDetectToRect(detection)

      detectionRect.x = (detectionRect.x / scaleFactor).toInt
      detectionRect.y = (detectionRect.y / scaleFactor).toInt
      detectionRect.width = (detectionRect.width / scaleFactor).toInt
      detectionRect.height = (detectionRect.height / scaleFactor).toInt

      // update the original detection to account for the scaling factor
      detection.put(0, 0, detectionRect.x)
      detection.put(0, 1, detectionRect.y)
      detection.put(0, 2, detectionRect.width)
      detection.put(0, 3, detectionRect.height)

      if (detectionRect.height < minFaceSize || detectionRect.width < minFaceSize) {
        logger.warn("Face region too small")
        None
      } else {
        Option(detection)
      }
    }).toList

    ret.flatten
  }

  //  def multiPassFaceDetect(image: Mat): List[Mat] = {
  //    println("Detecting faces with Yunet")
  //    val yunetResults = detectFacesWithYunet(image)
  //
  //    println(s"Detected ${yunetResults.length} faces with Yunet")
  //
  //    if (yunetResults.isEmpty) {
  //      return List.empty
  //    }
  //
  //
  //    val verifiedMatches: List[Option[Mat]] = (for (idx <- yunetResults.indices) yield {
  //      println(s"Verifying face $idx")
  //      val ynetResult = yunetResults(idx)
  //
  //      val faceRect = FaceService.faceDetectToRect(ynetResult)
  //      println(faceRect)
  //      val faceMat = image.submat(faceRect)
  //      val verifiedFaceMatches = detectFacesWithDnnNet(faceMat)
  //
  //      var idx2 = 0
  //      verifiedFaceMatches.foreach(fa => {
  //        println(fa)
  //        val faceMat2 = faceMat.submat(fa)
  //        // Imgcodecs.imwrite(s"/home/andrei/output/face$idx.$idx2.jpg", faceMat2)
  //        idx2 += 1
  //      })
  //
  //      None
  //    }).toList
  //
  //    verifiedMatches.flatten
  //  }

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

  def isFaceSimilar(image1: Mat, image2: Mat, faceMat1: Mat, faceMat2: Mat): Boolean = {
    val alignedFace1 = new Mat
    val alignedFace2 = new Mat
    sfaceRecognizer.alignCrop(image1, faceMat1, alignedFace1)
    sfaceRecognizer.alignCrop(image2, faceMat2, alignedFace2)
    Imgcodecs.imwrite("/home/andrei/output/face1-1.jpg", alignedFace1)
    Imgcodecs.imwrite("/home/andrei/output/face2-1.jpg", alignedFace2)

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
    println("COS: " + cosScore)
    println("L2: " + L2Score)
    println()
    cosScore >= 0.363
  }

  def faceDetectionMatToFace(detectedFace: Mat): Face = {
    val asRect = faceDetectToRect(detectedFace)
    Face(x1 = asRect.x, y1 = asRect.y, x2 = asRect.width, y2 = asRect.height)
  }

  def faceDetectToRect(detectedFace: Mat): Rect = {
    val x = detectedFace.get(0, 0)(0).asInstanceOf[Int]
    val y = detectedFace.get(0, 1)(0).asInstanceOf[Int]
    val w = detectedFace.get(0, 2)(0).asInstanceOf[Int]
    val h = detectedFace.get(0, 3)(0).asInstanceOf[Int]
    new Rect(x, y, w, h)
  }

  def faceToRect(face: Face): Rect = {
    new Rect(new Point(face.x1, face.y1), new Point(face.x2, face.y2))
  }
  //
  //  def faceToMat(face: Face): Mat = {
  //    // Create a Mat with 1 row and 4 columns
  //    val mat = new Mat(1, 4, CvType.CV_32F)
  //
  //    mat.put(0, 0, face.x1.toDouble)
  //    mat.put(0, 1, face.y1.toDouble)
  //    mat.put(0, 2, face.x2.toDouble - face.x1.toDouble)
  //    mat.put(0, 3, face.y2.toDouble - face.y1.toDouble)
  //
  //    mat
  //  }

  def matFromBytes(data: Array[Byte]): Mat = {
    Imgcodecs.imdecode(new MatOfByte(data: _*), Imgcodecs.IMREAD_GRAYSCALE)
  }

}

class FaceService(val app: Altitude) extends BaseService[Face] {
  Loader.load(classOf[opencv_java])

  override protected val dao: FaceDao = app.DAO.face

}
