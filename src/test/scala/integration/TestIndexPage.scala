package integration

import altitude.controllers.AltitudeServlet
import org.scalatest.FunSuiteLike
import org.scalatra.test.scalatest._

class TestIndexPage extends ScalatraSuite with FunSuiteLike {

  test("Index page") {
    addServlet(classOf[AltitudeServlet], "/*")
    get("/"){
      status should equal(200)
    }
  }
}