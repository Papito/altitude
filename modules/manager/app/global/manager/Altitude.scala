package global.manager

import altitude.common.dao.UtilitiesDao
import altitude.manager.dao.LibraryDao
import altitude.manager.services._
import altitude.{Const => C}
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.ScalaModule
import play.api.{Application, Play}
import altitude.util.log

import scala.collection.mutable

/*
Bridge between a Play! app instance and our app, which is our communication hub
between all the different services.

The Play! global object is a bad candidate since it has to follow the GlobalSettings trait.
 */

object Altitude {
  /*
  Our Altitude instances - multiple ones can exist at once
  (parallel test suites)

  NOTE: parallel suite execution did not work at the time (Jan 2015) - the test
  system seemed to shut down one test app cold before tests could finish.
  */
  val instances = mutable.HashMap.empty[Int, Altitude]

  // listen to Play! app init and register our own app instance with it
  def register(playApp: Application): Unit = {
    val id: Int = playApp.hashCode()
    val app: Altitude = new Altitude(playApp)
    instances += (id -> app)
    log.info("Registering app $app with Play! id: $id", Map("id" -> id, "app" -> app), C.tag.APP)
    log.info("We have $n applications running", Map("n" -> instances.size), C.tag.APP)
  }

  def deregister(playApp: Application): Unit = {
    val id: Int = playApp.hashCode()
    log.info("De-registering app $app with Play! id: $id", Map("app" -> instances.get(id), "id" -> id), C.tag.APP)
    instances.remove(playApp.hashCode())
    log.info("We have $n applications running", Map("n" -> instances.size), C.tag.APP)
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
  log.trace("Initializing app for Play! id: $id", Map("id" -> id), C.tag.APP)

  val injector = Guice.createInjector(new InjectionModule)

  /*
  Inject dependencies
   */
  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = playApp.configuration.getString("datasource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)
      dataSourceType match {
        case "mongo" => {
          bind[LibraryDao].toInstance(new altitude.manager.dao.mongo.LibraryDao)
          bind[UtilitiesDao].toInstance(new altitude.common.dao.mongo.UtilitiesDao)
        }
        case "postgres" => {
          bind[LibraryDao].toInstance(new altitude.manager.dao.postgres.LibraryDao)
          bind[UtilitiesDao].toInstance(new altitude.common.dao.postgres.UtilitiesDao)
        }
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType)
      }
    }
  }

  object service {
    val fileImport: FileImportService = new FileImportService
    val metadata: AbstractMetadataService = new TikaMetadataService
    val library: LibraryService = new LibraryService
    val dbUtilities: DbUtilitiesService = new DbUtilitiesService
  }
}
