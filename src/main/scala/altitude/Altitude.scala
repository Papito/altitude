package altitude

import java.sql.DriverManager

import altitude.dao.{ImportProfileDao, AssetDao, PreviewDao}
import altitude.service._
import altitude.transactions.{AbstractTransactionManager, JdbcTransaction}
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory

class Altitude(additionalConfiguration: Map[String, String] = Map()) {
  val log =  LoggerFactory.getLogger(getClass)

  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  log.info(s"Initializing Altitude application instance with ID $id")

  val environment = Environment.ENV match {
    case Environment.DEV => "developGment"
    case Environment.PROD => "production"
    case Environment.TEST => "test"
  }

  log.info(s"Environment is: $environment")

  val config = new Configuration(
    additionalConfiguration = additionalConfiguration)
  protected val dataSourceType = config.getString("datasource")
  log.info(s"Datasource type: $dataSourceType", C.LogTag.APP)

  val app: Altitude = this
  val JDBC_TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()

  /*
  Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      dataSourceType match {
        case "mongo" => {
          bind[AbstractTransactionManager].toInstance(new altitude.transactions.VoidTransactionManager(app))
          bind[AssetDao].toInstance(new altitude.dao.mongo.AssetDao(app))
          bind[PreviewDao].toInstance(new altitude.dao.mongo.PreviewDao(app))
          bind[ImportProfileDao].toInstance(new altitude.dao.mongo.ImportProfileDao(app))
        }
        case "postgres" => {
          DriverManager.registerDriver(new org.postgresql.Driver)

          bind[AbstractTransactionManager].toInstance(new altitude.transactions.JdbcTransactionManager(app))

          bind[AssetDao].toInstance(new altitude.dao.postgres.AssetDao(app))
          bind[PreviewDao].toInstance(new altitude.dao.postgres.PreviewDao(app))
          bind[ImportProfileDao].toInstance(new altitude.dao.postgres.ImportProfileDao(app))
        }
        case "sqlite" => {
          DriverManager.registerDriver(new org.sqlite.JDBC)

          bind[AbstractTransactionManager].toInstance(new altitude.transactions.JdbcTransactionManager(app))

          bind[AssetDao].toInstance(new altitude.dao.postgres.AssetDao(app))
          bind[PreviewDao].toInstance(new altitude.dao.postgres.PreviewDao(app))
          bind[ImportProfileDao].toInstance(new altitude.dao.postgres.ImportProfileDao(app))
        }
        case _ => {
          throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
        }
      }
    }
  }

  val injector = Guice.createInjector(new InjectionModule)
  val txManager = app.injector.instance[AbstractTransactionManager]

  // declare singleton services
  object service {
    val fileImport = new FileImportService(app)
    val metadata = new TikaMetadataService
    val library = new LibraryService(app)
    val asset = new AssetService(app)
    val preview = new PreviewService(app)
    val importProfile = new ImportProfileService(app)
    val tagConfig = new TagConfigService(app)
  }

  object transactions {
    var CREATED = 0
    var COMMITTED = 0
    var CLOSED = 0
  }
}