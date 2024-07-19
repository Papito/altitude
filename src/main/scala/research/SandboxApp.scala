package research

import org.apache.commons.io.FilenameUtils
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs

import java.io.File

abstract class SandboxApp extends App {
  Loader.load(classOf[opencv_java])

  private val sourceDirPath =  System.getenv().get("SOURCE")
  private val outputDirPath = System.getenv().get("OUTPUT")

  def process(path: String): Unit

  if (!new File(sourceDirPath).isDirectory) {
    println(s"Source directory [$sourceDirPath] does not exist")
  }

  if (!new File(outputDirPath).isDirectory) {
    println(s"Output directory [$outputDirPath] does not exist")
  }

  def getAllFilesInDirectory(directoryPath: String): List[String] = {
    val directory = new File(directoryPath)
    directory.listFiles.filter(_.isFile)
      .filter(file =>
        file.getName.toLowerCase.endsWith(".jpg") ||
          file.getName.toLowerCase.endsWith(".jpeg") ||
          file.getName.toLowerCase.endsWith(".png"))
      .map(_.getAbsolutePath).toList
  }

  def writeResult(ogFile: File, image: Mat): Unit = {
    val outputPath = FilenameUtils.concat(outputDirPath, ogFile.getName)
    println(String.format("Writing %s", outputPath))
    Imgcodecs.imwrite(outputPath, image)
  }

  val allFilePaths: List[String] = getAllFilesInDirectory(sourceDirPath)
}
