package research


import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.{CvType, Mat}
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.AllDone
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File
import java.util
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}

class Face(val path: String,
           val idx: Int,
           val detectionScore: Float,
           val image: Mat,
           val alignedFaceImage: Mat,
           val alignedFaceImageGraySc: Mat,
           val embedding: Array[Float],
           val features: Mat) {


  def name: String = {
    val f = new File(path)
    s"${f.getParentFile.getName}/${f.getName}"
  }
}

object PersonFace {
  /**
   * Automatically sort a person's faces by detection score, best score on top,
   * so when we compare people, we compare the "best" faces first, hopefully
   * knowing one way or the other quickly.
   */
  implicit val faceOrdering: Ordering[PersonFace] = Ordering.by(-_.face.detectionScore)
}

class PersonFace(val face: Face, val personLabel: Int) {
  override def toString: String =
    s"FACE ${face.name}. Label: $personLabel. Score: ${face.detectionScore}\n"
}

object Person {
  private val minTrainablePersonFaceNum = 10
}

/**
 * Note: A "trainable" person is a person with enough faces to train a model,
 * set with minTrainablePersonFaceNum
 */
class Person(val label: Int) {
  private val faces: mutable.TreeSet[PersonFace] = mutable.TreeSet[PersonFace]()

  def addFace(face: Face): PersonFace = {
    val pFace = new PersonFace(face, label)
    faces.addOne(pFace)
    println("Adding face " + face.name + " to person " + label + ". Number of faces: " + faces.size)
    pFace
  }

  def name: String = {
    faces.size match {
      case 0 => "Unknown"
      case _ => faces.head.face.name
    }
  }

  def allFaces(): List[PersonFace] = {
    faces.toList
  }

  def merge(person: Person): Unit = {
    println("Merging " + this.name + " with " + person.name)
    this.faces.addAll(person.allFaces())
  }

  override def toString: String =
    s"PERSON $name. Label: $label. Faces: ${faces.size}"
}


object DB {
  var LAST_LABEL = 1

  val db: mutable.Map[Int, Person] = mutable.Map[Int, Person]()

  def allPersons(): List[Person] = {
    db.values.toList
  }

  def allFaces(): List[PersonFace] = {
    db.values.flatMap(_.allFaces()).toList
  }

}

object FaceRecognition extends SandboxApp {
  private val trainedModelPath = FilenameUtils.concat(outputDirPath, "trained_model.xml")

  private val recognizer = LBPHFaceRecognizer.create()
  recognizer.setGridX(12)
  recognizer.setGridY(12)
  recognizer.setRadius(2)
  private val srcFaces = new util.ArrayList[Face]()

  override def process(path: String): Unit = {
    val file = new File(path)

    val fileByteArray: Array[Byte] = FileUtils.readFileToByteArray(file)
    val image: Mat = matFromBytes(fileByteArray)

    val results: List[Mat] = altitude.service.face.detectFacesWithYunet(image)

    results.indices.foreach { idx =>
      val res = results(idx)
      val rect = FaceService.faceDetectToRect(res)

      val alignedFaceImage = altitude.service.face.alignCropFaceFromDetection(image, res)

      // LBPHFaceRecognizer requires grayscale images
      val alignedFaceImageGr = altitude.service.face.getHistEqualizedGrayScImage(alignedFaceImage)
      // writeResult(file, alignedFaceImage, idx)

      val features = altitude.service.face.getFacialFeatures(alignedFaceImage)
      val embedding = altitude.service.face.getEmbeddings(alignedFaceImage)

      val faceImage = image.submat(rect)
      val face = new Face(
        path = path,
        idx = idx,
        detectionScore = res.get(0, 14)(0).asInstanceOf[Float],
        image = faceImage.clone(),
        alignedFaceImage = alignedFaceImage.clone(),
        alignedFaceImageGraySc = alignedFaceImageGr.clone(),
        embedding = embedding.clone(),
        features = features.clone())

      srcFaces.add(face)

      faceImage.release()
      alignedFaceImage.release()
      features.release()
    }
  }

  println("==>>>>>> TRAINING WITH INITIAL DATA")
  private val initialLabels = new Mat(2, 1, CvType.CV_32SC1)
  private val InitialImages = new util.ArrayList[Mat]()

  for (idx <- 0 to 1) {
    val file = new File(s"src/main/resources/train/1.jpg")
    val image: Mat = matFromBytes(FileUtils.readFileToByteArray(file))
    val results: List[Mat] = altitude.service.face.detectFacesWithYunet(image)
    val res = results.head
    val alignedFaceImage = altitude.service.face.alignCropFaceFromDetection(image, res)
    val alignedFaceImageGr = altitude.service.face.getHistEqualizedGrayScImage(alignedFaceImage)
    initialLabels.put(idx, 0, idx)
    InitialImages.add(alignedFaceImageGr)
  }
  recognizer.train(InitialImages, initialLabels)

  private val itr: Iterator[String] = recursiveFilePathIterator(sourceDirPath)

  private var fileCount = 0
  private var comparisonOpCount = 0
  private var modelHitCount = 0

  println("==>>>>>> CACHING IMAGE DATA")
  while (itr.hasNext) {
    val path: String = itr.next()
    fileCount += 1
    process(path)
  }

  private val cosineSimilarityThreshold = .46

