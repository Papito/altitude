package research


import org.apache.commons.io.FileUtils
import org.opencv.core.Mat
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File

object FaceRecognition extends SandboxApp {

  override def process(path: String): Unit = {
    val file = new File(path)

    println("\n=========================================")
    println(s"Processing ${file.getAbsolutePath}")

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = matFromBytes(fileByteArray)
  }

  println("Diving into " + sourceDirPath)
  val itr: Iterator[String] = recursiveFilePathIterator(sourceDirPath)

  println(itr.hasNext)
  while (itr.hasNext) {
    val path: String = itr.next()
    println(s"Processing $path")
  }
}
