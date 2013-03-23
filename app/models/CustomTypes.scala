package models

import slick.lifted.MappedTypeMapper
import org.joda.time.DateTime
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile

trait CustomTypes {

  // Converters for Slick DB
  /*implicit val dateTimeTypeMapper = MappedTypeMapper.base[DateTime, java.sql.Date](
    date => new java.sql.Date(date.toDate.getTime),
    timestamp => new DateTime(timestamp)
  )*/

  type UploadFile = MultipartFormData.FilePart[TemporaryFile]

}