  srcFaces.forEach(thisFace => {
    breakable {

      println("\n--------------------\n" + thisFace.path + "\n")

      // try to predict the label of the face
      val predLabelArr = new Array[Int](1)
      val confidenceArr = new Array[Double](1)
      recognizer.predict(thisFace.alignedFaceImageGraySc, predLabelArr, confidenceArr)

      val predLabel = predLabelArr.head
      val confidence = confidenceArr.head
      println("Predicted label: " + predLabel + " with confidence: " + confidence + " for " + thisFace.name)

      // if system label, ignore, we are probably at the start, just add the face and the NEW person
      if (predLabel < 2) {
        addNewPerson(thisFace)
        break()
      }

      // get the match from DB and do a similarity check
      val personMatch: Person = DB.db(predLabel)
      println(s"Running similarity check for this face and the Recognizer match -> ${personMatch.name}")

      comparisonOpCount += 1
      val simScore = altitude.service.face.getFeatureSimilarityScore(
        thisFace.features, personMatch.allFaces().head.face.features)
      println("Similarity score: " + simScore)

      if (simScore >= cosineSimilarityThreshold) {
        println("MATCHED")
        // Add the face and update the model
        val personFace = personMatch.addFace(thisFace)
        updateModelWithFace(personFace)
        println("Faces in DB: " + DB.allFaces().size)
        break()
      }

      // No match, brute force through all people (matching with only the top-ranking face)
      val bestPersonFaceMatch: Option[PersonFace] = getPersonFaceMatches(thisFace)

      // this is most likely a new person
      if (bestPersonFaceMatch.isEmpty) {
        addNewPerson(thisFace)
      } else {
        // WE GOT THEM
        modelHitCount += 1
        val matchedPerson = DB.db(bestPersonFaceMatch.get.personLabel)
        val newPersonFace = matchedPerson.addFace(thisFace)
        updateModelWithFace(newPersonFace)
        println("Faces in DB: " + DB.allFaces().size)
      }

      //      writeFrHit(predLabel = predLabel(0), confidence = confidence(0), face = thisFace)
    }
  })

    DB.allPersons().foreach { person =>
      println(person.name + " faces: \n" +  person.allFaces())
      println()
      writePerson(person)
    }

  private def addNewPerson(face: Face): Unit = {
    println("Adding new person with face " + face.name)
    val label = DB.LAST_LABEL + 1
    val person = new Person(label = label)
    val personFace: PersonFace = person.addFace(face)
    DB.db.put(label, person)
    updateModelWithFace(personFace)
    DB.LAST_LABEL = label
    println("Faces in DB: " + DB.allFaces().size)
  }

  private def updateModelWithFace(personFace: PersonFace): Unit = {
    println("Updating model for person " + personFace.personLabel + " with " + personFace.face.name)
    val labels = new Mat(1, 1, CvType.CV_32SC1)
    val images = new util.ArrayList[Mat]()
    labels.put(0, 0, personFace.personLabel)
    images.add(personFace.face.alignedFaceImageGraySc)
    recognizer.update(images, labels)
  }

  private def getPersonFaceMatches(thisFace: Face): Option[PersonFace] = {
    val faceSimilarityScores: List[(Double, PersonFace)] = DB.allPersons().map { person =>
      val bestFace = person.allFaces().head
      println(s"Comparing ${thisFace.name} Q:${thisFace.detectionScore} with ${bestFace.face.name} Q:${bestFace.face.detectionScore}")
      comparisonOpCount += 1
      val similarityScore = altitude.service.face.getFeatureSimilarityScore(
        thisFace.features, bestFace.face.features)
      println(" -> " + similarityScore)

      (similarityScore, bestFace)
    }

    // get the top one
    val sortedMatchingSimilarityWeights = faceSimilarityScores.sortBy(_._1)
//    println("WEIGHTS")
//    println(sortedMatchingSimilarityWeights.mkString("\n"))
    val highestSimilarityWeights = sortedMatchingSimilarityWeights.filter(_._1 >= cosineSimilarityThreshold)

    highestSimilarityWeights.headOption match {
      case None => None
      case Some(res) => Some(res._2)
    }
  }

  println("PROCESSED FILES: " + fileCount)
  println("COMPARISON OPERATIONS: " + comparisonOpCount)
  println("MODEL HITS: " + modelHitCount)

  def writeResult(ogFile: File, image: Mat, idx: Int): Unit = {
    val indexedFileName = idx + "-" + ogFile.getName
    val outputPath = FilenameUtils.concat(outputDirPath, indexedFileName)

    if (image.empty()) {
      println("Empty image !!!")
      return
    }

    Imgcodecs.imwrite(outputPath, image)
  }

  private def writePerson(person: Person): Unit = {
    val personPrefix = "person-" + person.label

    person.allFaces().indices.foreach { idx =>
      val dbFace = person.allFaces()(idx)
      val ogFile = new File(dbFace.face.path)
      val indexedFileName = personPrefix + "-" + idx + 1 + "." + FilenameUtils.getExtension(ogFile.getName)
      val outputPath = FilenameUtils.concat(outputDirPath, indexedFileName)
      println("Writing " + outputPath)
      Imgcodecs.imwrite(outputPath, dbFace.face.alignedFaceImage)
    }
  }

  private def writeFrHit(predLabel: Int, confidence: Double, face: Face): Unit = {
    println(s"Predicted label: ${predLabel} with confidence: ${confidence} for " + face.name)
    val ogFile = new File(face.path)
    val indexedFileName = "hit-" + "lbl_" + predLabel + "-conf_" + confidence.toInt + "-" + face.idx + "_" + ogFile.getName
    val outputPath = FilenameUtils.concat(outputDirPath, indexedFileName)

    println("Writing " + outputPath)
    Imgcodecs.imwrite(outputPath, face.alignedFaceImage)
  }
}
