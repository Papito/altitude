package models

import reactivemongo.bson.BSONObjectID

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel[BSONObjectID]