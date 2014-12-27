package unit

import _root_.exceptions.FieldValueException
import org.scalatest._
import models._

import scala.collection.immutable.HashSet

class MetaFieldTests extends FunSuite {
  final val STRING_FIELD = new MetaField[String]("String Field")
  final val LONG_FIELD = new MetaField[Long]("Long Field")
  final val DECIMAL_FIELD = new MetaField[BigDecimal]("Decimal Field")
  final val FIXED_NUMBER_FIELD = new FixedMetaField[Long](
    "Fixed Number Field", isMulti = false, (1L to 10L).toSet)
  final val FIXED_STRING_FIELD = new FixedMetaField[String](
    "Fixed String Field", isMulti = false, HashSet("a", "b", "c"))

  test("string field") {
    new MetaValue("a test value", STRING_FIELD)
  }

  test("number field") {
    new MetaValue(1L, LONG_FIELD)
  }

  test("Decimal field") {
    new MetaValue(BigDecimal(1.0), DECIMAL_FIELD)
  }

  test("fixed number field") {
    new MetaValue(1L, FIXED_NUMBER_FIELD)

    intercept[FieldValueException] {
      new MetaValue(11L, FIXED_NUMBER_FIELD)
    }
  }

  test("fixed string field") {
    new MetaValue("a", FIXED_STRING_FIELD)

    intercept[FieldValueException] {
      new MetaValue("d", FIXED_STRING_FIELD)
    }
  }
}