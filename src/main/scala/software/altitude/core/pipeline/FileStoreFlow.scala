package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

import scala.concurrent.Future

import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineConstants.parallelism
import software.altitude.core.pipeline.PipelineTypes.TDataAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.debugInfo
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

object FileStoreFlow {
  def apply(app: Altitude): Flow[TDataAssetOrInvalidWithContext, TDataAssetOrInvalidWithContext, NotUsed] =
    Flow[TDataAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        debugInfo(s"\tStoring asset ${dataAsset.asset.persistedId}")
        app.service.fileStore.addAsset(dataAsset)
        Future.successful((Left(dataAsset), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }
}
