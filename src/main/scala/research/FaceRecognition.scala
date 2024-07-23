package research


import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.bytedeco.javacpp.opencv_face.FisherFaceRecognizer
import org.opencv.core.{CvType, Mat}
import org.opencv.face.LBPHFaceRecognizer
import org.opencv.imgcodecs.Imgcodecs
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes

import java.io.File
import java.util
import scala.collection.mutable
import scala.sys.exit
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
  private val minViablePersonFaceNum = 5
  val minFacesSimilarityThreshold = 2
}

class Person(val label: Int, val isUnknown: Boolean = true) {
  // Above this number, a person becomes "known" and will be in the training set

  private val faces: mutable.ListBuffer[DbFace] = mutable.ListBuffer[DbFace]()

  def isKnown: Boolean = !isUnknown

  def addFaceAndGetUpdatedPerson(face: Face): Person = {
    faces.addOne(new DbFace(face, label))

    if (faces.size == Person.minViablePersonFaceNum) {
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

  override def toString: String =
    s"PERSON $name. Label: $label"
}

object DB {
  val db: mutable.Map[Int, Person] = mutable.Map[Int, Person]()

  def allUnknownPersonFaces(): List[DbFace] = {
    db.values.filter(_.isUnknown).flatMap(_.allFaces()).toList
  }

  def allKnownPersons = {
    db.values.filter(!_.isUnknown)
  }
}

object FaceRecognition extends SandboxApp {
  private val trainedModelPath = FilenameUtils.concat(outputDirPath, "trained_model.xml")

//     public static native @Ptr LBPHFaceRecognizer create(int radius/*=1*/, int neighbors/*=8*/, int grid_x/*=8*/, int grid_y/*=8*/, double threshold/*=DBL_MAX*/);
  private val recognizer = LBPHFaceRecognizer.create()
  recognizer.setGridX(32)
  recognizer.setGridY(32)
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

  println("==>>>>>> CACHING IMAGE DATA")
  while (itr.hasNext) {
    val path: String = itr.next()
    process(path)
  }

  private val minKnownPersons = 10
  private val cosineSimilarityThreshold = .48

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

        val personMatch = DB.
          db(unknownPersonFaceMatch.get.personLabel).
          addFaceAndGetUpdatedPerson(thisFace)

        if (personMatch.isKnown) {
          // Edge case - if we keep seeing the same person, we need to discard the new instance and keep going
          // until we have 2 people required to start the training
          if (DB.allKnownPersons.size == 1) {
            if (arePeopleSimilar(DB.allKnownPersons.head, personMatch)) {
              println("Removing " + personMatch.label)
              DB.db.remove(personMatch.label)
            } else {
              // overwrite the unknown person in DB with known person
              DB.db.put(personMatch.label, personMatch)
              println(s"\n!!! Person ${personMatch.name} is now known\n")
              println(personMatch.name + " faces: \n" +  personMatch.allFaces())
            }
          } else {
            // overwrite the unknown person in DB with known person
            DB.db.put(personMatch.label, personMatch)
            println(s"\n!!! Person ${personMatch.name} is now known\n")
            println(personMatch.name + " faces: \n" +  personMatch.allFaces())
          }
        }

        // println("\nAll unknown faces: \n" + DB.allUnknownPersonFaces())
      } else {
        // No match found for this face - create a new unknown person
        println("No match found for " + thisFace.name + " -> creating new unknown person with label " + labelIdx)
        val person = new Person(label = labelIdx, isUnknown = true).
          addFaceAndGetUpdatedPerson(thisFace)

        DB.db.put(labelIdx, person)
      }

      if (DB.allKnownPersons.size == minKnownPersons) {
        println("Number of known persons reached")

        DB.allKnownPersons.foreach { person =>
          println(person.name + " faces: \n" +  person.allFaces())
          println()
          writePerson(person)
        }

        println("Training with the minimal set of known persons")

        val allFaces = DB.allKnownPersons.flatMap(_.allFaces()).toList

        val labels = new Mat(allFaces.size, 1, CvType.CV_32SC1)
        val images = new util.ArrayList[Mat]()

        for (idx <- allFaces.indices) {
          val face = allFaces(idx)
          println("Training " + face.face.name + " with index " + idx + " and label " + face.personLabel)
          labels.put(idx, 0, face.personLabel)
          images.add(face.face.alignedFaceImageGraySc)
        }
        recognizer.train(images, labels)
      }
    }
  })


  println("==>>>>>> RERUNNING WITH TRAINED DATA")
  srcFaces.forEach(thisFace => {
      println("\n--------------------\n" + thisFace.path + "\n")

      val predLabel = new Array[Int](1)
      val confidence = new Array[Double](1)
      recognizer.predict(thisFace.alignedFaceImageGraySc, predLabel, confidence)
      writeDnnHit(predLabel = predLabel(0), confidence = confidence(0), face = thisFace)
  })

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

  println("MODEL THRESHOLD: " + recognizer.getThreshold)

  private def arePeopleSimilar(person1: Person, person2: Person): Boolean = {
    // assumes obviously that both people are "known" with same number of faces

    println("COMPARING PERSON1 " + person1.name + " with PERSON2 " + person2.name)
    val facePairs = person1.allFaces().zip(person2.allFaces())

    val areSimilar = for ((face1, face2) <- facePairs) yield {
      val similarityScore = altitude.service.face.getFeatureSimilarityScore(face1.face.features, face2.face.features)
      similarityScore >= cosineSimilarityThreshold
    }

    val hits = areSimilar.count(_ == true)
    println("HITS: " + hits + " out of " + Person.minFacesSimilarityThreshold + " required")

    if (hits >= Person.minFacesSimilarityThreshold) {
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

  private def writeDnnHit(predLabel: Int, confidence: Double, face: Face): Unit = {
    println(s"Predicted label: ${predLabel} with confidence: ${confidence} for " + face.name)
    val ogFile = new File(face.path)
    val indexedFileName = "hit-" + "lbl_" + predLabel + "-conf_" + confidence.toInt + "-" + face.idx + "_" + ogFile.getName
    val outputPath = FilenameUtils.concat(outputDirPath, indexedFileName)

    println("Writing " + outputPath)
    Imgcodecs.imwrite(outputPath, face.alignedFaceImage)
  }
}
