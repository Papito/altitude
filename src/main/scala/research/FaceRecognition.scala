package research


import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.opencv.core.{CvType, Mat}
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File
import java.util
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}

class Face(val path: String,
           val idx: Int,
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

class DbFace(val face: Face, val personLabel: Int) {
  override def toString: String =
    s"FACE ${face.name}. Label: $personLabel\n"
}

object Person {
  private val minViablePersonFaceNum = 4
  /**
   * Number of faces required to be similar to consider a person as known
   * THIS DEPENDS ON minViablePersonFaceNum, and since we are doing expensive quadratic
   * minViablePersonFaceNum*2 comparisons between unknown person faces, this number should be
   * around half that.
   * 4 faces -> 16 comparisons -> 8 similar faces
   * 5 faces -> 25 comparisons -> 12-ish similar faces
   */
  val minFacesSimilarityThreshold = 8
}

class Person(val label: Int, val isUnknown: Boolean = true) {
  private val faces: mutable.ListBuffer[DbFace] = mutable.ListBuffer[DbFace]()

  def isKnown: Boolean = !isUnknown

  def addFaceAndGetUpdatedPerson(face: Face): Person = {
    faces.addOne(new DbFace(face, label))
    println("Adding face " + face.name + " to person " + label + ". Number of faces: " + faces.size)

    if (isUnknown && faces.size >= Person.minViablePersonFaceNum) {
      val knownPerson = new Person(this.label, isUnknown = false)
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

  def allFaces(): List[DbFace] = {
    faces.toList
  }

  def merge(person: Person): Unit = {
    println("Merging " + this.name + " with " + person.name)
    this.faces.addAll(person.allFaces())
  }

  override def toString: String =
    s"PERSON $name. Label: $label. Faces: ${faces.size}. Known: $isKnown"
}


object DB {
  val db: mutable.Map[Int, Person] = mutable.Map[Int, Person]()

  def allUnknownPersonFaces(): List[DbFace] = {
    db.values.filter(_.isUnknown).flatMap(_.allFaces()).toList
  }

  def allKnownPersons: Iterable[Person] = {
    db.values.filter(!_.isUnknown)
  }
}

object FaceRecognition extends SandboxApp {
  private val trainedModelPath = FilenameUtils.concat(outputDirPath, "trained_model.xml")

  //     public static native @Ptr LBPHFaceRecognizer create(int radius/*=1*/, int neighbors/*=8*/, int grid_x/*=8*/, int grid_y/*=8*/, double threshold/*=DBL_MAX*/);
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
  private val cosineSimilarityThreshold = .43

  private var labelIdx = -1

  println("==>>>>>> INITIAL TRAINING RUN WITH MINIMAL DATA")
  srcFaces.forEach(thisFace => {
    breakable {
      if (DB.allKnownPersons.size == minKnownPersons) {
        break()
      }
      println("\n--------------------\n" + thisFace.path + "\n")
      labelIdx += 1

      // No data in DB
      if (DB.db.isEmpty) {
        val person = new Person(label = 0, isUnknown = true).addFaceAndGetUpdatedPerson(thisFace)
        DB.db.put(0, person)
        println("First person added as UNKNOWN")
        break()
      }

      // Below the number of minimum trainable persons
      val unknownPersonFaceMatch: Option[DbFace] = getUnknownPersonFaceMatch(thisFace)

      // found a match for this face for an existing unknown person
      if (unknownPersonFaceMatch.isDefined) {
        println("Found match for " + thisFace.name + " -> " + unknownPersonFaceMatch.get.face.name)

        val personMatch = DB.db(unknownPersonFaceMatch.get.personLabel).addFaceAndGetUpdatedPerson(thisFace)

        if (personMatch.isKnown) {
          // we have a complete person with enough faces to train, but are there similar people in DB?
          var similarPerson: Option[Person] = None

          DB.allKnownPersons.foreach { person =>
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
            println("Known persons: " + DB.allKnownPersons.size)
          }

        } else {
          // update the unknown person with the new face
          DB.db.put(personMatch.label, personMatch)
        }

      } else {
        // No match found for this face - create a new unknown person
        println("No match found for " + thisFace.name + " -> creating new unknown person with label " + labelIdx)
        val person = new Person(label = labelIdx, isUnknown = true).
          addFaceAndGetUpdatedPerson(thisFace)

        DB.db.put(labelIdx, person)
      }
    }
  })

  DB.allKnownPersons.foreach { person =>
    println(person.name + " faces: \n" +  person.allFaces())
    println()
    writePerson(person)
  }

  println("Training with the minimal set of known persons")

  private val allFaces = DB.allKnownPersons.flatMap(_.allFaces()).toList

  private val labels = new Mat(allFaces.size, 1, CvType.CV_32SC1)
  private val images = new util.ArrayList[Mat]()

  for (idx <- allFaces.indices) {
    val face = allFaces(idx)
    println("Training " + face.face.name + " with index " + idx + " and label " + face.personLabel)
    labels.put(idx, 0, face.personLabel)
    images.add(face.face.alignedFaceImageGraySc)
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

  private def getUnknownPersonFaceMatch(thisFace: Face): Option[DbFace] = {
    val unknownFaceSimilarityWeights: List[(Double, DbFace)] = DB.allUnknownPersonFaces().map { unknownPersonFace =>
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

    val cosDistancesTmp: List[List[Double]] = person1.allFaces().map { face =>
      person2.allFaces().map { face2 =>
        comparisonOpCount += 1
        altitude.service.face.getFeatureSimilarityScore(face.face.features, face2.face.features)
      }
    }
    val cosDistances = cosDistancesTmp.flatten

    val similarityHits = cosDistances.filter { similarityScore =>
      similarityScore >= cosineSimilarityThreshold
    }

    println("!!! COSINE DISTANCES: " + cosDistances)
    println("!!! HITS: " + similarityHits.length + " out of " + cosDistances.length)

    if (similarityHits.length >= Person.minFacesSimilarityThreshold) {
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
