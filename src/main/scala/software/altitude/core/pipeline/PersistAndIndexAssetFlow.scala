package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

import scala.concurrent.Future

import software.altitude.core.Altitude
import software.altitude.core.DuplicateException
import software.altitude.core.pipeline.PipelineConstants.parallelism
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.TDataAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.debugInfo
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

object PersistAndIndexAssetFlow {
  def apply(app: Altitude): Flow[TDataAssetOrInvalidWithContext, TDataAssetOrInvalidWithContext, NotUsed] =
    Flow[TDataAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        app.txManager.withTransaction {
          try {
            debugInfo(s"\tPersisting asset ${dataAsset.asset.persistedId}")
            app.service.asset.add(dataAsset.asset)
            debugInfo(s"\tIndexing asset ${dataAsset.asset.persistedId}")
            app.service.search.indexAsset(dataAsset.asset)
            debugInfo(s"\tUpdating stats for asset ${dataAsset.asset.persistedId}")
            app.service.stats.addAsset(dataAsset.asset)
            Future.successful((Left(dataAsset), ctx))
          } catch {
            case e: DuplicateException =>
              Future.successful(Right(Invalid(dataAsset.asset, Some(e))), ctx)
          }
        }
      case (Right(invalid), ctx) =>
        Future.successful((Right(invalid), ctx))
    }

}
