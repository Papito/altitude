package integration

import java.io.File

import altitude.models.{Asset, AssetType, Folder, User}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Environment, Util}
import com.google.inject.{AbstractModule, Guice}
import integration.util.dao
import integration.util.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import org.slf4j.LoggerFactory
import org.slf4j.MDC

abstract class IntegrationTestCore extends FunSuite with BeforeAndAfter with BeforeAndAfterEach {
  val log =  LoggerFactory.getLogger(getClass)
  Environment.ENV = Environment.TEST

  /* Stores test app config overrides, since we run same tests with a different app setup.
   */
  val config: Map[String, String]

  // force environment to always be TEST
  val datasource = config.get("datasource")
  protected def altitude: Altitude = datasource match {
    case Some("mongo") => MongoSuite.app
    case Some("postgres") => PostgresSuite.app
    case Some("sqlite") => SqliteSuite.app
    case _ => throw new IllegalArgumentException(s"Do not know of datasource: $datasource")
  }

  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]
  implicit val txId: TransactionId = new TransactionId

  private var count = 0

  override def beforeEach() = {
    MDC.put("USER", s"[USR:$CURRENT_USER]")
    count = count + 1
    MDC.put("REQUEST_ID", s"[TEST: $count]")

    dbUtilities.migrateDatabase()

    altitude.transactions.COMMITTED = 0
    altitude.transactions.CREATED = 0
    altitude.transactions.CLOSED = 0

    val dataDirFile = new File(altitude.dataPath)
    FileUtils.deleteDirectory(dataDirFile)
    FileUtils.forceMkdir(dataDirFile)
    dbUtilities.createTransaction(txId)
    //log.debug(s"Test transaction ID is ${txId.id}")
    SET_USER_1()
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
  private val ASSET_TYPE = new AssetType(mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime")

  private final val USER: User = User(
    id = Some("a11111111111111111111111"),
    rootFolderId = "a11111111111111111111111",
    uncatFolderId = "a22222222222222222222222")

  private final val ANOTHER_USER: User = User(
    id = Some("a22222222222222222222222"),
    rootFolderId = "a33333333333333333333333",
    uncatFolderId = "a44444444444444444444444")

  implicit var CURRENT_USER: User = USER
  implicit def CURRENT_USER_ID: String = CURRENT_USER.id.get

  def SET_USER_1() =
    CURRENT_USER = USER
  def SET_USER_2() =
    CURRENT_USER = ANOTHER_USER

  protected def makeAsset(folder: Folder) = Asset(
    userId = CURRENT_USER_ID,
    folderId = folder.id.get,
    assetType = ASSET_TYPE,
    path = Util.randomStr(30),
    md5 = Util.randomStr(30),
    sizeBytes = 1L)

}
