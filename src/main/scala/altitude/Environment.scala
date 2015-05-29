package altitude

object Environment extends Enumeration {
  val TEST, PROD, DEV = Value
  var ENV = DEV
}
