package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.models.Asset
import software.altitude.core.pipeline.PipelineConstants.parallelism
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext
import software.altitude.core.pipeline.PipelineUtils.threadInfo

import scala.concurrent.Future

object ExtractMetadataFlow {
  def apply(app: Altitude): Flow[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[TAssetOrInvalidWithContext].mapAsync(parallelism) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        threadInfo(s"\tExtracting metadata for asset: ${dataAsset.asset.persistedId}")
        val userMetadata = app.service.metadata.cleanAndValidate(dataAsset.asset.userMetadata)
        val extractedMetadata = app.service.metadataExtractor.extract(dataAsset.data)
        val publicMetadata = Asset.getPublicMetadata(extractedMetadata)

        val asset: Asset = dataAsset.asset.copy(
          extractedMetadata = extractedMetadata,
          publicMetadata = publicMetadata,
          userMetadata = userMetadata
        )

        Future.successful((Left(dataAsset.copy(asset = asset)), ctx))
      case (Right(invalid), ctx) => Future.successful((Right(invalid), ctx))
    }
}
