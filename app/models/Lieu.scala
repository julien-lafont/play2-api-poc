package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import controllers.tools.Paginated
import scala.slick.jdbc.{StaticQuery => Q}

case class Lieu(
  id: OptLong = None,
  nom: String,
  adresse: String,
  codePostal: String,
  ville: String,
  latitude: Double,
  longitude: Double,
  nbFans: Int = 0,
  googleId: Option[String] = None,
  googleCid: Option[String] = None,
  googleUrl: Option[String] = None
)

case class LieuDistance(lieu: Lieu, distance: Double)

object Lieu extends LieuDB {

  def searchLieuxProchesPosition(latitude: Double, longitude: Double)(implicit pagination: Paginated) =
    DB.withSession { implicit session =>
      val mapIdDistance = findIdLieuxProchePosition(latitude, longitude, None, pagination.offset, pagination.limit)
      Query(Lieu).filter(l => l.id inSet (mapIdDistance.keys))
        .list()
        .map(lieu => LieuDistance(lieu, mapIdDistance(lieu.id.get)))
        .sortBy(_.distance)
  }
}

abstract class LieuDB extends Table[Lieu]("lieux") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def nom = column[String]("nom", O.NotNull)
  def adresse = column[String]("adresse", O.NotNull, O.DBType("text"))
  def codePostal = column[String]("code_postal", O.NotNull)
  def ville = column[String]("ville", O.NotNull)
  def latitude = column[Double]("latitude", O.NotNull)
  def longitude = column[Double]("longitude", O.NotNull)
  def nbFans = column[Int]("nbFans", O.NotNull)
  def googleId = column[String]("google_id", O.Nullable)
  def googleCid = column[String]("google_cid", O.Nullable)
  def googleUrl = column[String]("google_url", O.Nullable)

  def * = (id.? ~ nom ~ adresse ~ codePostal ~ ville ~ latitude ~ longitude ~
    nbFans ~ googleId.? ~ googleCid.? ~ googleUrl.?) <> (Lieu.apply _, Lieu.unapply _)

  def insert(lieu: Lieu) = DB.withSession { implicit session =>
    (* returning id).insert(lieu)
  }

  def calculerDistance(lt1: Column[Double], lg1: Column[Double], lt2: Column[Double], lg2: Column[Double]) =
    SimpleFunction("calculerDistanceKm")(TypeMapper.DoubleTypeMapper)(Seq(lt1, lg1, lt2, lg2))

  /**
   * Retourne les lieux les plus proches de ma position
   * @param lt Latitude de ma position
   * @param lg Longitude de ma position
   * @param first Offset du premier résultat à retourner
   * @param nb Nombre de résultats à retourner
   * @param rayonMax Limiter la recherche à X km autours de ma position
   */
  def findIdLieuxProchePosition(lt: Double, lg: Double, rayonMax: Option[Double] = None, first: Int = 0, nb: Int = 20): Map[Long, Double] =
    DB.withSession { implicit s =>
      rayonMax match {
        case Some(rayon) =>
          Q.query[(Double, Double, Int, Int, Double), (Long, Double)]("call listerLieuxProchesPositionDansRayon(?, ?, ?, ?, ?)")
            .list(lt, lg, first, nb, rayon).map(r => r._1 -> r._2).toMap
        case None =>
          Q.query[(Double, Double, Int, Int), (Long, Double)]("call listerLieuxProchesPosition(?, ?, ?, ?)")
            .list(lt, lg, first, nb).map(r => r._1 -> r._2).toMap
      }
  }
}

// TODO
/*case class LieuSearch(
  nom: Option[String],
  rayon: Option[Int],
  ville: Option[String])*/

object LieuJson {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val lieuWriter: Writes[Lieu] = Json.writes[Lieu]
  implicit val lieuDistWritter: Writes[LieuDistance] = Json.writes[LieuDistance]

}