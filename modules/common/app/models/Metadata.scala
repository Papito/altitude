package models

class Metadata(private val raw: Map[String, String]) extends BaseModel {
  override def toMap = raw
}
