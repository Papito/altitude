package software.altitude.test.core.controller

import org.scalatest.DoNotDiscover
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.MimedPreviewData
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.ControllerTestCore

@DoNotDiscover class ContentViewControllerTests(override val testApp: Altitude) extends ControllerTestCore {

  // FIXME: This works alone but fails within the context of the suite - what state is being shared?
  test("Access to thumbnail image denied for not logged in users") {
    // no need to persist the repository or import an asset - should never get to that point
//    get(s"/content/preview/lol") {
//      status should equal(302)
//    }
  }

  test("View preview image") {
    testContext.persistRepository() // and user
    val repoId = testContext.repository.persistedId

    val importAsset = IntegrationTestUtil.getImportAsset("images/1.jpg")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    get(s"/content/r/$repoId/${C.DataStore.PREVIEW}/${importedAsset.persistedId}", headers=testAuthHeaders()) {
      status should equal(200)
      response.getContentType() should be(s"${MimedPreviewData.MIME_TYPE};charset=utf-8")
    }
  }

  test("View a file") {
    testContext.persistRepository() // and user
    val repoId = testContext.repository.persistedId

    val importAsset = IntegrationTestUtil.getImportAsset("images/1.jpg")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    get(s"/content/r/$repoId/${C.DataStore.FILE}/${importedAsset.persistedId}", headers=testAuthHeaders()) {
      response.getContentType() should startWith("application/octet-stream")
      status should equal(200)
    }
  }

  test("View a person's cover image") {
    testContext.persistRepository()
    val repoId = testContext.repository.persistedId

    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get
    val faces1 = testApp.service.faceDetection.extractFaces(importAsset.data)
    val person: Person = testApp.service.person.add(Person())

    val face: Face = faces1.head
    val savedFace: Face = testApp.service.face.add(face=face, asset=importedAsset, person = person)

    get(s"/content/r/$repoId/${C.DataStore.FACE}/${savedFace.persistedId}", headers=testAuthHeaders()) {
      response.getContentType() should startWith("image/png")
      status should equal(200)
    }
  }
}