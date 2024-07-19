package research

import org.apache.commons.io.FileUtils
import org.opencv.core._
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import software.altitude.core.models.Face
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

    val faces: List[Face] = FaceService.detectFacesWithDnnNet(fileByteArray)

    for (face <- faces) {
      totalFaceRegions += 1
      val rect: Rect = new Rect(new Point(face.x1, face.y1), new Point(face.x2, face.y2))
      Imgproc.rectangle(image, rect.tl(), rect.br(), markerColor, 2)
    }

    writeResult(file, image)
  }

  allFilePaths.foreach(process)

  println(s"\n=======\nTotal face regions detected: $totalFaceRegions")
}
