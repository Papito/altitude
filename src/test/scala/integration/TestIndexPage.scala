package integration

import altitude.controllers.IndexController
import org.scalatest.FunSuiteLike
import org.scalatra.test.scalatest._

class TestIndexPage extends ScalatraSuite with FunSuiteLike {

  test("Index page") {
    addServlet(classOf[IndexController], "/*")
    get("/"){
      status should equal(200)
    }
  }
}
