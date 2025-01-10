package software.altitude.core.pipeline.flows

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.{Altitude, RequestContext}
import software.altitude.core.models.{Face, FaceForBulkTraining, FaceForBulkTrainingWithData, Repository}
import software.altitude.core.pipeline.PipelineConstants.parallelism

import scala.concurrent.Future

/**
 * This scoops up aligned grayscale face images from the file store
 */
object AddFaceDataToModelFlow {
  def apply(app: Altitude): Flow[FaceForBulkTrainingWithData, Face, NotUsed] = {
    Flow[FaceForBulkTrainingWithData].map {
      faceForTrainingWithData =>
        val repository = Repository(
          id = Some(faceForTrainingWithData.repositoryId), // we just need the ID in context
          name = "",
          ownerAccountId = "",
          rootFolderId = "",
          fileStoreType = "")

        RequestContext.repository.value = Some(repository)

        val face = Face(
          id = Some(faceForTrainingWithData.id),
          alignedImageGs = faceForTrainingWithData.alignedImageGs,
          checksum = 0,
          detectionScore = 0,
          embeddings = Array.emptyFloatArray,
          features = Array.emptyFloatArray,
          x1 = 0,
          y1 = 0,
          width = 0,
          height = 0
        )

        app.service.faceRecognition.indexFace(face, faceForTrainingWithData.personLabel)
        face
    }
  }
}
