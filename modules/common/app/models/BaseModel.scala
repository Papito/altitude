package models

abstract class BaseModel(val id: String = null, val isClean: Boolean = false) {

  def toMap: Map[String, Any] = throw new NotImplementedError
  def fromMap(d: Map[String, Any]): BaseModel = throw new NotImplementedError
}
