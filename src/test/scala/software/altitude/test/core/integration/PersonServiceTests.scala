package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.must.Matchers.empty
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, theSameInstanceAs}
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.service.FaceRecognitionService
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class PersonServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  // default face count for a person
  val NUM_OF_FACES = 12

  test("Can save and retrieve a face object") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.faceDetection.extractFaces(importAsset.data)

    faces.foreach(face => {
      val person = testApp.service.person.addPerson(Person())
      // no pun intended
      val savedFace: Face = testApp.service.person.addFace(face=face, asset=importedAsset, person = person)

      savedFace.image.length should be > 1000
      savedFace.alignedImage.length should be > 1000
      savedFace.alignedImageGs.length should be > 1000
      savedFace.personId should be (Some(person.persistedId))
      savedFace.personLabel should be (Some(person.label))

      val retrievedFace: Face = testApp.service.person.getFaceById(savedFace.id.get)
      retrievedFace.features should be (savedFace.features)
      retrievedFace.embeddings should be (savedFace.embeddings)

      retrievedFace.image.length shouldBe savedFace.image.length
      retrievedFace.alignedImage.length shouldBe savedFace.alignedImage.length
      retrievedFace.alignedImageGs.length shouldBe savedFace.alignedImageGs.length
      retrievedFace.personId shouldBe savedFace.personId
      retrievedFace.personLabel shouldBe savedFace.personLabel
    })
  }

  test("Faces are added to a person") {
    val importAsset1 = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset1: Asset = testApp.service.assetImport.importAsset(importAsset1).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset1.data)
    val person: Person = testApp.service.person.addPerson(Person())

    person.numOfFaces should be (0)

    val face1: Face = faces1.head
    val savedFace1: Face = testApp.service.person.addFace(face=face1, asset=importedAsset1, person = person)

    val updatedPerson: Person = testApp.service.person.getById(person.persistedId)
    updatedPerson.numOfFaces should be (1)
    updatedPerson.coverFaceId should be (Some(savedFace1.persistedId))

    // Add another face to the same person
    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
    val importedAsset2: Asset = testApp.service.assetImport.importAsset(importAsset2).get

    val faces2 = testApp.service.faceDetection.extractFaces(importAsset2.data)
    val face2 = faces2.head
    testApp.service.person.addFace(face=face2, asset=importedAsset2, person = updatedPerson)

    val updatedPersonAgain: Person = testApp.service.person.getById(person.persistedId)
    updatedPersonAgain.numOfFaces should be (2)
    // cover face should not have changed
    updatedPersonAgain.coverFaceId should be (Some(savedFace1.persistedId))
  }

  test("Can add and retrieve a new person with a face") {
    val importAsset1 = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset1: Asset = testApp.service.assetImport.importAsset(importAsset1).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset1.data)
    val face1: Face = faces1.head

    val personModel = Person()
    personModel.addFace(face1)
    val person: Person = testApp.service.person.addPerson(personModel, Some(importedAsset1))
    person.numOfFaces should be(1)
    person.getFaces.size should be(1)
    person.coverFaceId should not be None
    testApp.service.person.getFaceById(person.coverFaceId.get)
  }

  test("Can add and retrieve a person") {
    val person1Model = Person()
    val person1: Person = testApp.service.person.addPerson(person1Model)

    val retrievedPerson1: Person = testApp.service.person.getById(person1.persistedId)
    retrievedPerson1.isHidden should be(false)

    val person2Model = Person()
    val person2: Person = testApp.service.person.addPerson(person2Model)
    val retrievedPerson2: Person = testApp.service.person.getById(person2.persistedId)

    retrievedPerson2.label - retrievedPerson1.label should be(1)
  }

  test("Person merge B -> A") {
    val personA: Person = testApp.service.person.addPerson(Person())
    testContext.addTestFaces(personA, NUM_OF_FACES)

    //
    // *** Person in cache should have only the required top faces, sorted by detection score
    //
    var cachedA = testApp.service.faceCache.getPersonByLabel(personA.label).get

    cachedA.getFaces.size should be(FaceRecognitionService.MAX_COMPARISONS_PER_PERSON)

    // save the destination scores to compare with later
    val destOriginalFaceScores = cachedA.getFaces.map(_.detectionScore)

    cachedA.getFaces.sliding(2).forall(faces =>
      faces.head.detectionScore >= faces.last.detectionScore) should be(true)

    val personB: Person = testApp.service.person.addPerson(Person())
    var cachedB = testApp.service.faceCache.getPersonByLabel(personB.label).get

    val mergedA: Person = testApp.service.person.merge(dest=personA, source=personB)

    //
    // *** Merged destination should have the merged with ID (one)
    //
    mergedA.mergedWithIds.size should be(1)

    //
    // *** Merged cached person should have top faces, sorted by detection score, and not identical by score to where we started
    //
    cachedA = testApp.service.faceCache.getPersonByLabel(personA.label).get
    cachedA.getFaces.size should be(FaceRecognitionService.MAX_COMPARISONS_PER_PERSON)

    cachedA.getFaces.sliding(2).forall(faces =>
      faces.head.detectionScore >= faces.last.detectionScore) should be(true)

    cachedA.getFaces.map(_.detectionScore) should not be destOriginalFaceScores

    //
    // *** Cached source person should now return the merged person, if retrieved by label from cache
    //
    cachedB = testApp.service.faceCache.getPersonByLabel(personB.label).get
    cachedB should be theSameInstanceAs cachedA

    //
    // *** Sanity checks for persisted instances of source and destination2
    //
    val persistedA: Person = testApp.service.person.getById(personA.persistedId)
    persistedA.mergedWithIds should be(mergedA.mergedWithIds)

    val persistedB: Person = testApp.service.person.getById(personB.persistedId)
    persistedB.mergedIntoId should be(Some(mergedA.persistedId))
    persistedB.mergedIntoLabel should be(Some(mergedA.label))

    val bFacesInDb: List[Face] = testApp.service.person.getFaces(persistedB.persistedId)
    bFacesInDb shouldBe empty
  }

  test("Person merge C -> B, B -> A", Focused) {
    val personC: Person = testApp.service.person.addPerson(Person(name=Some("C")))
    testContext.addTestFaces(personC, NUM_OF_FACES)

    val personB: Person = testApp.service.person.addPerson(Person(name=Some("B")))
    testContext.addTestFaces(personB, NUM_OF_FACES)

    val personA: Person = testApp.service.person.addPerson(Person(name=Some("A")))
    // no faces, to keep it simple

    //
    // *** C -> B
    //
    val mergedB: Person = testApp.service.person.merge(dest=personB, source=personC)

    var bFacesInDb: List[Face] = testApp.service.person.getFaces(mergedB.persistedId)
    bFacesInDb.size should be(NUM_OF_FACES * 2)

    var cFacesInDb: List[Face] = testApp.service.person.getFaces(personC.persistedId)
    cFacesInDb.size should be(0)

    var cachedC = testApp.service.faceCache.getPersonByLabel(personC.label).get
    var cachedB = testApp.service.faceCache.getPersonByLabel(personB.label).get

    cachedB should be theSameInstanceAs cachedC

    val persistedA: Person = testApp.service.person.getById(personA.persistedId)
    val persistedB: Person = testApp.service.person.getById(personB.persistedId)

    //
    // *** B -> A
    //
    val mergedA: Person = testApp.service.person.merge(dest=persistedA, source=persistedB)

    val aFacesInDb: List[Face] = testApp.service.person.getFaces(mergedA.persistedId)
    aFacesInDb.size should be(NUM_OF_FACES * 2)

    bFacesInDb = testApp.service.person.getFaces(personB.persistedId)
    bFacesInDb.size should be(0)

    cFacesInDb = testApp.service.person.getFaces(personC.persistedId)
    cFacesInDb.size should be(0)
  }

  def isSortedDescending(seq: Seq[Double]): Boolean = {
    seq.sliding(2).forall {
      case Seq(x, y) => x >= y
      case _ => true
    }
  }
}
