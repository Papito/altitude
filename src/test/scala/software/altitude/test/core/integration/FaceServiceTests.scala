package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Faces are recognized in an image (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.face.detectFaces(importAsset.data)

    faces.length should be(1)
  }

  test("Faces are recognized in an image (2)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/speed.png")
    testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.face.detectFaces(importAsset.data)

    faces.length should be(2)
  }

  test("Faces are recognized in an image (3)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-wednesday.png")
    testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.face.detectFaces(importAsset.data)

    faces.length should be(2)
  }
}
