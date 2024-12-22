package software.altitude.core.pipeline

import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Sink
import software.altitude.core.pipeline.PipelineConstants.DEBUG
import software.altitude.core.pipeline.PipelineTypes.Invalid
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalid

import scala.concurrent.Future

object Sinks {
  val toConsoleErrorSink: Sink[Invalid, Future[Done]] = Sink.foreach[Invalid](each => if (DEBUG) println(s"Reached errorSink: $each"))

  val voidErrorSink: Sink[Invalid, Future[Done]] = Sink.ignore

  val seqOutputSink: Sink[TAssetOrInvalid, Future[Seq[TAssetOrInvalid]]] = Sink.seq[TAssetOrInvalid]

  val voidOutputSink: Sink[TAssetOrInvalid, Future[Seq[TAssetOrInvalid]]] = Sink.fold(Seq.empty[TAssetOrInvalid])((acc, _) => acc)
}
