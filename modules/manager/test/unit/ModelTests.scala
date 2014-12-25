package unit

import java.io.File
import models.manager.FileImportAsset
import org.scalatest.Matchers._
import org.scalatest._

class ModelTests extends FunSuite {
  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
    model.id shouldBe null
    model.isClean shouldEqual false

    val d: Map[String, Any] = model.toMap
    d should contain key "id"
  }
}