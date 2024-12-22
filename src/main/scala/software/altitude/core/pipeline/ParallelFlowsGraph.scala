package software.altitude.core.pipeline

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.Graph
import org.apache.pekko.stream.scaladsl.Broadcast
import org.apache.pekko.stream.scaladsl.GraphDSL
import org.apache.pekko.stream.scaladsl.ZipWith
import software.altitude.core.Altitude
import software.altitude.core.pipeline.PipelineTypes.TAssetOrInvalidWithContext

object ParallelFlowsGraph {
  def apply(app: Altitude): Graph[FlowShape[TAssetOrInvalidWithContext, TAssetOrInvalidWithContext], NotUsed] = GraphDSL.create() {
    val fileStoreFlow = FileStoreFlow(app)
    val updateStatsFlow = UpdateStatsFlow(app)
    val addPreviewFlow = AddPreviewFlow(app)

    implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[TAssetOrInvalidWithContext](3))

      val zip = builder.add(
        ZipWith[
          TAssetOrInvalidWithContext,
          TAssetOrInvalidWithContext,
          TAssetOrInvalidWithContext,
          TAssetOrInvalidWithContext](
          (extractMetadataSubstream, _, _) => extractMetadataSubstream)
      )

      // Define the flows
      val fileStoreFlowShape = builder.add(fileStoreFlow)
      val updateStatsFlowShape = builder.add(updateStatsFlow)
      val addPreviewFlowShape = builder.add(addPreviewFlow)

      // Connect the flows
      broadcast ~> fileStoreFlowShape ~> zip.in0
      broadcast ~> updateStatsFlowShape ~> zip.in1
      broadcast ~> addPreviewFlowShape ~> zip.in2

      FlowShape(broadcast.in, zip.out)
  }
}
