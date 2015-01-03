package global

import com.google.inject.{AbstractModule, Guice}
import dao.manager.LibraryDao
import net.codingwell.scalaguice.ScalaModule
import play.api.Application
import play.api.Play
import service.manager.{LibraryService, TikaMetadataService, AbstractMetadataService, FileImportService}
import util.log
import scala.collection.mutable
import altitude.{Const => C}

object App {
  val instances = mutable.HashMap.empty[Int, App]

  def register(playApp: Application): Unit = {
    val id: Int = playApp.hashCode()
    val app: App = new App(playApp)
    instances += (id -> app)
    log.info("Registering app $app with Play! id: $id", Map("id" -> id, "app" -> app.hashCode()), C.tag.APP)

    log.info("$n APPS", Map("n" -> instances.size), C.tag.APP)
    for (app <- instances.values) {
      log.info("$app: fileImport -> $fileImport", Map("app" -> app.hashCode(), "fileImport" -> app.service.fileImport), C.tag.APP)
    }
  }

  def getInstance(playApp: Application = Play.current): App = {
    val id: Int = playApp.hashCode()
    val appInstance: Option[App] = instances.get(id)
    require(appInstance != None)
    appInstance.get
  }
}

class App(val playApp: Application) {
  val id = playApp.hashCode()
  log.info("Initializing app for play id: $id", Map("id" -> id), C.tag.APP)

  val injector = Guice.createInjector(new InjectionModule)

  class InjectionModule extends AbstractModule with ScalaModule  {
    override def configure(): Unit = {
      val dataSourceType = playApp.configuration.getString("datasource").getOrElse("")
      log.info("Datasource type: $source", Map("source" -> dataSourceType), C.tag.APP)
      dataSourceType match {
        case "mongo" => bind[LibraryDao].toInstance(new dao.manager.mongo.LibraryDao)
        case "postgres" => bind[LibraryDao].toInstance(new dao.manager.postgres.LibraryDao)
        case _ => throw new IllegalArgumentException("Do not know of datasource: " + dataSourceType);
      }
    }
  }

  object service {
    val fileImport: FileImportService = new FileImportService
    val metadata: AbstractMetadataService = new TikaMetadataService
    val library: LibraryService = new LibraryService
  }
}
