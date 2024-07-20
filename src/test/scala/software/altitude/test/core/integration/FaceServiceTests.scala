package software.altitude.test.core.integration

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.service.FaceService
import software.altitude.test.IntegrationTestUtil
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class FaceServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  def dumpDetections(imageMat: Mat, detections: List[Mat]): Unit = {
    detections.indices foreach(idx => {
      val detection = detections(idx)
      val faceMat1 = imageMat.submat(FaceService.faceDetectToRect(detection))
      Imgcodecs.imwrite(s"/home/andrei/output/res${idx}.jpg", faceMat1)
    })
  }

  test("Face is detected in an image (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    testApp.service.assetImport.importAsset(importAsset).get

    val imageMat = FaceService.matFromBytes(importAsset.data)
    val detections = FaceService.detectFacesWithYunet(imageMat)
    println("Found face: " + detections.length)
    dumpDetections(imageMat, detections)
//    detections.length should be(1)
  }

  test("Faces are detected in an image (2)") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    testApp.service.assetImport.importAsset(importAsset).get

    val faces = FaceService.detectFacesWithDnnNet(FaceService.matFromBytes(importAsset.data))

    faces.length should be(2)
  }

  test("Faces are detected in an image (3)") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-wednesday.png")
    testApp.service.assetImport.importAsset(importAsset).get

    val faces = FaceService.detectFacesWithDnnNet(FaceService.matFromBytes(importAsset.data))

    faces.length should be(2)
  }

  test("Faces from the same image are identical") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val faces = FaceService.detectFacesWithDnnNet(imageMat1)
    val faceMat1 = imageMat1.submat(faces.head)
    val imageMat2 = imageMat1.clone()
    val faceMat2 = faceMat1.clone()

    val isSimilar = FaceService.isFaceSimilar(imageMat1, imageMat2, faceMat1, faceMat2)
    isSimilar should be(true)
  }

  test("Same person is identified as same in multiple images") {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val faces = FaceService.detectFacesWithDnnNet(imageMat1)
    val faceMat1 = imageMat1.submat(faces.head)

    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
    val imageMat2: Mat = FaceService.matFromBytes(importAsset2.data)
    val faces2 = FaceService.detectFacesWithDnnNet(imageMat2)
    val faceMat2 = imageMat2.submat(faces2.head)

    val importAsset3 = IntegrationTestUtil.getImportAsset("people/meme-ben3.png")
    val imageMat3: Mat = FaceService.matFromBytes(importAsset3.data)
    val faces3 = FaceService.detectFacesWithDnnNet(imageMat3)
    val faceMat3 = imageMat3.submat(faces3.head)

    // 1 & 2 are the same
    FaceService.isFaceSimilar(imageMat1, imageMat2, faceMat1, faceMat2) should be(true)
    // 1 & 3 are the same
    FaceService.isFaceSimilar(imageMat1, imageMat3, faceMat1, faceMat3) should be(true)
    // 2 & 3 are the same
    FaceService.isFaceSimilar(imageMat2, imageMat3, faceMat2, faceMat3) should be(true)
  }

  test("Different people are NOT identified as same in multiple images") {
//    val importAsset = IntegrationTestUtil.getImportAsset("people/affleck.jpg")
//    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
//    val faces = FaceService.detectFacesWithYunet(importAsset.data)
//    val faceMat1 = imageMat1.submat(faces.head.y1, faces.head.y2, faces.head.x1, faces.head.x2)

    val importAsset2 = IntegrationTestUtil.getImportAsset("people/bullock.jpg")
    val imageMat2: Mat = FaceService.matFromBytes(importAsset2.data)
    val faces2 = FaceService.detectFacesWithDnnNet(imageMat2)
    val faceMat2 = imageMat2.submat(faces2.head)

    Imgcodecs.imwrite("/home/andrei/output/face2.jpg", faceMat2)

//    val isSimilar = FaceService.isFaceSimilar(faceMat1, faceMat2, face1AreaMat, face2AreaMat)
//    isSimilar should be(false)
  }
}
