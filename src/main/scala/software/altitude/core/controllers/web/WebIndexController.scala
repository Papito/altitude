package software.altitude.core.controllers.web

import net.codingwell.scalaguice.InjectorExtensions._
import org.apache.commons.dbutils.QueryRunner
import org.scalatra.scalate.ScalateSupport
import software.altitude.core.AltitudeServletContext
import software.altitude.core.RequestContext
import software.altitude.core.controllers.AltitudeStack
import software.altitude.core.dao.FolderDao

class WebIndexController extends AltitudeStack with ScalateSupport with AltitudeServletContext {

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
    val dao: FolderDao = app.injector.instance[FolderDao]

    app.txManager.asReadOnly {
      app.txManager.asReadOnly {
        val rec = dao.oneBySqlQuery("SELECT * FROM folder LIMIT 1", List())
        println(rec)
      }
      val rec = dao.oneBySqlQuery("SELECT * FROM folder LIMIT 1", List())
      println(rec)
    }
    "read"
  }

  get("/write") {
    contentType = "text/html"
    app.txManager.withTransaction {
      app.txManager.withTransaction {
        app.txManager.withTransaction {
          val runner: QueryRunner = new QueryRunner()
          runner.execute(RequestContext.conn.value.get, "UPDATE system SET initialized = ? WHERE id = ?", false, 1)
        }
      }
    }
    "write"
  }
}
