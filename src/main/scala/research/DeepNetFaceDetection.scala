package research

import org.apache.commons.io.FileUtils
import org.opencv.core._
import org.opencv.dnn.Dnn.blobFromImage
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import software.altitude.core.service.FaceService

import java.io.File

object DeepNetFaceDetection extends SandboxApp {
  private var totalFaceRegions = 0
  private val markerColor = new Scalar(0, 255, 255, 0)

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
          println(s"@@@ Confidence: $confidence")

          val x1 = (di.get(idx, 3)(0) * image.size().width).toInt
          val y1 = (di.get(idx, 4)(0) * image.size().height).toInt
          val x2 = (di.get(idx, 5)(0) * image.size().width).toInt
          val y2 = (di.get(idx, 6)(0) * image.size().height).toInt

          val rect = new Rect(new Point(x1, y1), new Point(x2, y2))

          println(s"@@@ Size: ${rect.width}x${rect.height}\n")

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

    // Draw rectangles around detected faces
    for (rect <- faceRegions) {
      totalFaceRegions += 1
      Imgproc.rectangle(image, rect.tl(), rect.br(), markerColor, 2)
    }

    writeResult(file, image)
  }

  allFilePaths.foreach(process)

  println(s"\n=======\nTotal face regions detected: $totalFaceRegions")
}
