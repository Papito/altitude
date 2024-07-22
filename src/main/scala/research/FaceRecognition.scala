package research


import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.{CvType, Mat}
import org.opencv.face.{FisherFaceRecognizer, LBPHFaceRecognizer}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File
import java.util

class Face(val id: String = BaseDao.genId,
           val image: Mat,
           val alignedImage: Mat,
           val embedding: Array[Float],
           val features: Mat) {
}

object FaceRecognition extends SandboxApp {
  private val recognizer  = FisherFaceRecognizer.create()
  // private val recognizer  = LBPHFaceRecognizer.create()

  val images = new util.ArrayList[Mat]()

  override def process(path: String): Unit = {
    val file = new File(path)

    println("\n=========================================")
    println(s"Processing ${file.getAbsolutePath}")

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = matFromBytes(fileByteArray)

    val results: List[Mat] = altitude.service.face.detectFacesWithYunet(image)

    println(s"Detected ${results.length} faces")

    val faces: List[Face] = (for (idx <- results.indices) yield {
      val res = results(idx)
      val rect = FaceService.faceDetectToRect(res)
      val faceImage = image.submat(rect)

      val alignedImage = altitude.service.face.alignCropFace(image, res)
      writeResult(file, alignedImage, idx)

      val alignedFaceBlob = altitude.service.face.getAlignedFaceBlob(alignedImage)

      val greyAlignedImage = new Mat()
      Imgproc.cvtColor(alignedImage, greyAlignedImage, Imgproc.COLOR_BGR2GRAY)
      images.add(greyAlignedImage)

      val features = altitude.service.face.getFeatures(alignedImage)
      val embedding = altitude.service.face.getEmbeddings(alignedFaceBlob)

      val face = new Face(image = faceImage, alignedImage = alignedImage, embedding = embedding, features = features)
      face
    }).toList

  }

  def writeResult(ogFile: File, image: Mat, idx: Int): Unit = {
    val indexedFileName = idx + "-" + ogFile.getName
    val outputPath = FilenameUtils.concat(outputDirPath, indexedFileName)

    if (image.empty()) {
      println("Empty image !!!")
      return
    }

    Imgcodecs.imwrite(outputPath, image)
  }


  println("Diving into " + sourceDirPath)
  val itr: Iterator[String] = recursiveFilePathIterator(sourceDirPath)

  while (itr.hasNext) {
    val path: String = itr.next()
    process(path)
  }

  private val labels = new Mat(images.size(), 1, CvType.CV_32SC1)
  for (idx <- 0 until images.size()) {
    labels.put(idx, 0, idx)
  }

  recognizer.train(images, labels)
}
