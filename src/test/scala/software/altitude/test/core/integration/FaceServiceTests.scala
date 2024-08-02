package software.altitude.test.core.integration

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

  test("Can save and retrieve a face object", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.face.extractFaces(importAsset.data)

    faces.foreach(face => {
      val person = testApp.service.person.add(Person())
      // no pun intended
      val savedFace: Face = testApp.service.face.add(face=face, asset=importedAsset, person: Person)

      savedFace.image.length should be > 1000
      savedFace.aligned_image.length should be > 1000
      savedFace.aligned_image_gs.length should be > 1000

      val retrievedFace: Face = testApp.service.face.getById(savedFace.id.get)
      retrievedFace.features should be (savedFace.features)
      retrievedFace.embeddings should be (savedFace.embeddings)

      retrievedFace.image.length shouldBe savedFace.image.length
      retrievedFace.aligned_image.length shouldBe savedFace.aligned_image.length
      retrievedFace.aligned_image_gs.length shouldBe savedFace.aligned_image_gs.length
    })
  }
}
