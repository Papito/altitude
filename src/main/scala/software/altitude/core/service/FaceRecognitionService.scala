package software.altitude.core.service

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.opencv.core.{CvType, Mat}
import org.opencv.face.LBPHFaceRecognizer
import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.service.FaceDetectionService.matFromBytes
import software.altitude.core.{Altitude, Const, Environment}

import java.io.File

class FaceRecognitionService(app: Altitude) {
  final val logger: Logger = LoggerFactory.getLogger(getClass)

  private val MODELS_PATH = FilenameUtils.concat(app.dataPath, Const.DataStore.MODELS)
  private val FACE_RECOGNITION_MODEL_PATH = FilenameUtils.concat(MODELS_PATH, "lbphf_face_rec_model.xml")

  private val modelFile = new File(FACE_RECOGNITION_MODEL_PATH)
  private val pessimisticCosineSimilarityThreshold = .46
  private val maxFaceComparesPerPerson = 8

  val recognizer: LBPHFaceRecognizer = LBPHFaceRecognizer.create()
  recognizer.setGridX(10)
  recognizer.setGridY(10)
  recognizer.setRadius(2)

  private def initialize(): Unit = {
    if (modelFile.exists()) {
      logger.info("Found facial recognition model, loading...")
      recognizer.read(FACE_RECOGNITION_MODEL_PATH)
      return
    }

  logger.warn("No facial recognition model found, training with initial data...")

    val initialLabels = new Mat(2, 1, CvType.CV_32SC1)
    val InitialImages = new java.util.ArrayList[Mat]()

    for (idx <- 1 to 2) {
      val bytes = getClass.getResourceAsStream(s"/train/${idx}.png").readAllBytes()
      val image: Mat = matFromBytes(bytes)
      initialLabels.put(idx, 0, idx)
      InitialImages.add(image)
    }

    recognizer.train(InitialImages, initialLabels)
    saveModel()
  }

  initialize()

  private def saveModel(): Unit = {
    /**
     * Unorthodox, but this whole file-writing thing is messy and we can't really mock this method
     * as it can be invoked during deep app init, before we can use Mockito.
     *
     * In test, the model is ephemeral.
     */
    if (Environment.CURRENT == Environment.Name.TEST) {
      return
    }

    logger.info("Saving initial model")
    FileUtils.forceMkdirParent(modelFile)
    recognizer.save(FACE_RECOGNITION_MODEL_PATH)
  }

  def train(mat: Mat): Unit = {
  }
}
