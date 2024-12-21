package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext
import software.altitude.core.pipeline.PipelineUtils.threadInfo

object FacialRecognitionFlow {
  def apply(app: Altitude): Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].map {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tRunning facial recognition ${dataAsset.asset.persistedId}")
        app.service.faceRecognition.processAsset(dataAsset)
        (Left(dataAsset), ctx)
      case (Right(invalid), ctx) => (Right(invalid), ctx)
    }
}
