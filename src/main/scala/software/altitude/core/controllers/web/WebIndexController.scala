package software.altitude.core.controllers.web

import net.codingwell.scalaguice.InjectorExtensions._
import org.scalatra.scalate.ScalateSupport
import software.altitude.core.AltitudeApplicationContext
import software.altitude.core.controllers.AltitudeStack
import software.altitude.core.dao.FolderDao

class WebIndexController extends AltitudeStack with ScalateSupport with AltitudeApplicationContext {

  val dao: FolderDao = app.injector.instance[FolderDao]
//  before() {
//    requireLogin()
//  }

  get("/") {
    contentType = "text/html"
    mustache("/index")
  }

  get("/read") {
    contentType = "text/html"
    app.txManager.asReadOnly {
      "read"
    }
    "read"
  }

  get("/write") {
    contentType = "text/html"
    "write"
  }
}
