package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.{be, not}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
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
      val person = testApp.service.person.add(Person())
      // no pun intended
      val savedFace: Face = testApp.service.person.addFace(face=face, asset=importedAsset, person = person)

      savedFace.image.length should be > 1000
      savedFace.alignedImage.length should be > 1000
      savedFace.alignedImageGs.length should be > 1000

      val retrievedFace: Face = testApp.service.person.getFaceById(savedFace.id.get)
      retrievedFace.features should be (savedFace.features)
      retrievedFace.embeddings should be (savedFace.embeddings)

      retrievedFace.image.length shouldBe savedFace.image.length
      retrievedFace.alignedImage.length shouldBe savedFace.alignedImage.length
      retrievedFace.alignedImageGs.length shouldBe savedFace.alignedImageGs.length
    })
  }

  test("Faces are added to a person") {
    val importAsset1 = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset1: Asset = testApp.service.assetImport.importAsset(importAsset1).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset1.data)
    val person: Person = testApp.service.person.add(Person())

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

  test("Can add and retrieve a new person with a face", Focused) {
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
    val person1: Person = testApp.service.person.add(person1Model)

    val retrievedPerson1: Person = testApp.service.person.getById(person1.persistedId)
    retrievedPerson1.isHidden should be(false)

    val person2Model = Person()
    val person2: Person = testApp.service.person.add(person2Model)
    val retrievedPerson2: Person = testApp.service.person.getById(person2.persistedId)

    retrievedPerson2.label - retrievedPerson1.label should be(1)
  }

  test("Can merge three people into one") {
    val person1Model = Person()
    val person1: Person = testApp.service.person.add(person1Model)

    val person2Model = Person()
    val person2: Person = testApp.service.person.add(person2Model)

    val mergedIntoPerson: Person = testApp.service.person.merge(dest=person1, source=person2)
    val savedMergedIntoPerson: Person = testApp.service.person.getById(person1.persistedId)

    mergedIntoPerson.mergedWithIds.size should be(1)
    mergedIntoPerson.mergedWithIds should be(savedMergedIntoPerson.mergedWithIds)

    val savedMergedPerson: Person = testApp.service.person.getById(person2.persistedId)
    savedMergedPerson.mergedIntoId should be(Some(mergedIntoPerson.persistedId))

    val person3Model = Person()
    val person3: Person = testApp.service.person.add(person3Model)

    /**
     * To completely make sure this works, merge a second person into the OG destination (person1)
     *
     * The returned person from the merge() operation should identical to the persisted version, when we
     * get that object by ID directly.
     */
    val mergedIntoPersonAgain: Person = testApp.service.person.merge(dest=mergedIntoPerson, source=person3)
    mergedIntoPersonAgain.mergedWithIds.size should be(2)

    val savedMergedIntoPersonAgain: Person = testApp.service.person.getById(mergedIntoPerson.persistedId)
    mergedIntoPersonAgain.mergedWithIds should be(savedMergedIntoPersonAgain.mergedWithIds)
  }
}
