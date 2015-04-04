package integration

import altitude.{Const => C}
import altitude.dao.{LibraryDao, postgres, mongo, UtilitiesDao}
import altitude.util.log
import com.google.inject.{Guice, AbstractModule}
import global.Altitude
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication
import net.codingwell.scalaguice.InjectorExtensions._

abstract class IntegrationTestCore extends FunSuite
  with OneAppPerSuite with ScalaFutures with BeforeAndAfter {
  /* Stores test app config overrides, since we run same tests with different app setup.
   */
  val config: Map[String, _]

  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]
  override lazy val app = FakeApplication(additionalConfiguration = config)
  protected lazy val altitude: Altitude = Altitude.getInstance(this.app)

  // async setup
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  before {
    dbUtilities.dropDatabase()
  }

  after {
  }

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = app.configuration.getString("datasource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)

      dataSourceType match {
        case "mongo" =>
          bind[UtilitiesDao].toInstance(new mongo.UtilitiesDao)
        case "postgres" =>
          bind[UtilitiesDao].toInstance(new postgres.UtilitiesDao)

        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

}
