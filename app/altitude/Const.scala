package altitude

import play.api.libs.json.Json

object Const {
   object tag {
     val APP     = "APP"
     val API     = "API"
     val WEB     = "WEB"
     val SERVICE = "SERVICE"
     val DB = "DB"
   }

   object Common {
     val ID = "id"
   }

  object Db {
    val EMPTY: Int = Json.obj().hashCode()

  }
 }
