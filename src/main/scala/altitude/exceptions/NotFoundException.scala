package altitude.exceptions

class NotFoundException(idType: String, id: String)
  extends Exception(s"Record for $idType:$id not found")

