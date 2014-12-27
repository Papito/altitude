package models

trait FixedField[T] extends AbstractMetaField[T] {
  val allowed: Set[T]
}
