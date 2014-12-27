package unit

import _root_.exceptions.FieldValueException
import org.scalatest._
import models._

import scala.collection.immutable.HashSet

class MetaFieldTests extends FunSuite {
  final val stringField = new StringMetaField("String Field")
  final val numberField = new NumberMetaField("Number Field")
  final val fixedNumberField = new FixedNumberMetaField(
    "Fixed Number Field", isMulti = false, (1L to 10L).toSet)
  final val fixedStringField = new FixedStringMetaField(
    "Fixed String Field", isMulti = false, HashSet("a", "b", "c"))

  test("string field") {
    new StringMetaValue("a test value", stringField)
  }

  test("number field") {
    new NumberMetaValue(1, numberField)
  }

  test("fixed number field") {
    new NumberMetaValue(1, fixedNumberField)

    intercept[FieldValueException] {
      new NumberMetaValue(11, fixedNumberField)
    }
  }

  test("fixed string field") {
    new StringMetaValue("a", fixedStringField)

    intercept[FieldValueException] {
      new StringMetaValue("d", fixedStringField)
    }
  }
}