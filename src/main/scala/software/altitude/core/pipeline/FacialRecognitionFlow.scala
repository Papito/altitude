package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

import scala.concurrent.Future

import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.debugInfo
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

object FacialRecognitionFlow {
  def apply(app: Altitude): Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(1) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        debugInfo(s"\tRunning facial recognition ${dataAsset.asset.persistedId}")
        app.service.faceRecognition.processAsset(dataAsset)
        Future.successful(Left(dataAsset), ctx)
      case (Right(invalid), ctx) => Future.successful(Right(invalid), ctx)
    }
}
