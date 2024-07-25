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
import scala.math.pow
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
  /**
   * Number of faces required to be similar to consider a person as known
   * THIS DEPENDS ON minViablePersonFaceNum, and since we are doing expensive quadratic
   * minViablePersonFaceNum * 2 comparisons between unknown person faces (only theoretical worst case),
   * this number should be a fraction of that. So, if it's..
   *   5 faces -> 25 comparisons -> 5 similar faces minimum
   *   10 faces -> 100 comparisons -> 25 similar faces minimum
   */
  val minFacesSimilarityThreshold: Int = pow(minTrainablePersonFaceNum, 2).intValue / 4
  println("minFacesSimilarityThreshold: " + minFacesSimilarityThreshold)

  /**
   * Face number threshold to short-circuit similarity comparisons between persons' faces.
   * If we keep getting negatives this many times in a row, we can stop early to save on computation.
   */
  val similarityShortCircuitThreshold: Int = (minFacesSimilarityThreshold / 2) + 1
    println("similarityShortCircuitThreshold: " + similarityShortCircuitThreshold)
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

    // if not trainable and we have enough faces, make it trainable and return
    if (!isTrainable && faces.size >= Person.minTrainablePersonFaceNum) {
      val knownPerson = new Person(this.label, isTrainable = true)
      knownPerson.faces.addAll(this.faces)
      return knownPerson
    }

    this
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
    s"PERSON $name. Label: $label. Faces: ${faces.size}. Trainable: $isTrainable"
}


object DB {
  val db: mutable.Map[Int, Person] = mutable.Map[Int, Person]()

  def allUntrainablePersonFaces(): List[PersonFace] = {
    db.values.filter(!_.isTrainable).flatMap(_.allFaces()).toList
  }

  def allTrainablePersons: Iterable[Person] = {
    db.values.filter(_.isTrainable)
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

  private val itr: Iterator[String] = recursiveFilePathIterator(sourceDirPath)

  private var fileCount = 0
  private var comparisonOpCount = 0

  println("==>>>>>> CACHING IMAGE DATA")
  while (itr.hasNext) {
    val path: String = itr.next()
    fileCount += 1
    process(path)
  }

  private val minKnownPersons = 6
  private val cosineSimilarityThreshold = .47

  private var labelIdx = -1

  println("==>>>>>> INITIAL TRAINING RUN WITH MINIMAL DATA")
  srcFaces.forEach(thisFace => {

    breakable {
      if (DB.allTrainablePersons.size == minKnownPersons) {
        break()
      }
      println("\n--------------------\n" + thisFace.path + "\n")
      labelIdx += 1

      // No data in DB
      if (DB.db.isEmpty) {
        val person = new Person(label = 0, isTrainable = false).addFaceAndGetUpdatedPerson(thisFace)
        DB.db.put(0, person)
        println("First person added as UNKNOWN")
        break()
      }

      // Below the number of minimum trainable persons
      val unknownPersonFaceMatch: Option[PersonFace] = getUnknownPersonFaceMatch(thisFace)

      // found a match for this face for an existing unknown person
      if (unknownPersonFaceMatch.isDefined) {
        println("Found match for " + thisFace.name + " -> " + unknownPersonFaceMatch.get.face.name)

        val personMatch = DB.db(unknownPersonFaceMatch.get.personLabel).addFaceAndGetUpdatedPerson(thisFace)

        if (personMatch.isTrainable) {
          // we have a complete person with enough faces to train, but are there similar people in DB?
          var similarPerson: Option[Person] = None

          DB.allTrainablePersons.foreach { person =>
            if (similarPerson.isEmpty && arePeopleSimilar(person, personMatch)) {
              similarPerson = Some(person)
            }
          }

          if (similarPerson.isDefined) {
            println(s"Found ONE similar person in DB: ${similarPerson.get.name} -> MERGING")
            similarPerson.get.merge(personMatch)
            DB.db.remove(personMatch.label)
            println("Merged person: " + similarPerson)
            DB.db.put(similarPerson.get.label, similarPerson.get)
          } else {
            println("No other known persons are similar to " + personMatch.name)
            // update the unknown person as known
            DB.db.put(personMatch.label, personMatch)
            println("Known persons: " + DB.allTrainablePersons.size)
          }

        } else {
          // update the unknown person with the new face
          DB.db.put(personMatch.label, personMatch)
        }

      } else {
        // No match found for this face - create a new unknown person
        println("No match found for " + thisFace.name + " -> creating new unknown person with label " + labelIdx)
        val person = new Person(label = labelIdx, isTrainable = false).
          addFaceAndGetUpdatedPerson(thisFace)

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

  private val allFaces = DB.allTrainablePersons.flatMap(_.allFaces()).toList

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

  private def getUnknownPersonFaceMatch(thisFace: Face): Option[PersonFace] = {
    val unknownFaceSimilarityWeights: List[(Double, PersonFace)] = DB.allUntrainablePersonFaces().map { unknownPersonFace =>
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

  private def arePeopleSimilar(person1: Person, person2: Person): Boolean = {
    println("Comparing PERSON1 " + person1.name + " with PERSON2 " + person2.name)

    val histAccumulator = ListBuffer.empty[Boolean]
    var comparisonHits = 0
    try {
      person1.allFaces().map { personFace =>
        person2.allFaces().map { face2 =>
          val score = altitude.service.face.getFeatureSimilarityScore(personFace.face.features, face2.face.features)
          comparisonOpCount += 1 // for keeping global count

          val isAMatch = score >= cosineSimilarityThreshold
          comparisonHits +=  (if (isAMatch) 1 else 0)
          histAccumulator.addOne(isAMatch)

          // short-circuit if we have enough comparisons and no hits
          if (histAccumulator.size == Person.similarityShortCircuitThreshold && comparisonHits == 0) {
            throw AllDone(success = false)
          }

          // short-circuit if we have enough hits
          if (comparisonHits == Person.minFacesSimilarityThreshold) {
            throw AllDone(success = true)
          }

          isAMatch
        }
      }
    } catch {
        case AllDone(true) => println("Short-circuiting similarity comparisons with TRUE")
        case AllDone(false) => println("Short-circuiting similarity comparisons with FALSE")
    }

    val similarityHits = histAccumulator.count(_ == true)

    println("!!! HITS: " + similarityHits + " out of " + histAccumulator.size)

    if (similarityHits >= Person.minFacesSimilarityThreshold) {
      println("People are similar")
      true
    } else {
      println("People are not similar")
      false
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
