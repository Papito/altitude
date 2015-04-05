package global

import altitude.dao._
import altitude.services._
import altitude.util.log
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.ScalaModule
import play.api.{Application, Play}

import scala.collection.mutable

/*
Bridge between a Play! app instance and our app, which is our communication hub
between all the different services.

The Play! global object is a bad candidate since it has to follow the GlobalSettings trait.
 */

object Altitude {
  /*
  Our Altitude instances - multiple ones can exist at once
  (parallel test suites). Ideally, but this probably won't work until
  version 3, where global state will be removed.
  */
  val instances = mutable.HashMap.empty[Int, Altitude]

  // listen to Play! app init and register our own app instance with it
  def register(playApp: Application): Unit = {
    val id: Int = playApp.hashCode()
    val app: Altitude = new Altitude(playApp)
    instances += (id -> app)
  }

  def deregister(playApp: Application): Unit = {
    val id: Int = playApp.hashCode()
    instances.remove(playApp.hashCode())
  }

  def getInstance(playApp: Application = Play.current): Altitude = {
    val id: Int = playApp.hashCode()
    val appInstance: Option[Altitude] = instances.get(id)
    require(appInstance != None)
    appInstance.get
  }
}

class Altitude(val playApp: Application) {
  val id = playApp.hashCode()
  val injector = Guice.createInjector(new InjectionModule)

  /*
  Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = playApp.configuration.getString("datasource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)
      dataSourceType match {
        case "mongo" =>
          bind[AbstractTransactionManager].toInstance(new altitude.services.VoidTransactionManager)
          bind[LibraryDao].toInstance(new altitude.dao.mongo.LibraryDao)
        case "postgres" =>
          bind[AbstractTransactionManager].toInstance(new altitude.services.JdbcTransactionManager)
          bind[LibraryDao].toInstance(new altitude.dao.postgres.LibraryDao)
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

  // declare singleton services
  object service {
    val fileImport: FileImportService = new FileImportService
    val metadata: AbstractMetadataService = new TikaMetadataService
    val library: LibraryService = new LibraryService
  }
}
