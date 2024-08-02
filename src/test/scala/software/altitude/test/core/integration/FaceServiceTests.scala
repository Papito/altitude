package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.{Asset, Face, Person}
import software.altitude.core.service.FaceService
import software.altitude.core.service.FaceService.matFromBytes
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Can save and retrieve a face object", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val importedAsset: Asset = testApp.service.assetImport.importAsset(importAsset).get

    val faces = testApp.service.face.extractFaces(importAsset.data)

    faces.foreach(face => {
      println("\n")
      val person = testApp.service.person.add(Person())
      // no pun intended
      val savedFace: Face = testApp.service.face.add(face=face, asset=importedAsset, person: Person)
      println(savedFace.embeddings.mkString(","))
      savedFace.image.length should be > 1000
      savedFace.aligned_image.length should be > 1000
      savedFace.aligned_image_gs.length should be > 1000

      val retrievedFace: Face = testApp.service.face.getById(savedFace.id.get)
      println()
      println(retrievedFace.embeddings.mkString(","))

      FaceService.writeDebugOpenCvMat(matFromBytes(retrievedFace.image), s"source.png")
      FaceService.writeDebugOpenCvMat(matFromBytes(retrievedFace.aligned_image), s"aligned.png")
      FaceService.writeDebugOpenCvMat(matFromBytes(retrievedFace.aligned_image_gs), s"aligned_gs.png")
    })
  }
}
