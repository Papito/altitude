package unit

import java.io.File

import altitude.models.{AssetLocation, Asset, BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import altitude.{Const => C}
import play.api.libs.json._

class ModelTests extends FunSuite {
  case class TestModel(id: Option[String] = None) extends BaseModel {
    def toJson: JsObject = coreJsonAttrs
  }

  test("create a model") {
    val model = TestModel()
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}