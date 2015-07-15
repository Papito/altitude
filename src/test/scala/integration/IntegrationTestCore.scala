package integration

import altitude.transactions.{Transaction, TransactionId}
import altitude.{Altitude, Const => C, Environment}
import com.google.inject.{AbstractModule, Guice}
import integration.util.dao
import integration.util.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import org.slf4j.LoggerFactory

abstract class IntegrationTestCore extends FunSuite with BeforeAndAfter with BeforeAndAfterEach {
  val log =  LoggerFactory.getLogger(getClass)
  Environment.ENV = Environment.TEST

  /* Stores test app config overrides, since we run same tests with different app setup.
   */
  val config: Map[String, String]

  // force environment to always be TEST
  protected lazy val altitude: Altitude = new Altitude(additionalConfiguration = config)
  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]
  implicit val txId: TransactionId = new TransactionId

  before {
    dbUtilities.dropDatabase()
  }

  override def beforeEach() = {
    println("\n")
    dbUtilities.createTransaction(txId)
  }

  override def afterEach() {
    dbUtilities.cleanup()
    // should not have committed anything, else implicit transaction id is not being propagated somewhere
    require(Transaction.COMMITTED == 0)
  }

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = altitude.config.getString("datasource")
      log.info(s"Datasource type: $dataSourceType", C.tag.APP)

      dataSourceType match {
        case "mongo" =>
          bind[UtilitiesDao].toInstance(new dao.mongo.UtilitiesDao(altitude))
        case "postgres" =>
          bind[UtilitiesDao].toInstance(new dao.postgres.UtilitiesDao(altitude))
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

}
