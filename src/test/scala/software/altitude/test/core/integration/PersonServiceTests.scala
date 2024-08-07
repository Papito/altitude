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
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class PersonServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

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

  test("Person merge A -> B", Focused) {
    val destPerson: Person = testApp.service.person.addPerson(Person())
    testContext.addMockFaces(destPerson, 3)

    val sourcePerson: Person = testApp.service.person.addPerson(Person())
    testContext.addMockFaces(sourcePerson, 3)

    val destPersonMerged: Person = testApp.service.person.merge(dest=destPerson, source=sourcePerson)
    destPersonMerged.mergedWithIds.size should be(1)

    val destPersonFaces: List[Face] = testApp.service.person.getFaces(destPersonMerged.persistedId)
    destPersonFaces.size should be(6)

    val destPersonInDb: Person = testApp.service.person.getById(destPerson.persistedId)
    destPersonInDb.mergedWithIds should be(destPersonMerged.mergedWithIds)

    val sourcePersonInDb: Person = testApp.service.person.getById(sourcePerson.persistedId)
    sourcePersonInDb.mergedIntoId should be(Some(destPersonMerged.persistedId))
    sourcePersonInDb.mergedIntoLabel should be(Some(destPersonMerged.label))

    val sourcePersonFaces: List[Face] = testApp.service.person.getFaces(sourcePersonInDb.persistedId)
    sourcePersonFaces shouldBe empty

    // the person in cache under source label should be the dest person
    val destPersonInCache = testApp.service.faceCache.getPersonByLabel(sourcePerson.label).get
    destPersonInCache should be(destPersonMerged)
    destPersonInCache should be theSameInstanceAs destPersonMerged
  }

  test("Person merge B -> A, C -> A") {
  }
}
