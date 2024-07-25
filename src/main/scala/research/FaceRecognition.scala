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
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.math.pow
import scala.sys.exit
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
class Person(val label: Int, val isTrainable: Boolean = false) {
  private val faces: mutable.TreeSet[PersonFace] = mutable.TreeSet[PersonFace]()

  def addFaceAndGetUpdatedPerson(face: Face): Person = {
    faces.addOne(new PersonFace(face, label))
    println("Adding face " + face.name + " to person " + label + ". Number of faces: " + faces.size)

    checkIfTrainableAndReturnNewPersonObj()
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

  def merge(person: Person): Person = {
    println("Merging " + this.name + " with " + person.name)
    this.faces.addAll(person.allFaces())

    checkIfTrainableAndReturnNewPersonObj()
  }

  private def checkIfTrainableAndReturnNewPersonObj(): Person = {
    if (!isTrainable && faces.size >= Person.minTrainablePersonFaceNum) {
      val trainablePerson = new Person(this.label, isTrainable = true)
      trainablePerson.faces.addAll(this.faces)
      return trainablePerson
    }

    // change nothing
    this
  }

  override def toString: String =
    s"PERSON $name. Label: $label. Faces: ${faces.size}. Trainable: $isTrainable"
}


object DB {
  val db: mutable.Map[Int, Person] = mutable.Map[Int, Person]()

  def allUntrainedPersonFaces(): List[PersonFace] = {
    db.values.filter(!_.isTrainable).flatMap(_.allFaces()).toList
  }

  def allTrainedPersons: Iterable[Person] = {
    db.values.filter(_.isTrainable)
  }

  def allUntrainedPersons: Iterable[Person] = {
    db.values.filter(!_.isTrainable)
  }

  def all(): Iterable[Person] = {
    db.values
  }
}

object FaceRecognition extends SandboxApp {
  private val trainedModelPath = FilenameUtils.concat(outputDirPath, "trained_model.xml")

  private val recognizer = LBPHFaceRecognizer.create()
  recognizer.setGridX(12)
  recognizer.setGridY(12)
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

  for (idx <- 1 to 2) {
    val file = new File("src/main/resources/train/1.jpg")
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

  println("==>>>>>> CACHING IMAGE DATA")
  while (itr.hasNext) {
    val path: String = itr.next()
    fileCount += 1
    process(path)
  }

  private val cosineSimilarityThreshold = .5

  private var labelIdx = 3

  println("==>>>>>> INITIAL TRAINING RUN WITH MINIMAL DATA")
  srcFaces.forEach(thisFace => {

    breakable {
      println("\n--------------------\n" + thisFace.path + "\n")
      labelIdx += 1

      val untrainedPersonFaceMatch: Option[PersonFace] = getUntrainedPersonFaceMatch(thisFace)

      // found a match for this face for an existing person
      if (untrainedPersonFaceMatch.isDefined) {
        println("Found match for " + thisFace.name + " -> " + untrainedPersonFaceMatch.get.face.name)
        val personMatch = DB.db(untrainedPersonFaceMatch.get.personLabel).addFaceAndGetUpdatedPerson(thisFace)
        DB.db.put(personMatch.label, personMatch)
      } else {
        val person = new Person(label = labelIdx, isTrainable = false).addFaceAndGetUpdatedPerson(thisFace)
        DB.db.put(labelIdx, person)
      }
    }
  })

  DB.all().foreach { person =>
    println(person.name + " faces: \n" +  person.allFaces())
    println()
    writePerson(person)
  }

  println("Training with the minimal set of known persons")

  private val allFaces = DB.allTrainedPersons.flatMap(_.allFaces()).toList

  private val labels = new Mat(allFaces.size, 1, CvType.CV_32SC1)
  private val images = new util.ArrayList[Mat]()

  for (idx <- allFaces.indices) {
    val personFace = allFaces(idx)
    println("Training " + personFace.face.name + " with index " + idx + " and label " + personFace.personLabel)
    labels.put(idx, 0, personFace.personLabel)
    images.add(personFace.face.alignedFaceImageGraySc)
  }
  recognizer.train(images, labels)

//  println("==>>>>>> RERUNNING WITH TRAINED DATA")
//  srcFaces.forEach(thisFace => {
//      println("\n--------------------\n" + thisFace.path + "\n")
//
//      val predLabel = new Array[Int](1)
//      val confidence = new Array[Double](1)
//      recognizer.predict(thisFace.alignedFaceImageGraySc, predLabel, confidence)
//      writeFrHit(predLabel = predLabel(0), confidence = confidence(0), face = thisFace)
//  })

  private def getUntrainedPersonFaceMatch(thisFace: Face): Option[PersonFace] = {
    val unknownFaceSimilarityWeights: List[(Double, PersonFace)] = DB.allUntrainedPersonFaces().map { unknownPersonFace =>
      comparisonOpCount += 1
      val similarityScore = altitude.service.face.getFeatureSimilarityScore(
        thisFace.features, unknownPersonFace.face.features)

      (similarityScore, unknownPersonFace)
    }

    val sortedMatchingSimilarityWeights = unknownFaceSimilarityWeights.sortBy(_._1)
    val highestSimilarityWeights = sortedMatchingSimilarityWeights.filter(_._1 >= cosineSimilarityThreshold)

    highestSimilarityWeights.headOption match {
      case None => None
      case Some(res) => Some(res._2)
    }
  }

  println("PROCESSED FILES: " + fileCount)
  println("COMPARISON OPERATIONS: " + comparisonOpCount)

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
      val isCompleteStr = if (person.isTrainable) "compl" else "incompl"
      val indexedFileName = isCompleteStr + "-" + personPrefix + "-" + idx + 1 + "." + FilenameUtils.getExtension(ogFile.getName)
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
