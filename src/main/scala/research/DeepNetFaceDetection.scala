package research

import org.opencv.core._
import org.opencv.dnn.Dnn.{blobFromImage, readNetFromCaffe}
import org.opencv.imgcodecs.Imgcodecs.{IMREAD_COLOR, imread}
import org.opencv.imgproc.Imgproc

import java.io.File

object DeepNetFaceDetection extends SandboxApp {
  private val confidenceThreshold = 0.35
  private val modelConfiguration = loadResourceAsFile("/opencv/deploy.prototxt")
  private val modelBinary = loadResourceAsFile("/opencv/res10_300x300_ssd_iter_140000.caffemodel")
  private val inWidth = 300
  private val inHeight = 300
  private val inScaleFactor = 1.0
  private val meanVal = new Scalar(104.0, 177.0, 123.0, 128)
  val markerColor = new Scalar(0, 255, 255, 0)

  if (!modelConfiguration.exists()) {
    println(s"Cannot find model configuration: ${modelConfiguration.getCanonicalPath}")
  }
  if (!modelBinary.exists()) {
    println(s"Cannot find model file: ${modelConfiguration.getCanonicalPath}")
  }

  private val net = readNetFromCaffe(modelConfiguration.getCanonicalPath, modelBinary.getCanonicalPath)

  override def process(path: String): Unit = {
    val file = new File(path)

    println("\n=========================================")
    println(s"Processing ${file.getAbsolutePath}")

    val image = imread(file.getAbsolutePath, IMREAD_COLOR)
    if (image.empty) {
      println("!!! Couldn't load image: " + file.getAbsolutePath)
    }

    // Convert image to format suitable for using with the net
    val inputBlob = blobFromImage(
      image, inScaleFactor, new Size(inWidth, inHeight), meanVal, false, false, CvType.CV_32F)

    // Set the network input
    net.setInput(inputBlob)

    // Make forward pass, compute output
    val detections = net.forward()

    println(s"Number of detections: ${detections.size()}")

    // Decode detected face locations
    val di = detections.reshape(1, detections.total().asInstanceOf[Int] / 7)
    for (idx <- 0 until di.rows()) {
      val confidence = di.get(idx, 2)(0)
      if (confidence > confidenceThreshold) {
        println(confidence)
        val x1 = (di.get(idx, 3)(0) * image.size().width).toInt
        val y1 = (di.get(idx, 4)(0) * image.size().height).toInt
        val x2 = (di.get(idx, 5)(0) * image.size().width).toInt
        val y2 = (di.get(idx, 6)(0) * image.size().height).toInt

        Imgproc.rectangle(image,
          new Point(x1, y1),
          new Point(x2, y2),
          new Scalar(0, 255, 0))

      }
    }

    writeResult(file, image)
  }
  allFilePaths.foreach(process)
}
