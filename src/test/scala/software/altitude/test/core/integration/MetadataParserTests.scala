package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.AssetType
import software.altitude.core.models.UserMetadata
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class MetadataParserTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Detect asset type JPEG") {
    val importAsset =IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val assetType: AssetType = testApp.service.assetImport.detectAssetType(importAsset)
    assetType.mediaType should be("image")
    assetType.mediaSubtype should be("jpeg")
    assetType.mime should be("image/jpeg")
  }

  test("Detect asset type PNG") {
    val importAsset =IntegrationTestUtil.getImportAsset("images/3.PNG")
    val assetType: AssetType = testApp.service.assetImport.detectAssetType(importAsset)
    assetType.mediaType should be("image")
    assetType.mediaSubtype should be("png")
    assetType.mime should be("image/png")
  }

  test("Extract metadata", Focused) {
    val importAsset =IntegrationTestUtil.getImportAsset("images/cactus.jpg")
    val metadata: UserMetadata = testApp.service.metadataExtractor.extract(importAsset.data)
    println(metadata)
  }
}
