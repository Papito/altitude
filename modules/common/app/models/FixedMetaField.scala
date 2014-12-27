package models

trait FixedMetaField[T] extends AbstractMetaField[T] {
  val allowed: Set[T]
}
