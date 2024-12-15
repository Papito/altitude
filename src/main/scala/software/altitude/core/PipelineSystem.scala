package software.altitude.core

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import software.altitude.core.models.AssetWithData

object PipelineSystem {
  sealed trait Command
  case class ProcessAssetCommand(assetWithData: AssetWithData) extends Command

  def apply(): Behavior[ProcessAssetCommand] =
    Behaviors.setup {
      context =>
        context.log.info("Starting the Pipeline actor system....")
        Behaviors.receiveMessage {
          case ProcessAssetCommand(assetWithData) =>
            context.log.info("Processing asset: {}", assetWithData.asset.id)
            Behaviors.same
        }
    }
}
