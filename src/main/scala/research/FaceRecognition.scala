package research


import org.apache.commons.io.FileUtils
import org.opencv.core.{CvType, Mat, MatOfByte, Point, Rect, Scalar, Size}
import org.opencv.dnn.Dnn.{blobFromImage, readNetFromTorch}
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.models.Face
import software.altitude.core.service.FaceService

// import scala.jdk.CollectionConverters
import java.io.File

object FaceRecognition extends SandboxApp {
  val recognizer = LBPHFaceRecognizer.create()
  private val modelFilePath = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7").getCanonicalPath
  private val embedder = readNetFromTorch(modelFilePath)

  override def process(path: String): Unit = {
    val file = new File(path)

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = Imgcodecs.imdecode(new MatOfByte(fileByteArray: _*), Imgcodecs.IMREAD_ANYCOLOR)

    val faces: List[Face] = FaceService.detectFaces(fileByteArray)

    for (face <- faces) {
      val rect: Rect = new Rect(new Point(face.x1, face.y1), new Point(face.x2, face.y2))
      val faceBlob = blobFromImage(image.submat(rect), 1.0 / 255, new Size(96, 96), new Scalar(0, 0, 0), true, false)
//      images.put(faceBlob.asInstanceOf[org.bytedeco.opencv.opencv_core.Mat])
//      val labels = new Mat(1, 1, CvType.CV_32SC1)("Unknown")
      //      embedder.setInput(faceBlob)
//      val embeddingsMat = embedder.forward()
//      val embeddings = new Array[Float](128)
//      embeddingsMat.get(0, 0, embeddings)
//      recognizer.update(List(embeddingsMat).asJava, 0)
      //      val label = new Array[Int](1)
      //      val confidence = new Array[Double](0)
      //
      //      faceRecognizer.predict(embeddingsMat, label, confidence)
      //      println(embeddings.mkString(","))
    }

  }

  allFilePaths.foreach(process)
}
