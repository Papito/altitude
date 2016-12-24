package integration

import java.io.File

import altitude.models._
import altitude.{Const => C, _}
import com.google.inject.{AbstractModule, Guice}
import integration.util.dao
import integration.util.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import org.slf4j.{LoggerFactory, MDC}

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

  private var count = 0

  override def beforeEach() = {
    MDC.put("USER", s"[USR:$currentUser]")
    count = count + 1
    MDC.put("REQUEST_ID", s"[TEST: $count]")

    dbUtilities.migrateDatabase()

    // keep transaction stats clean after DB migration dirties them
    altitude.transactions.COMMITTED = 0
    altitude.transactions.CREATED = 0
    altitude.transactions.CLOSED = 0

    val dataDirFile = new File(altitude.dataPath)
    FileUtils.deleteDirectory(dataDirFile)
    FileUtils.forceMkdir(dataDirFile)
    dbUtilities.createTransaction(ctx)
    //log.debug(s"Test transaction ID is ${txId.id}")
    SET_USER_1()
    SET_PRIMARY_REPO()
  }

  override def afterEach() {
    dbUtilities.cleanupTest()

    if (datasource.contains("postgres") || datasource.contains("sqlite")) {
      // should not have committed anything for tests
      require(altitude.transactions.COMMITTED == 0)
      // should only have had one transaction - if this fails, implicit transaction semantics are off somewhere
      if (altitude.transactions.CREATED != 1) {
        log.error(s"${altitude.transactions.CREATED} transactions instead of 1!")
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

  private final val user: User = C.USER

  private final val anotherUser: User = User(id = Some("a22222222222222222222222"))

  var currentUser = user
  def currentUserId: String = currentUser.id.get

  private val repo = C.REPO

  private val repo2 = new Repository(name = "Repository 2",
    id = Some("a20000000000000000000000"),
    rootFolderId  = "b20000000000000000000000",
    uncatFolderId = "c20000000000000000000000")

  var currentRepo = repo

  implicit var ctx: Context = new Context(repo = repo, user = currentUser)

  def SET_USER_1() = {
    currentUser = user
    ctx = new Context(repo = currentRepo, user = currentUser, txId = ctx.txId)
  }
  def SET_USER_2() = {
    currentUser = anotherUser
    ctx = new Context(repo = currentRepo, user = currentUser, txId = ctx.txId)
  }

  def SET_PRIMARY_REPO() = {
    currentRepo = repo
    ctx = new Context(repo = currentRepo, user = currentUser, txId = ctx.txId)
  }

  def SET_SECONDARY_REPO() = {
    currentRepo = repo2
    ctx = new Context(repo = currentRepo, user = currentUser, txId = ctx.txId)
  }

  protected def makeAsset(folder: Folder) = Asset(
    userId = currentUserId,
    repoId = ctx.repo.id.get,
    folderId = folder.id.get,
    assetType = ASSET_TYPE,
    path = Util.randomStr(50),
    md5 = Util.randomStr(32),
    sizeBytes = 1L)

}
