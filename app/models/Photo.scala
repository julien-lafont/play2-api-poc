package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import play.api.Play.current
import java.util.{Date, UUID}
import net.coobird.thumbnailator.Thumbnails
import scala.util.{Try, Success, Failure}
import play.api.Logger
import utils.FileUtil._
import utils.Config
import scalax.file.Path

case class Photo(
  id: Option[Long] = None,
  path: String,
  contentType: String,
  alert: Int = 0,
  display: Boolean = true)

object Photo extends PhotoDB {

  // Config
  val uploadBaseDir = Config.getString("wenria.upload.dir")
  val dimension = Config.getInt("wenria.picture.baseDimension")
  val maxSize = Config.getInt("wenria.picture.maxSize")
  val extensions = Config.getStringList("wenria.picture.extensions")

  def uploadAndSaveFile(file: UploadFile, folder: String): Either[String, Long] = {
    for {
      _         <- checkFile(file, extensions, 5).right
      filename  <- uploadPicture(file, uploadBaseDir + folder, dimension).right
      id        <- insertInDB(filename, file.contentType).right
    } yield (id)
  }

  private def uploadPicture(file: UploadFile, folder: String, dimension: Int): Either[String, String] = {
    Try {
      val extension = Path(file.filename).extension.get
      val destFilename = folder + new Date().getTime + '-' + UUID.randomUUID().toString.substring(0,8) + '.' + extension
      Thumbnails.of(file.ref.file).size(dimension, dimension).toFile(destFilename)
      destFilename
    } match {
      case Success(filename) => Right(filename)
      case Failure(exception) => {
        Logger.error("Cannot create thumbnail", exception)
        Left("Cannot create thumbnail")
      }
    }
  }

  private def insertInDB(filename: String, contentType: Option[String]): Either[String, Long] = {
    Try {
      insert(Photo(None, filename, contentType.getOrElse("")))
    } match {
      case Success(id) => Right(id)
      case Failure(exception) => {
        Logger.error("Cannot save photo in DB", exception)
        Left("Cannot save photo in DB")
      }
    }
  }

}

abstract class PhotoDB extends Table[Photo]("photos") with CustomTypes {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("login", O.NotNull)
  def contentType = column[String]("contentType", O.NotNull)
  def alert = column[Int]("email", O.NotNull)
  def display = column[Boolean]("password", O.NotNull)

  def * = (id.? ~ path ~ contentType ~ alert ~ display) <> (Photo.apply _, Photo.unapply _)

  def insert(photo: Photo): Long = DB.withSession { implicit session =>
    (* returning id).insert(photo)
  }

  def findByPath(path: String): Option[Photo] = DB.withSession { implicit session =>
    Query(Photo).filter(p => p.path === path && p.display === true).firstOption
  }

  def findById(id: Long): Option[Photo] = DB.withSession { implicit session =>
    Query(Photo).filter(p => p.id === id && p.display === true).firstOption
  }

}
