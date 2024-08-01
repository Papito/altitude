package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import software.altitude.core.Altitude
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Can save and retrieve a face") {
//    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
//    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get
  }
}
