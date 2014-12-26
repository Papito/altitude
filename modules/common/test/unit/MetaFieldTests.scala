package unit

import org.scalatest._
import models.{StringMetaField, MetaValue}

class MetaFieldTests extends FunSuite {
  test("string field") {
    val strVal = new MetaValue[String]("a test value")
    val strMetaField = new StringMetaField("field_name")
    strMetaField.check(strVal)
  }
}