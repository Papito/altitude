package altitude.service

import java.io.InputStream

import altitude.models.{AssetType, FileImportAsset, AssetType$}
import play.api.libs.json.JsValue

abstract class AbstractMetadataService {

  /*
  This structure defines how we construct the final metadata object.
  Metadata can have a lot of fields that are related or identical -
  this sets the priority for them, so we know which field to take
  from source metadata, depending on availability.
   */
  val FIELD_BIBLE: Map[String, List[String]] = Map(
    /*
        FINAL_FIELD_1 -> [
          POSSIBLE_FIELD_1_PRIORITY_1,
          POSSIBLE_FIELD_1_PRIORITY_2]
        FINAL_FIELD_2 -> [
          POSSIBLE_FIELD_2_PRIORITY_1,
          POSSIBLE_FIELD_2_PRIORITY_2,
          POSSIBLE_FIELD_2_PRIORITY_3]
     */
    "X Resolution" -> List(),
    "Y Resolution" -> List(),
    "tiff:Orientation" -> List(),

    "Image Width" -> List(
      "Image Width",
      "Exif Image Width",
      "tiff:ImageWidth"),

    "Image Height" -> List(
      "Image Height",
      "Exif Image Height",
      "tiff:ImageLength"),

    "tiff:Make" -> List(
      "tiff:Make",
      "Make"
    ),

    "tiff:Model" -> List(
      "tiff:Model",
      "Model"
    ),

    "tiff:Software" -> List(),
    "Lens Information" -> List(
      "Lens Information",
      "Lens",
      "Lens Model"
    ),

    "exif:IsoSpeedRatings" -> List(
      "exif:IsoSpeedRatings",
      "Shutter Speed Value",
      "ISO Speed Ratings"),

    "exif:FocalLength" -> List(
      "exif:FocalLength",
      "Focal Length",
      "Aperture Value"
    ),

    "exif:FNumber" -> List(
      "exif:FNumber",
      "F-Number"),

    "exif:ExposureTime" -> List(
      "exif:ExposureTime",
      "Exposure Time"
    ),

    "exif:Flash" -> List(
      "exif:Flash",
      "Flash"
    ),

    "Exposure Mode" -> List(),
    "Exposure Program" -> List(),
    "JPEG Quality" -> List(),

    "User Comment" -> List(
      "User Comment",
      "w:comments",
      "JPEG Comment",
      "Comments",
      "Comment")
  )


  def extract(importAsset: FileImportAsset, assetType: AssetType, asRaw: Boolean): JsValue
  def detectAssetTypeFromStream(is: InputStream): AssetType
}
