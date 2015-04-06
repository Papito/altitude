package integration

import altitude.dao.{Transaction, TransactionId}
import altitude.util.log
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import global.Altitude
import integration.util.dao
import integration.util.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

abstract class IntegrationTestCore extends FunSuite
  with OneAppPerSuite with ScalaFutures with BeforeAndAfter with BeforeAndAfterEach {
  /* Stores test app config overrides, since we run same tests with different app setup.
   */
  val config: Map[String, _]

  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]
  override lazy val app = FakeApplication(additionalConfiguration = config)
  protected lazy val altitude: Altitude = Altitude.getInstance(this.app)

  // async setup
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  implicit val txId: TransactionId = new TransactionId

  before {
    dbUtilities.dropDatabase()
  }

  override def beforeEach() = {
    dbUtilities.createTransaction(txId)
  }

  override def afterEach() {
    dbUtilities.cleanup()
    // should not have committed anything, else implicit transaction id is not being propagated somewhere
    require(Transaction.COMMITTED == 0)
  }

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = app.configuration.getString("datasource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)

      dataSourceType match {
        case "mongo" =>
          bind[UtilitiesDao].toInstance(new dao.mongo.UtilitiesDao)
        case "postgres" =>
          bind[UtilitiesDao].toInstance(new dao.postgres.UtilitiesDao)

        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

}
