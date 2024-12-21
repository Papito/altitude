package software.altitude.core.pipeline

import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import software.altitude.core.pipeline.PipelineConstants.DEBUG
import software.altitude.core.pipeline.PipelineTypes.Invalid
import org.apache.pekko.NotUsed

object Sinks {
  val defaultErrorSink: Sink[Invalid,NotUsed] = Flow[Invalid]
    .map(
      each => {
        if (DEBUG) println(s"Reached errorSink: $each")
      })
    .to(Sink.ignore)

}
