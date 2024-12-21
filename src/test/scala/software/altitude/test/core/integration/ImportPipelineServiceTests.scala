package software.altitude.test.core.integration

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.DoNotDiscover
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.have
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import software.altitude.core.Altitude
import software.altitude.core.models.AssetWithData
import software.altitude.core.service.PipelineContext
import software.altitude.test.core.IntegrationTestCore

import scala.concurrent.Future


@DoNotDiscover class ImportPipelineServiceTests(override val testApp: Altitude) extends IntegrationTestCore with Eventually {
  implicit val customPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))

    test("Pipeline should import multiple assets", Focused) {
      val batchSize = 10
      val dataAssetsIn = (1 to batchSize).map(_ => testContext.makeAssetWithData())

      val pipelineContext = PipelineContext(testContext.repository, testContext.user)
      val source: Source[(AssetWithData, PipelineContext), NotUsed] = Source.fromIterator(
        () => dataAssetsIn.iterator).map((_, pipelineContext))

      val pipelineResFuture: Future[Seq[AssetWithData]] = testApp.service.importPipeline.run(source)

      eventually {
        pipelineResFuture.futureValue should have size batchSize
      }
    }
}
