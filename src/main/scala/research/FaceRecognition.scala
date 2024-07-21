package research


import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File

object FaceRecognition extends SandboxApp {

  override def process(path: String): Unit = {
    val file = new File(path)

    println("\n=========================================")
    println(s"Processing ${file.getAbsolutePath}")

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = matFromBytes(fileByteArray)

    val results: List[Mat] = FaceService.detectFacesWithYunet(image)

    println(s"Detected ${results.length} faces")

    for (idx <- results.indices) {
      val res = results(idx)
      val rect = FaceService.faceDetectToRect(res)
      val faceImage = image.submat(rect)
      writeResult(file, faceImage, idx)
    }
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
