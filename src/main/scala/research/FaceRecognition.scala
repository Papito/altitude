package research


import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File

class Face(val id: String = BaseDao.genId,
           val image: Mat,
           val alignedImage: Mat,
           val embedding: Array[Float],
           val features: Mat) {
}

object FaceRecognition extends SandboxApp {

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

      val features = altitude.service.face.getFeatures(alignedImage)
      val embedding = altitude.service.face.getEmbeddings(features)
      println(s"Embedding size: ${embedding.mkString(", ")}")

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
}
