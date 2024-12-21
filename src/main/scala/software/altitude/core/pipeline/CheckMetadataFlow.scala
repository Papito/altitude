package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.UnsupportedMediaTypeException
import software.altitude.core.models.AssetWithData
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.PipelineContext
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext
import software.altitude.core.pipeline.PipelineTypes.TAssetWithContext
import software.altitude.core.pipeline.PipelineUtils.threadInfo

object CheckMetadataFlow {
  def apply(app: Altitude): Flow[TAssetWithContext, TAssetOrInvalidWithContext, NotUsed] =
    Flow[(AssetWithData, PipelineContext)].map {
      case (dataAsset, ctx) =>
        threadInfo(s"Checking media type: ${dataAsset.asset.fileName}: ${dataAsset.asset.assetType.toJson}")

        try {
          app.service.library.checkMediaType(dataAsset.asset)
          (Left(dataAsset), ctx)
        } catch {
          case e: UnsupportedMediaTypeException =>
            (Right(Invalid(dataAsset, Some(e))), ctx)
        }
    }
}
