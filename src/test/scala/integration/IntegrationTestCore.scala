package integration

import java.io.File

import altitude.models.{MediaType, Asset, Folder}
import altitude.transactions.TransactionId
import altitude.{Const => C, Util, Altitude, Environment}
import com.google.inject.{AbstractModule, Guice}
import integration.util.dao
import integration.util.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import org.slf4j.LoggerFactory

object IntegrationTestCore {
  /*
   initialize app instances for all known datasources to avoid creating
   this for every test class.

  */
  val sqliteApp = new Altitude(Map("datasource" -> "sqlite"))
  val postgresApp = new Altitude(Map("datasource" -> "postgres"))
  val mongoDbApp = new Altitude(Map("datasource" -> "mongo"))
}

abstract class IntegrationTestCore extends FunSuite with BeforeAndAfter with BeforeAndAfterEach {
  val log =  LoggerFactory.getLogger(getClass)
  Environment.ENV = Environment.TEST

  /* Stores test app config overrides, since we run same tests with different app setup.
   */
  val config: Map[String, String]

  // force environment to always be TEST
  val datasource = config.get("datasource")
  protected def altitude: Altitude = datasource match {
    case Some("mongo") => IntegrationTestCore.mongoDbApp
    case Some("postgres") => IntegrationTestCore.postgresApp
    case Some("sqlite") => IntegrationTestCore.sqliteApp
    case _ => throw new IllegalArgumentException(s"Do not know of datasource: $datasource")
  }

  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]
  implicit val txId: TransactionId = new TransactionId

  override def beforeEach() = {
    dbUtilities.migrateDatabase()

    altitude.transactions.COMMITTED = 0
    altitude.transactions.CREATED = 0
    altitude.transactions.CLOSED = 0

    val dataDirFile = new File(altitude.dataPath)
    FileUtils.deleteDirectory(dataDirFile)
    FileUtils.forceMkdir(dataDirFile)
    dbUtilities.createTransaction(txId)
    //log.debug(s"Test transaction ID is ${txId.id}")
  }

  override def afterEach() {
    dbUtilities.cleanupTest()

    if (datasource.contains("postgres") || datasource.contains("sqlite")) {
      // should not have committed anything for tests
      require(altitude.transactions.COMMITTED == 0)
      // should only have had one transaction - if this fails, implicit transaction semantics are off somewhere
      if (altitude.transactions.CREATED != 1) {
        println(s"${altitude.transactions.CREATED} transactions instead of 1!")
      }
      require(altitude.transactions.CREATED == 1)
    }
  }

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      log.info(s"Datasource type: ${altitude.dataSourceType}", C.LogTag.APP)

      altitude.dataSourceType match {
        case "mongo" =>
          bind[UtilitiesDao].toInstance(new dao.mongo.UtilitiesDao(altitude))
        case "postgres" | "sqlite" =>
          bind[UtilitiesDao].toInstance(new dao.jdbc.UtilitiesDao(altitude))
        case _ => throw new IllegalArgumentException(s"Do not know of datasource: ${altitude.dataSourceType}")
      }
    }
  }

  /* INTEGRATION UTILITIES*/
  private val MEDIA_TYPE = new MediaType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

  protected  def makeAsset(folder: Folder) = new Asset(
    folderId = folder.id.get,
    mediaType = MEDIA_TYPE,
    path = Util.randomStr(30),
    md5 = Util.randomStr(30),
    sizeBytes = 1L)

}
