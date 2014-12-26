package unit

import org.scalatest._
import models.{StringMetaValue, StringMetaField, MetaValue}

class MetaFieldTests extends FunSuite {
  final val stringField = new StringMetaField("String Field")

  test("string field") {
    new StringMetaValue("a test value", stringField)
  }
}