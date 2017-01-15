package altitude

import java.sql.DriverManager

import altitude.dao._
import altitude.service._
import altitude.service.migration.{PostgresMigrationService, SqliteMigrationService}
import altitude.transactions._
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory

class Altitude(configOverride: Map[String, Any] = Map()) {
  private final val log = LoggerFactory.getLogger(getClass)
  final val app: Altitude = this

  // ID for this application - which we may have multiple of in the same environment
  final val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  log.info(s"Initializing Altitude application instance with ID $id")

  // this is the environment we are running in
  final val environment = Environment.ENV match {
    case Environment.DEV => "development"
    case Environment.PROD => "production"
    case Environment.TEST => "test"
  }
  log.info(s"Environment is: $environment")

  final val config = new Configuration(
    configOverride = configOverride)

  final val dataSourceType = config.getString("datasource")
  log.info(s"Datasource type: $dataSourceType", C.LogTag.APP)

  /**
   * At this point determine which data access classes we are loading, which
   * transaction manager we are using for the data sources of choice, load the drivers,
   * etc.
   */
  final val injector = Guice.createInjector(new InjectionModule)

  /**
   * This is our injected transaction manager, determined based on our database.
   */
  final val txManager = app.injector.instance[AbstractTransactionManager]

  /**
   * This is all of the internal services the app will be using
   */
  object service {
    val repository = new RepositoryService(app)
    val fileImport = new FileImportService(app)
    val metadataExtractor = new TikaMetadataExtractionService
    val metadata = new MetadataService(app)
    val library = new LibraryService(app)
    val search = new SearchService(app)
    val asset = new AssetService(app)
    val preview = new PreviewService(app)
    val data = new DataService(app)
    val folder = new FolderService(app)
    val stats = new StatsService(app)

    val migration = dataSourceType match {
      case "sqlite" => new SqliteMigrationService(app)
      case "postgres" => new PostgresMigrationService(app)
    }
  }

  if (service.migration.migrationRequired) {
    log.warn("Migration is required!")
    service.migration.migrate()
  }

  /**
   * Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      dataSourceType match {

        case "postgres" =>
          DriverManager.registerDriver(new org.postgresql.Driver)

          val jdbcTxManager = new altitude.transactions.JdbcTransactionManager(app)
          bind[AbstractTransactionManager].toInstance(jdbcTxManager)
          bind[JdbcTransactionManager].toInstance(jdbcTxManager)

          bind[MigrationDao].toInstance(new postgres.MigrationDao(app))
          bind[RepositoryDao].toInstance(new postgres.RepositoryDao(app))
          bind[AssetDao].toInstance(new postgres.AssetDao(app))
          bind[FolderDao].toInstance(new postgres.FolderDao(app))
          bind[StatDao].toInstance(new postgres.StatDao(app))
          bind[MetadataFieldDao].toInstance(new postgres.MetadataFieldDao(app))
          bind[SearchDao].toInstance(new postgres.SearchDao(app))

        case "sqlite" =>
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
          throw new IllegalArgumentException(s"Do not know of datasource $dataSourceType")
      }
    }
  }

  def freeResources(): Unit = {
    txManager.freeResources()
  }

  // DEPRECATED. this will be part of data access
  val workPath = System.getProperty("user.dir")
  val dataDir = config.getString("dataDir")
  val dataPath = workPath + "/" + dataDir + "/"
  log.info(s"Data path is '$dataPath'")
  val previewPath = dataPath + "p/"
  log.info(s"Altitude instance initialized")
}