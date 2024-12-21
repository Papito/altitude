package software.altitude.test.core.integration

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.have
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.{AssetType, AssetWithData}
import software.altitude.core.service.PipelineContext
import software.altitude.test.core.IntegrationTestCore

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


@DoNotDiscover class ImportPipelineServiceTests(override val testApp: Altitude)
  extends IntegrationTestCore {

  test("Pipeline should import multiple assets") {
    val batchSize = 10
    val dataAssets = (1 to batchSize).map(_ => testContext.makeAssetWithData())

    val pipelineContext = PipelineContext(testContext.repository, testContext.user)
    val source: Source[(AssetWithData, PipelineContext), NotUsed] = Source.fromIterator(
      () => dataAssets.iterator).map((_, pipelineContext))

    val pipelineResFuture: Future[Seq[AssetWithData]] = testApp.service.importPipeline.run(source)

    Await.result(pipelineResFuture, Duration.Inf) should have size batchSize
  }

  test("Pipeline should complete on duplicate asset errors", Focused) {
    val batchSize = 10
    val dataAssets = (1 to batchSize).map(_ => testContext.makeAssetWithData())

    // now it's 11, but the second one is a duplicate
    val dataAssetsWithDuplicate = dataAssets.head +: dataAssets

    val pipelineContext = PipelineContext(testContext.repository, testContext.user)
    val source: Source[(AssetWithData, PipelineContext), NotUsed] = Source.fromIterator(
      () => dataAssetsWithDuplicate.iterator).map((_, pipelineContext))

    val pipelineResFuture: Future[Seq[AssetWithData]] = testApp.service.importPipeline.run(source)

    Await.result(pipelineResFuture, Duration.Inf) should have size batchSize
  }

  test("Pipeline should complete on unsupported media type errors", Focused) {
    val badMediaType = AssetType("bad", "type", "mime")
    val assetWithBadMediaType = testContext.makeAsset().copy(assetType = badMediaType)
    val assetWithData = testContext.makeAssetWithData().copy(asset = assetWithBadMediaType)

    val pipelineContext = PipelineContext(testContext.repository, testContext.user)
    val source: Source[(AssetWithData, PipelineContext), NotUsed] = Source.single(assetWithData, pipelineContext)

    val pipelineResFuture: Future[Seq[AssetWithData]] = testApp.service.importPipeline.run(source)

    Await.result(pipelineResFuture, Duration.Inf) should have size 1
  }
}
