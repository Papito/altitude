package software.altitude.core.pipeline.flows

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

import scala.concurrent.Future
import software.altitude.core.{Altitude, DuplicateException, SamePersonDetectedTwiceException}
import software.altitude.core.pipeline.PipelineTypes.{Invalid, TDataAssetOrInvalidWithContext}
import software.altitude.core.pipeline.PipelineUtils.debugInfo
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

object FacialRecognitionFlow {
  def apply(app: Altitude): Flow[TDataAssetOrInvalidWithContext, TDataAssetOrInvalidWithContext, NotUsed] =
    Flow[TDataAssetOrInvalidWithContext].mapAsync(1) {
      case (Left(dataAsset), ctx) =>
        setThreadLocalRequestContext(ctx)

        debugInfo(s"\tRunning facial recognition ${dataAsset.asset.fileName}")
        try {
          app.service.faceRecognition.processAsset(dataAsset)
        } catch {
          case e: DuplicateException =>
            Future.successful(
              Right(Invalid(dataAsset.asset,
                Some(SamePersonDetectedTwiceException(e.message.get)))), ctx)
        }
        Future.successful(Left(dataAsset), ctx)
      case (Right(invalid), ctx) => Future.successful(Right(invalid), ctx)
    }
}
