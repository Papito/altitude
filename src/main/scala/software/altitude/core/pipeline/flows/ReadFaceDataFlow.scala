package software.altitude.core.pipeline.flows

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.models.{Face, FaceForBulkTraining, FaceForBulkTrainingWithData, Repository}
import software.altitude.core.pipeline.PipelineConstants.parallelism

import scala.concurrent.Future

/**
 * This scoops up aligned grayscale face images from the file store, and then indexes them into the face recognition system.
 */
object ReadFaceDataFlow {
  def apply(app: Altitude): Flow[FaceForBulkTraining, FaceForBulkTrainingWithData, NotUsed] = {
    Flow[FaceForBulkTraining].map {
      faceForTraining =>
        // hacky, and we could add the repo to the context via a new composite class,
        // but probably not worth the boilerplate and complexity in this case -
        // we already have the ID in the object here
        val repository = Repository(
          id = Some(faceForTraining.repositoryId), // we just need the ID in context
          name = "",
          ownerAccountId = "",
          rootFolderId = "",
          fileStoreType = "")

        RequestContext.repository.value = Some(repository)

        val alignedImageGs = app.service.fileStore.getAlignedGreyscaleFaceById(faceForTraining.id)

        val faceForTrainingWithData = FaceForBulkTrainingWithData(
          id = faceForTraining.id,
          personLabel = faceForTraining.personLabel,
          repositoryId = faceForTraining.repositoryId,
          alignedImageGs = alignedImageGs.data)

        faceForTrainingWithData
    }
  }
}
