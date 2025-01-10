package software.altitude.core.pipeline.flows

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow
import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineTypes.TFaceWithContext
import software.altitude.core.pipeline.PipelineUtils.setThreadLocalRequestContext

/** This scoops up aligned grayscale face images from the file store */
object AddFaceDataToModelFlow {
  def apply(app: Altitude): Flow[TFaceWithContext, TFaceWithContext, NotUsed] = {
    Flow[TFaceWithContext].map {
      case (face, ctx) =>
        setThreadLocalRequestContext(ctx)
        app.service.faceRecognition.indexFace(face, face.personLabel.get)
        (face, ctx)
    }
  }
}
