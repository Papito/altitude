package altitude.models

/*
Trait to use for submodels that are not stored in databases as a separate
documents, hence requiring no ID.
 */
trait NoId {
  val id: Option[String] = None
}
