package altitude

import java.sql.DriverManager

import altitude.dao._
import altitude.dao.mongo.BaseMongoDao
import altitude.service._
import altitude.service.migration.{MongoMigrationService, PostgresMigrationService, SqliteMigrationService}
import altitude.transactions._
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory

class Altitude(additionalConfiguration: Map[String, Any] = Map()) {
  private final val log = LoggerFactory.getLogger(getClass)

  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  log.info(s"Initializing Altitude application instance with ID $id")

  val environment = Environment.ENV match {
    case Environment.DEV => "development"
    case Environment.PROD => "production"
    case Environment.TEST => "test"
  }

  log.info(s"Environment is: $environment")

  val config = new Configuration(
    additionalConfiguration = additionalConfiguration)

  val dataSourceType = config.getString("datasource")
  log.info(s"Datasource type: $dataSourceType", C.LogTag.APP)

  val app: Altitude = this

  /*
  Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      dataSourceType match {
        case "mongo" => {
          bind[AbstractTransactionManager].toInstance(new altitude.transactions.VoidTransactionManager(app))

          bind[MigrationDao].toInstance(new mongo.MigrationDao(app))
          bind[AssetDao].toInstance(new mongo.AssetDao(app))
          bind[TrashDao].toInstance(new mongo.TrashDao(app))
          bind[ImportProfileDao].toInstance(new mongo.ImportProfileDao(app))
          bind[FolderDao].toInstance(new mongo.FolderDao(app))
        }
        case "postgres" => {
          DriverManager.registerDriver(new org.postgresql.Driver)

          val JDBC_TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()
          val jdbcTxManager = new altitude.transactions.JdbcTransactionManager(app, JDBC_TRANSACTIONS)
          bind[AbstractTransactionManager].toInstance(jdbcTxManager)
          bind[JdbcTransactionManager].toInstance(jdbcTxManager)

          bind[MigrationDao].toInstance(new postgres.MigrationDao(app))
          bind[AssetDao].toInstance(new postgres.AssetDao(app))
          bind[TrashDao].toInstance(new postgres.TrashDao(app))
          //bind[ImportProfileDao].toInstance(new postgres.ImportProfileDao(app))
          bind[FolderDao].toInstance(new postgres.FolderDao(app))
        }
        case "sqlite" => {
          DriverManager.registerDriver(new org.sqlite.JDBC)

          val JDBC_TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()
          val jdbcTxManager = new SqliteTransactionManager(app, JDBC_TRANSACTIONS)
          bind[AbstractTransactionManager].toInstance(jdbcTxManager)
          bind[JdbcTransactionManager].toInstance(jdbcTxManager)

          bind[MigrationDao].toInstance(new sqlite.MigrationDao(app))
          bind[AssetDao].toInstance(new sqlite.AssetDao(app))
          bind[TrashDao].toInstance(new sqlite.TrashDao(app))
          //bind[ImportProfileDao].toInstance(new sqlite.ImportProfileDao(app))
          bind[FolderDao].toInstance(new sqlite.FolderDao(app))
        }
        case _ => {
          throw new IllegalArgumentException(s"Do not know of datasource $dataSourceType")
        }
      }
    }
  }

  val injector = Guice.createInjector(new InjectionModule)

  // create all services
  object service {
    val fileImport = new FileImportService(app)
    val metadata = new TikaMetadataService
    val library = new LibraryService(app)
    val asset = new AssetService(app)
    val trash = new TrashService(app)
    val preview = new PreviewService(app)
    val folder = new FolderService(app)
//    val importProfile = new ImportProfileService(app)
    val tagConfig = new TagConfigService(app)

    val migration = dataSourceType match {
      case "mongo" => new MongoMigrationService(app)
      case "sqlite" => new SqliteMigrationService(app)
      case "postgres" => new PostgresMigrationService(app)
    }
  }

  object transactions {
    var CREATED = 0
    var COMMITTED = 0
    var CLOSED = 0
  }

  if (config.getFlag("migrationsEnabled")) {
    service.migration.initDb()
    val migrationRequired = service.migration.migrationRequired()
    if (migrationRequired) {
      log.warn("Migration is required!")
    }

    if (migrationRequired && service.migration.migrationConfirmed) {
      log.info("Migration go-ahead confirmed by user")
      service.migration.migrate()
    }
  }

  def freeResources(): Unit = {
    if (BaseMongoDao.CLIENT.isDefined) {
      log.info("Closing MONGO client")
      BaseMongoDao.CLIENT.get.close()
    }
    log.info("Freeing transaction list")
  }

  val workPath = System.getProperty("user.dir")
  val dataDir = config.getString("dataDir")
  val dataPath = workPath + "/" + dataDir + "/"
  log.info(s"Data path is '$dataPath'")
  val previewPath = dataPath + "p/"
  log.info(s"Altitude instance initialized")
}