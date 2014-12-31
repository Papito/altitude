package unit

import java.io.File

import models.manager.FileImportAsset
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.JsObject

class ModelTests extends FunSuite {
  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
    model.isClean shouldEqual false

    val j: JsObject = model.toJson
  }
}