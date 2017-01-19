package altitude.util

object Query {
  object ParamType extends Enumeration {
    val EQ, NOT_EQ, GT, LT, GTE, LTE, IN, RANGE, OR, CONTAINS, MATCHES = Value
  }

  case class QueryParam private (values: Set[Any], paramType: ParamType.Value) {
    require(values.nonEmpty)

    // a range requires two values
    if (paramType == ParamType.RANGE || paramType == ParamType.OR)
      require(values.size == 2)

    // overloaded to accept one value
    def this(value: Any, paramType: ParamType.Value) =
      this(Set(value), paramType)

    // overloaded to accept two values
    def this(value1: Any, value2: Any, paramType: ParamType.Value) =
      this(value1 :: value2 :: Nil, paramType)

    // overloaded as simple equals
    def this(value: Any) =
      this(value, ParamType.EQ)
  }

  def EQUALS(value: Any) = new QueryParam(value)

  def NOT_EQUALS(value: Any) = new QueryParam(value, ParamType.NOT_EQ)

  def LT(value: Any) = new QueryParam(value, ParamType.LT)

  def GT(value: Any) = new QueryParam(value, ParamType.GT)

  def LTE(value: Any) = new QueryParam(value, ParamType.LTE)

  def GTE(value: Any) = new QueryParam(value, ParamType.GTE)

  def RANGE(values: List[Any]) = new QueryParam(values, ParamType.RANGE)

  def OR(values: List[Any]) = new QueryParam(values, ParamType.OR)

  def IN(values: List[Any]) = {
    // if only one value given - simplify this to be just an equals
    if (values.size == 1) {
      new QueryParam(values.head)
    } else {
      new QueryParam(values, ParamType.IN)
    }
  }
}

case class Query(params: Map[String, Any] = Map(), rpp: Int = 0, page: Int = 1) {
  if (rpp < 0) throw new IllegalArgumentException(s"Invalid results per page value: $rpp")
  if (page < 1) throw new IllegalArgumentException(s"Invalid page value: $page")
}
