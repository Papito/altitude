package research


import org.apache.commons.io.FileUtils
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.global.opencv_dnn.blobFromImage
import org.bytedeco.opencv.global.opencv_dnn.readNetFromTorch
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_ANYCOLOR
import org.bytedeco.opencv.global.opencv_imgcodecs.imdecode
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer
import org.opencv.core.CvType
import software.altitude.core.models.Face
import software.altitude.core.service.FaceService

import java.io.File
import java.nio.IntBuffer

object FaceRecognition extends SandboxApp {
  val recognizer: LBPHFaceRecognizer = LBPHFaceRecognizer.create()
  private val modelFilePath = loadResourceAsFile("/opencv/openface_nn4.small2.v1.t7").getCanonicalPath
  private val embedder = readNetFromTorch(modelFilePath)

  override def process(path: String): Unit = {
    val file = new File(path)

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = imdecode(new Mat(new BytePointer(fileByteArray:_*)), IMREAD_ANYCOLOR)

    val faces: List[Face] = FaceService.detectFaces(fileByteArray)

    for (face <- faces) {
      val rect: Rect = new Rect(new Point(face.x1, face.y1), new Point(face.x2, face.y2))
      val faceBlob = blobFromImage(new Mat(image, rect), 1.0 / 255, new Size(96, 96), new Scalar(0, 0), true, false, CvType.CV_32F)
      val images = new MatVector(1)
      images.put(faceBlob)
      val labels = new Mat(1, 1, CvType.CV_32SC1)
      val labelsBuf: IntBuffer = labels.createBuffer()
      labelsBuf.put(0, 0)
//      recognizer.train(images, labels)
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
