package software.altitude.core.service

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.face.LBPHFaceRecognizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.Const
import software.altitude.core.Environment
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.util.ImageUtil.matFromBytes

import java.io.File

object FaceRecognitionService {
  // Number of labels reserved for special cases, and not used for actual people instances
  // Labels start at this number + 1 but Unknown people start at 1 (so reserved label count must be known)
  val RESERVED_LABEL_COUNT = 10

  /**
   * If there is no machine learning model verified hit, we cycle through all people in the database,
   * but only doing the matching on THIS many best face detections that we have (1 to X)
   *
   * Higher number means more matches will be found, at the cost of performance.
   *
   * Lower number means faster matching but the same person may be detected as new.
   * Technically, just 1 "top" face will work, and the accuracy benefits get diminished the higher we go
   */
  private val MAX_BRUTE_FORCE_COMPARISONS_PER_PERSON = 8

  /**
   * If the cosine distance between the face embeddings is below this threshold, we consider the face a match.
   */
  private val PESSIMISTIC_COSINE_DISTANCE_THRESHOLD = .46
}

class FaceRecognitionService(app: Altitude) {
  final val logger: Logger = LoggerFactory.getLogger(getClass)

  private val MODELS_PATH = FilenameUtils.concat(app.dataPath, Const.DataStore.MODELS)
  private val FACE_RECOGNITION_MODEL_PATH = FilenameUtils.concat(MODELS_PATH, "lbphf_face_rec_model.xml")

  private val modelFile = new File(FACE_RECOGNITION_MODEL_PATH)

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

    /**
     * Initial data is two random images, as we need the minimum of two.
     * Can't just add the first user image without an error.
     */
    val initialLabels = new Mat(2, 1, CvType.CV_32SC1)
    val InitialImages = new java.util.ArrayList[Mat]()

    for (idx <- 0 to 1) {
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

  /**
   * Returns an existing OR a new person, already persisted in the database.
   *
   * The person/faces are also added to the cache for this repository, as we may need to
   * brute-force search for the person's face in the future.
   */
  def recognizePerson(face: Face, asset: Asset): Person = {
    val predLabelArr = new Array[Int](1)
    val confidenceArr = new Array[Double](1)
    recognizer.predict(face.alignedImageGsMat, predLabelArr, confidenceArr)

    val predLabel = predLabelArr.head
    val confidence = confidenceArr.head
    println("Predicted label: " + predLabel + " with confidence: " + confidence + " for " + face.persistedId)

    // if system label, ignore, we are probably at the start, just add the face and the NEW person
    if (predLabel <= FaceRecognitionService.RESERVED_LABEL_COUNT) {
      val newPerson = app.service.person.add(Person())
      val persistedFace = app.service.person.addFace(face, asset, newPerson)
      return newPerson
    }

    null
  }

  def train(mat: Mat): Unit = {
  }
}
