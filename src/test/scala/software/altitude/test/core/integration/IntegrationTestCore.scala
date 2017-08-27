package software.altitude.test.core.integration

import java.io.File

import com.google.inject.{AbstractModule, Guice}
import software.altitude.test.core.integration.util.dao.{jdbc, UtilitiesDao}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite, Tag}
import org.slf4j.{LoggerFactory, MDC}
import software.altitude.core.Const.FileStoreType
import software.altitude.core.models._
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Const => C, _}
import software.altitude.test.core.suites.{PostgresSuite, SqliteSuite}

abstract class IntegrationTestCore extends FunSuite with BeforeAndAfter with BeforeAndAfterEach {
  val log =  LoggerFactory.getLogger(getClass)

  // Stores test app config overrides, since we run same tests with a different app setup.
  val config: Map[String, Any]

  // Force environment to always be TEST
  Environment.ENV = Environment.TEST

  val datasource = config.get("datasource").get.asInstanceOf[C.DatasourceType.Value]
  protected def altitude = datasource match {
    case C.DatasourceType.POSTGRES => PostgresSuite.app
    case C.DatasourceType.SQLITE => SqliteSuite.app
    case _ => throw new IllegalArgumentException(s"Do not know of datasource: $datasource")
  }

  /**
   * Inject DB utilities, based on current data source
   */
  val injector = Guice.createInjector(new InjectionModule)
  protected val dbUtilities = injector.instance[UtilitiesDao]

  /**
   * Extremely important! This is the one and only transaction id for tests.
   * This has to be controlled by us here, and always be implicitly defined.
   * The transaction managers will not commit transactions if it's an existing
   * transaction ID. This allows us to rollback every test, keeping the database
   * clean.
   */
  implicit val txId: TransactionId = new TransactionId

  /**
   * Scalatest tag to run a specific test[s]
   *
   * test("work in progress", CurrentTag) {
   *
   * }
   *
   * To run:
   *
   * sbt> test-only -- -n Current
   * sbt> test-only integration.SqliteSuite -- -n Current
   */
  object CurrentTag extends Tag("Current")


  /**
   * Our test users. We may alternate between them to make sure there is proper
   * separation.
   */
  private final val user: User = altitude.USER
  private final val anotherUser: User = User(id = Some("a22222222222222222222222"))
  var currentUser = user

  /**
   * Just as with users, we need at least two repositories to make sure repository
   * bounds are not broken. Normally, no request should ever be able to peek into
   * data from other repositories. This is enforced in the DAO layer.
   */
  private val repo = altitude.REPO

  private val workPath = FileUtils.getUserDirectory.getAbsolutePath
  private val dataDir = altitude.config.getString("dataDir")
  private val dataPath2 = FilenameUtils.concat(FilenameUtils.concat(workPath, dataDir), "2")

  private val repo2 = new Repository(name = "Repository 2",
    id = Some("a20000000000000000000000"),
    rootFolderId  = C.Folder.IDs.ROOT,
    triageFolderId = C.Folder.IDs.TRIAGE,
    fileStoreType = FileStoreType.FS,
    fileStoreConfig = Map(C.Repository.Config.PATH -> dataPath2))
  var currentRepo = repo

  /**
   * Our implicit context for all tests.
   * Note that it is a function, so it will dynamically set the current
   * user and repository.
   */
  implicit final def ctx: Context = new Context(repo = currentRepo, user = currentUser)

  /**
   * Methods to toggle between different user and repositories.
   */
  def SET_PRIMARY_USER() = {
    currentUser = user
  }
  def SET_SECONDARY_USER() = {
    currentUser = anotherUser
  }

  def SET_PRIMARY_REPO() = {
    currentRepo = repo
  }

  def SET_SECONDARY_REPO() = {
    currentRepo = repo2
  }

  /**
   * A helper method to quickly cook a test asset
   */
  protected def makeAsset(folder: Folder, metadata: Metadata = new Metadata()) = Asset(
    userId = currentUser.id.get,
    folderId = folder.id.get,
    assetType = new AssetType(
      mediaType = "mediaType", mediaSubtype = "mediaSubtype", mime = "mime"),
    fileName = Util.randomStr(50),
    path = Some(Util.randomStr(50)),
    md5 = Util.randomStr(32),
    metadata = metadata,
    sizeBytes = 1000L)

  /**
   * Convert a file system resource to an import asset
   */
  def fileToImportAsset(file: File): ImportAsset = new ImportAsset(
    fileName = file.getName,
    path = file.getAbsolutePath,
    sourceType = C.FileStoreType.FS,
    data =  FileUtils.readFileToByteArray(file),
    metadata = new Metadata())

  // test count - we use it as a request ID for our logging environment
  private var count = 0

  override def beforeEach() = {
    // this is for logging context
    MDC.put("USER", s"[USR:$currentUser]")
    count = count + 1
    MDC.put("REQUEST_ID", s"[TEST: $count]")

    // FIXME: should be in settings
    val testDir = new File("tmp/test/data")
    FileUtils.cleanDirectory(testDir)
    FileUtils.forceMkdir(testDir)

    dbUtilities.migrateDatabase()

    // keep transaction stats clean after DB migration dirties them
    altitude.txManager.transactions.reset()


    dbUtilities.createTransaction(txId)

    SET_PRIMARY_USER()
    SET_PRIMARY_REPO()
  }

  override def afterEach() {
    dbUtilities.cleanupTest()

    if (datasource == C.DatasourceType.SQLITE || datasource == C.DatasourceType.POSTGRES) {
      // should not have committed anything for tests
      require(altitude.txManager.transactions.COMMITTED == 0)
      // should only have had one transaction - if this fails, implicit transaction logic is likely broken
      if (altitude.txManager.transactions.CREATED != 1) {
        log.error(s"${altitude.txManager.transactions.CREATED} transactions instead of 1!")
      }
      require(altitude.txManager.transactions.CREATED == 1)
    }
  }

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      log.info(s"Datasource type: ${altitude.dataSourceType}", C.LogTag.APP)

      altitude.dataSourceType match {
        case C.DatasourceType.POSTGRES | C.DatasourceType.SQLITE =>
          bind[UtilitiesDao].toInstance(new jdbc.UtilitiesDao(altitude))
        case _ => throw new IllegalArgumentException(s"Do not know of datasource: ${altitude.dataSourceType}")
      }
    }
  }
}