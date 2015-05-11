package altitude

import altitude.transactions.AbstractTransactionManager
import org.slf4j.LoggerFactory

//import altitude.dao._
//import altitude.services._
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.ScalaModule

class Altitude(additionalConfiguration: Map[String, String] = Map(),
               val isTest: Boolean = false,
               val isProd: Boolean = false) {
  val log =  LoggerFactory.getLogger(getClass)

  log.info("Initializing Altitude application instance")
  log.info(s"Test? $isTest, Prod? $isProd")

  // at least one ENV should be chosen
  require(isTest || isProd)
  // but not two or more at the same time
  require(List(isTest, isProd).count(_ == true) == 1)

  val config = new Configuration(additionalConfiguration = additionalConfiguration)

  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  val app: Altitude = this

  /*
  Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = config.get("datasource")
      log.info(s"Datasource type: $dataSourceType", C.tag.APP)
      dataSourceType match {
        case "mongo" =>
          bind[AbstractTransactionManager].toInstance(new altitude.transactions.VoidTransactionManager(app))
          //bind[LibraryDao].toInstance(new altitude.dao.mongo.LibraryDao)
        case "postgres" =>
          bind[AbstractTransactionManager].toInstance(new altitude.transactions.JdbcTransactionManager(app))
          //bind[LibraryDao].toInstance(new altitude.dao.postgres.LibraryDao)
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

  val injector = Guice.createInjector(new InjectionModule)


  // declare singleton services
/*
  object service {
    val fileImport: FileImportService = new FileImportService(app)
    val metadata: AbstractMetadataService = new TikaMetadataService
    val library: LibraryService = new LibraryService(app)
  }
*/
}