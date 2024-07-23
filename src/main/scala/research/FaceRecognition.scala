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
  private val matchDirPath: String =  System.getenv().get("MATCH")

  if (!new File(matchDirPath).isDirectory) {
    println(s"Match directory [$matchDirPath] does not exist")
  }

  private val recognizer  = FisherFaceRecognizer.create()
  // private val recognizer  = LBPHFaceRecognizer.create()

  val trainedModelPath = FilenameUtils.concat(outputDirPath, "trained_model.xml")

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

      val alignedFaceImage = altitude.service.face.alignCropFaceFromDetection(image, res)
      // val trainingImage = altitude.service.face.getTrainingFaceImage(alignedFaceImage)
      images.add(alignedFaceImage)

      writeResult(file, alignedFaceImage, idx)

      val features = altitude.service.face.getFacialFeatures(alignedFaceImage)
      val embedding = altitude.service.face.getEmbeddings(alignedFaceImage)

      val faceImage = image.submat(rect)
      val face = new Face(image = faceImage, alignedImage = alignedFaceImage, embedding = embedding, features = features)
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

// COMMENT OUT TO TRAIN
//=====================
  println("Diving into " + sourceDirPath)
  private val itr: Iterator[String] = recursiveFilePathIterator(sourceDirPath)

  while (itr.hasNext) {
    val path: String = itr.next()
    process(path)
  }

  private val labels = new Mat(images.size(), 1, CvType.CV_32SC1)
  for (idx <- 0 until images.size()) {
    labels.put(idx, 0, 0)
  }

//  recognizer.train(images, labels)
//  recognizer.save(trainedModelPath)

// COMMENT OUT TO RECOGNIZE
//=========================
//  recognizer.read(trainedModelPath)
//
//  private val itr2: Iterator[String] = recursiveFilePathIterator(matchDirPath)
//
//  while (itr2.hasNext) {
//    val path: String = itr2.next()
//
//    val file = new File(path)
//    println("Matching " + file.getAbsolutePath)
//
//    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
//    val image: Mat = matFromBytes(fileByteArray)
//
//    val results: List[Mat] = altitude.service.face.detectFacesWithYunet(image)
//
//    results.foreach({res: Mat => {
//      val alignedImage = altitude.service.face.alignCropFace(image, res)
//      val greyAlignedImage = new Mat()
//      Imgproc.cvtColor(alignedImage, greyAlignedImage, Imgproc.COLOR_BGR2GRAY)
//      Imgproc.equalizeHist(greyAlignedImage, greyAlignedImage)
//
//      val predLabel = new Array[Int](1)
//      val confidence = new Array[Double](1)
//      writeResult(file, greyAlignedImage, 0)
//      recognizer.predict(greyAlignedImage, predLabel, confidence)
//      println(s"Predicted label: ${predLabel(0)} with confidence: ${confidence(0)}")
//    }})
//  }
}
