package software.altitude.core.pipeline

import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Sink
import software.altitude.core.pipeline.PipelineConstants.DEBUG
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext

import scala.concurrent.Future

object Sinks {
  val toConsoleErrorSink: Sink[Invalid, Future[Done]] = Sink.foreach[Invalid](each => if (DEBUG) println(s"Reached errorSink: $each"))

  val voidErrorSink: Sink[Invalid, Future[Done]] = Sink.ignore

  val seqOutputSink: Sink[TAssetOrInvalidWithContext, Future[Seq[TAssetOrInvalidWithContext]]] = Sink.seq[TAssetOrInvalidWithContext]

  val voidOutputSink: Sink[TAssetOrInvalidWithContext, Future[Seq[TAssetOrInvalidWithContext]]] =
    Sink.fold(Seq.empty[TAssetOrInvalidWithContext])((acc, _) => acc)
}
