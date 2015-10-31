package altitude.exceptions

// FIXME: accept only a message
class NotFoundException(idType: String, id: String)
  extends Exception(s"Record for $idType:$id not found")

