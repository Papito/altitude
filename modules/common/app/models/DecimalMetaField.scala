package models

class DecimalMetaField(name: String)
  extends AbstractMetaField[BigDecimal](name, isMulti=false)
