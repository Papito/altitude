package software.altitude.core.pipeline.sinks

import org.apache.pekko.stream.scaladsl.Sink

import scala.concurrent.Future

import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext

object SeqOutputSink {
  def apply(): Sink[TAssetOrInvalidWithContext, Future[Seq[TAssetOrInvalidWithContext]]] = Sink.seq[TAssetOrInvalidWithContext]
}
