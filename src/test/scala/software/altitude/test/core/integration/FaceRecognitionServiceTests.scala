package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceRecognitionServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Recognize one new person", Focused) {
    val importAsset1 = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset1: Asset = testApp.service.assetImport.importAsset(importAsset1).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset1.data)
    val face1: Face = faces1.head



//    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
//    val importedAsset2: Asset = testApp.service.assetImport.importAsset(importAsset2).get
//
//    val faces2 = testApp.service.faceDetection.extractFaces(importAsset2.data)
//    val face2 = faces2.head
  }

  test("Recognize two new people") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.faceDetection.extractFaces(importAsset.data)

    faces.foreach(face => {
    })
  }

}
