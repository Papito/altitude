package software.altitude.core

import java.sql.DriverManager

import com.google.inject.{Injector, AbstractModule, Guice}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import software.altitude.core.Const.FileStoreType
import software.altitude.core.dao._
import software.altitude.core.models.{Repository, User}
import software.altitude.core.service._
import software.altitude.core.service.filestore.{FileStoreService, FileSystemStoreService}
import software.altitude.core.service.migration._
import software.altitude.core.service.sources.FileSystemSourceService
import software.altitude.core.transactions._
import software.altitude.core.{Const => C}

class Altitude(configOverride: Map[String, Any] = Map()) extends AltitudeCoreApp  {
  private final val log = LoggerFactory.getLogger(getClass)
  log.info(s"Initializing Altitude Server application instance with ID [$id]")

  final val app: Altitude = this

  // TEMPORARY constants for user and repo IDS
  val workPath = System.getProperty("user.dir")
  val dataDir = config.getString("dataDir")
  val dataPath = FilenameUtils.concat(workPath, dataDir)
  log.info(s"Data path is '$dataPath'")

  final val REPO = new Repository(name = "Repository",
    id = Some("a10000000000000000000000"),
    rootFolderId  = C.Folder.IDs.ROOT,
    triageFolderId = C.Folder.IDs.TRIAGE,
    fileStoreType = FileStoreType.FS,
    fileStoreConfig = Map(C.Repository.Config.PATH -> dataPath))

  final val USER = new User(Some("a11111111111111111111111"))
  // end TEMP definitions

  final val fileStoreType = REPO.fileStoreType
  log.info(s"File store type: $fileStoreType")

  /**
   * At this point determine which data access classes we are loading, which
   * transaction manager we are using for the data sources of choice, load the drivers,
   * etc.
   */
  override val injector: Injector = Guice.createInjector(new InjectionModule)

  /**
   * This is our injected transaction manager, determined based on our database.
   */
  override val txManager = app.injector.instance[AbstractTransactionManager]

  /**
   * This is all of the services the app will be using
   */
  object service {
    val repository = new RepositoryService(app)
    val assetImport = new AssetImportService(app)
    val metadataExtractor = new TikaMetadataExtractionService
    val metadata = new MetadataService(app)
    val library = new LibraryService(app)
    val search = new SearchService(app)
    val asset = new AssetService(app)
    val folder = new FolderService(app)
    val stats = new StatsService(app)

    object source {
      val fileSystem = new FileSystemSourceService(app)
    }

    val fileStore: FileStoreService = fileStoreType match {
      case C.FileStoreType.FS => new FileSystemStoreService(app)
      case _ => throw new NotImplementedError
    }

    val migration = dataSourceType match {
      case C.DatasourceType.SQLITE => new ServerMigrations(app) with JdbcMigrationService with Sqlite
      case C.DatasourceType.POSTGRES => new ServerMigrations(app) with JdbcMigrationService with Postgres
    }
  }

  /**
   * Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      dataSourceType match {

        case C.DatasourceType.POSTGRES =>
          DriverManager.registerDriver(new org.postgresql.Driver)

          val jdbcTxManager = new software.altitude.core.transactions.JdbcTransactionManager(app)
          bind[AbstractTransactionManager].toInstance(jdbcTxManager)
          bind[JdbcTransactionManager].toInstance(jdbcTxManager)

          bind[MigrationDao].toInstance(new postgres.MigrationDao(app))
          bind[RepositoryDao].toInstance(new postgres.RepositoryDao(app))
          bind[AssetDao].toInstance(new postgres.AssetDao(app))
          bind[FolderDao].toInstance(new postgres.FolderDao(app))
          bind[StatDao].toInstance(new postgres.StatDao(app))
          bind[MetadataFieldDao].toInstance(new postgres.MetadataFieldDao(app))
          bind[SearchDao].toInstance(new postgres.SearchDao(app))

        case C.DatasourceType.SQLITE =>
          DriverManager.registerDriver(new org.sqlite.JDBC)

          val jdbcTxManager = new SqliteTransactionManager(app)
          bind[AbstractTransactionManager].toInstance(jdbcTxManager)
          bind[JdbcTransactionManager].toInstance(jdbcTxManager)

          bind[MigrationDao].toInstance(new sqlite.MigrationDao(app))
          bind[RepositoryDao].toInstance(new sqlite.RepositoryDao(app))
          bind[AssetDao].toInstance(new sqlite.AssetDao(app))
          bind[FolderDao].toInstance(new sqlite.FolderDao(app))
          bind[StatDao].toInstance(new sqlite.StatDao(app))
          bind[MetadataFieldDao].toInstance(new sqlite.MetadataFieldDao(app))
          bind[SearchDao].toInstance(new sqlite.SearchDao(app))

        case _ =>
          throw new IllegalArgumentException(s"Do not know of datasource [$dataSourceType]")
      }
    }
  }

  def freeResources(): Unit = {
    txManager.freeResources()
  }

  override def runMigrations(): Unit = {
    if (service.migration.migrationRequired) {
      log.warn("Migration is required!")
      if (Environment.ENV != Environment.TEST) {
        service.migration.migrate()
      }
    }
  }

  log.info(s"Altitude Server instance initialized")
}