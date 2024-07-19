package research


import org.apache.commons.io.FileUtils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn.blobFromImage
import org.opencv.dnn.Dnn.readNetFromTorch
import org.opencv.face.FisherFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgcodecs.Imgcodecs.imdecode
import software.altitude.core.Util.loadResourceAsFile
import software.altitude.core.service.FaceService

import java.io.File
import scala.jdk.CollectionConverters._

object FaceRecognition extends SandboxApp {
  private val recognizer  = FisherFaceRecognizer.create()
  private val modelFilePath = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7").getCanonicalPath
  private val embedder = readNetFromTorch(modelFilePath)
  override def process(path: String): Unit = {
    val file = new File(path)
    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = imdecode(new MatOfByte(fileByteArray:_*), Imgcodecs.IMREAD_ANYCOLOR)

    val faces: List[Rect] = FaceService.detectFacesWithDnnNet(image)

    for (idx <- faces.indices) {
      val rect = faces(idx)
      val faceBlob = blobFromImage(image.submat(rect), 1.0 / 255, new Size(96, 96), new Scalar(0, 0, 0), true, false)
      faceMats = faceMats :+ faceBlob
    }
  }

  private var faceMats = List[Mat]()
  allFilePaths.foreach(process)

  // now that we know how many faces we have, we can create the labels
  private val labels = new Mat(faceMats.length, 1, CvType.CV_32SC1);

  for (idx <- faceMats.indices) {
    labels.put (idx, 0, idx)
  }

  println("\nTraining recognizer...")
  recognizer.train(faceMats.asJava, labels)
}
