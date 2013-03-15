package models

import slick.lifted.MappedTypeMapper
import org.joda.time.DateTime

trait CustomTypes {

  implicit val dateTimeTypeMapper = MappedTypeMapper.base[DateTime, java.sql.Date](
    date => new java.sql.Date(date.toDate.getTime),
    timestamp => new DateTime(timestamp)
  )
}
