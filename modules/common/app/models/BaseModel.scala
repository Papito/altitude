package models

abstract class BaseModel(val id: String = null, val isClean: Boolean = false) {

  def toDict: Map[String, Any] = throw new NotImplementedError
  def fromDict(d: Map[String, Any]): BaseModel = throw new NotImplementedError
}
