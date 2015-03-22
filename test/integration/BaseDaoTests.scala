package integration

import altitude.dao.Transaction
import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.language.implicitConversions

class TestModel extends BaseModel

trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    implicit val tx = Some(new Transaction)
    val f: Future[JsValue] = service.add(model)

    whenReady(f) {json =>
      val id = (json \ "id").asOpt[String].getOrElse("")
      id should equal(model.id)

      // retrieve the object
      val f = service.getById(model.id)

      whenReady(f) {json =>
        val id = (json \ "id").asOpt[String].getOrElse("")
        id should be(model.id)

        tx.get.close()
      }
    }
  }
}
