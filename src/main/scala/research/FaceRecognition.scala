package research


import org.apache.commons.io.FileUtils
import org.bytedeco.javacpp.{DoublePointer, IntPointer}
import org.opencv.core.{CvType, Mat, MatOfByte, Point, Rect, Scalar, Size}
import org.opencv.dnn.Dnn.{blobFromImage, readNetFromTorch}
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.service.FaceService

import java.io.File

object FaceRecognition extends SandboxApp {
  val faceRecognizer = LBPHFaceRecognizer.create()
  private val modelFilePath = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7").getCanonicalPath
  val embedder = readNetFromTorch(modelFilePath)

  override def process(path: String): Unit = {
    val file = new File(path)

    println("\n=========================================")
    println(s"Processing ${file.getAbsolutePath}")

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = Imgcodecs.imdecode(new MatOfByte(fileByteArray: _*), Imgcodecs.IMREAD_ANYCOLOR)

    // Convert image to format suitable for using with the net
    val inputBlob = blobFromImage(
      image, FaceService.inScaleFactor, new Size(FaceService.inWidth, FaceService.inHeight), FaceService.meanVal, false, false, CvType.CV_32F)

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

          val x1 = (di.get(idx, 3)(0) * image.size().width).toInt
          val y1 = (di.get(idx, 4)(0) * image.size().height).toInt
          val x2 = (di.get(idx, 5)(0) * image.size().width).toInt
          val y2 = (di.get(idx, 6)(0) * image.size().height).toInt

          val rect = new Rect(new Point(x1, y1), new Point(x2, y2))

          if (rect.width < FaceService.minFaceSize || rect.height < FaceService.minFaceSize) {
            println("!!! Face region is too small")
            None
          } else {
            Option(rect)
          }
        } else {
          None
        }
      }
    }.flatten

    println(s"Number of face regions: ${faceRegions.size}")

    // DETECTION END
    //
    // RECOGNITION START

    for (rect <- faceRegions) {
      val faceBlob = blobFromImage(image.submat(rect), 1.0 / 255, new Size(96, 96), new Scalar(0, 0, 0), true, false)
      embedder.setInput(faceBlob)
      val embeddingsMat = embedder.forward()
      val embeddings = new Array[Float](128)
      embeddingsMat.get(0, 0, embeddings)

//      val label = new Array[Int](1)
//      val confidence = new Array[Double](0)
//
//      faceRecognizer.predict(embeddingsMat, label, confidence)
//      println(embeddings.mkString(","))
    }
  }

  allFilePaths.foreach(process)
}
