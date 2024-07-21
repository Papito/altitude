package software.altitude.test.core.integration

import org.opencv.core.Mat
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
      val faceMat = imageMat.submat(FaceService.faceDetectToRect(detection))
      FaceService.writeDebugOpenCvMat(faceMat, s"res${idx}.jpg")
    })
  }

  test("Face is detected in an image (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")


    val detections = testApp.service.face.detectFacesWithYunet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(1)
  }

  test("Faces are detected in an image (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val detections = testApp.service.face.detectFacesWithDnnNet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(2)
  }

  test("Faces are detected in an image (2)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-wednesday.png")
    val detections = testApp.service.face.detectFacesWithDnnNet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(2)
  }

  test("Small face image is detected (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/small-face1.jpg")
    val detections = testApp.service.face.detectFacesWithYunet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(1)
  }

  test("Small face image is detected (2)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/small-face2.jpg")
    val detections = testApp.service.face.detectFacesWithYunet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(1)
  }

  test("Large portrait face image is detected (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/affleck.jpg")
    val imageMat = FaceService.matFromBytes(importAsset.data)
    val detections = testApp.service.face.detectFacesWithYunet(imageMat)
    dumpDetections(imageMat, detections)
    detections.length should be(1)
  }

  test("Large portrait face image is detected (2)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/bullock.jpg")
    val detections = testApp.service.face.detectFacesWithYunet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(1)
  }

  test("Large portrait face image is detected (3)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/damon.jpg")
    val detections = testApp.service.face.detectFacesWithYunet(FaceService.matFromBytes(importAsset.data))
    detections.length should be(1)
  }

  test("Faces from the same image are identical", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val imageMat2 = imageMat1.clone()

    val detections = testApp.service.face.detectFacesWithYunet(imageMat1)
    val faceMat1 = FaceService.faceDetectToMat(imageMat1, detections.head)
    val faceMat2 = faceMat1.clone()

    val isSimilar = testApp.service.face.isFaceSimilar(imageMat1, imageMat2, faceMat1, faceMat2)
    isSimilar should be(true)
  }

  test("Same person is identified as same in multiple images (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/meme-ben.jpg")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val detections = testApp.service.face.detectFacesWithYunet(imageMat1)
    val detection1 = detections.head

    val importAsset2 = IntegrationTestUtil.getImportAsset("people/meme-ben2.png")
    val imageMat2: Mat = FaceService.matFromBytes(importAsset2.data)
    val detections2 = testApp.service.face.detectFacesWithYunet(imageMat2)
    val detection2 = detections2.head

    val importAsset3 = IntegrationTestUtil.getImportAsset("people/meme-ben3.png")
    val imageMat3: Mat = FaceService.matFromBytes(importAsset3.data)
    val detections3 = testApp.service.face.detectFacesWithYunet(imageMat3)
    val detection3 = detections3.head

    // 1 & 2
    testApp.service.face.isFaceSimilar(imageMat1, imageMat2, detection1, detection2) should be(true)
    // 1 & 3
    testApp.service.face.isFaceSimilar(imageMat1, imageMat3, detection1, detection3) should be(true)
    // 2 & 3
    testApp.service.face.isFaceSimilar(imageMat2, imageMat3, detection2, detection3) should be(true)
  }

  test("Different people are NOT identified as same in the same image (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/movies-speed.png")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val detections = testApp.service.face.detectFacesWithYunet(imageMat1)
    val detection1 :: detection2 :: Nil = detections

    testApp.service.face.isFaceSimilar(imageMat1, imageMat1, detection1, detection2) should be(false)
  }

  test("Different people are NOT identified as same in multiple images (1)", Focused) {
    val importAsset = IntegrationTestUtil.getImportAsset("people/affleck.jpg")
    val imageMat1: Mat = FaceService.matFromBytes(importAsset.data)
    val detections = testApp.service.face.detectFacesWithYunet(imageMat1)
    val detection1 = detections.head

    val importAsset2 = IntegrationTestUtil.getImportAsset("people/bullock.jpg")
    val imageMat2: Mat = FaceService.matFromBytes(importAsset2.data)
    val detections2 = testApp.service.face.detectFacesWithYunet(imageMat2)
    val detection2 = detections2.head

    val importAsset3 = IntegrationTestUtil.getImportAsset("people/damon.jpg")
    val imageMat3: Mat = FaceService.matFromBytes(importAsset3.data)
    val detections3 = testApp.service.face.detectFacesWithYunet(imageMat3)
    val detection3 = detections3.head

    // 1 & 2
    testApp.service.face.isFaceSimilar(imageMat1, imageMat2, detection1, detection2) should be(false)
    // 1 & 3
    testApp.service.face.isFaceSimilar(imageMat1, imageMat3, detection1, detection3) should be(false)
    // 2 & 3
    testApp.service.face.isFaceSimilar(imageMat2, imageMat3, detection2, detection3) should be(false)
  }
}
