package models

class Metadata(private val data: Map[String, String]) extends BaseModel {
  override def toMap = data
}
