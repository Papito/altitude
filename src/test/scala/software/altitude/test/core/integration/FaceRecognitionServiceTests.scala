package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.must.Matchers.empty
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceRecognitionServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Recognize a person twice") {
    val importAsset1 = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset1: Asset = testApp.service.assetImport.importAsset(importAsset1).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset1.data)
    val face1: Face = faces1.head
    val recognizedPerson: Person = testApp.service.faceRecognition.recognizeFace(face1, importedAsset1)

    // the person should be in the cache
    var cachedPerson = testApp.service.faceCache.getPersonByLabel(recognizedPerson.label)
    cachedPerson should not be empty
    cachedPerson.get.getFaces.size should be(1)

    // person is trained on one face (model has one label reference)
    val people = testApp.service.person.getPeople(importedAsset1.persistedId)
    val person = people.head
    getLabels.count(_ == person.label) should be(1)

    // Recognize again
    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
    val importedAsset2: Asset = testApp.service.assetImport.importAsset(importAsset2).get
    val faces2 = testApp.service.faceDetection.extractFaces(importAsset2.data)
    val face2: Face = faces2.head

    val samePerson: Person = testApp.service.faceRecognition.recognizeFace(face2, importedAsset2)
    samePerson.persistedId shouldBe recognizedPerson.persistedId
    cachedPerson.get.getFaces.size should be(2)

    // second face was used to train the same person
    getLabels.count(_ == person.label) should be(2)

    // Recognize a second time
    val importAsset3 = IntegrationTestUtil.getImportAsset("people/meme-ben3.png")
    val importedAsset3: Asset = testApp.service.assetImport.importAsset(importAsset3).get
    val faces3 = testApp.service.faceDetection.extractFaces(importAsset3.data)
    val face3: Face = faces3.head

    val samePersonAgain: Person = testApp.service.faceRecognition.recognizeFace(face3, importedAsset3)
    samePersonAgain.persistedId shouldBe recognizedPerson.persistedId

    cachedPerson = testApp.service.faceCache.getPersonByLabel(recognizedPerson.label)
    cachedPerson.get.getFaces.size should be(3)

    val persistedPerson = testApp.service.person.getPersonById(recognizedPerson.persistedId)
    persistedPerson.numOfFaces should be(3)

    // third face was used to train the same person
    getLabels.count(_ == person.label) should be(3)
  }

  test("Recognize two new people") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val people = testApp.service.person.getPeople(importedAsset.persistedId)
    people.size should be(2)

    // model has two label references, one for each person
    getLabels.count(_ == people.head.label) should be(1)
    getLabels.count(_ == people.last.label) should be(1)

    // There should be two new people in the cache
    testApp.service.faceCache.size() should be(2)

  }

}
