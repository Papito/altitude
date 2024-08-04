package software.altitude.test.core.integration

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Can save and retrieve a face object") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.faceDetection.extractFaces(importAsset.data)

    faces.foreach(face => {
      val person = testApp.service.person.add(Person())
      // no pun intended
      val savedFace: Face = testApp.service.face.add(face=face, asset=importedAsset, person = person)

      savedFace.image.length should be > 1000
      savedFace.alignedImage.length should be > 1000
      savedFace.alignedImageGs.length should be > 1000

      val retrievedFace: Face = testApp.service.face.getById(savedFace.id.get)
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
    val savedFace1: Face = testApp.service.face.add(face=face1, asset=importedAsset1, person = person)

    val updatedPerson: Person = testApp.service.person.getById(person.persistedId)
    updatedPerson.numOfFaces should be (1)
    updatedPerson.coverFaceId should be (Some(savedFace1.persistedId))

    // Add another face to the same person
    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
    val importedAsset2: Asset = testApp.service.assetImport.importAsset(importAsset2).get

    val faces2 = testApp.service.faceDetection.extractFaces(importAsset2.data)
    val face2 = faces2.head
    testApp.service.face.add(face=face2, asset=importedAsset2, person = updatedPerson)

    val updatedPersonAgain: Person = testApp.service.person.getById(person.persistedId)
    updatedPersonAgain.numOfFaces should be (2)
    // cover face should not have changed
    updatedPersonAgain.coverFaceId should be (Some(savedFace1.persistedId))
  }
}
