package software.altitude.core.pipeline.flows

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.debugInfo
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

import scala.concurrent.Future
import scala.concurrent.duration._

object SaveFaceRecModelFlow {
  def apply(app: Altitude): Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] = {
    Flow[TAssetOrInvalidWithContext]
      .groupedWithin(100, 15.minutes)
      .mapAsync(1) {
        batch =>
          val (_, contexts) = batch.unzip
          val ctx = contexts.head // all contexts here should be from same repo substream
          setThreadLocalRequestContext(ctx)

          debugInfo(s"\tSaving facial recognition model for repo: ${ctx.repository.name}")
          app.service.faceRecognition.saveModel()
          Future.successful(batch)
      }
      .mapConcat(identity)
  }
}