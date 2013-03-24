package models

import org.joda.time._

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.libs.Codecs
import play.api.Play.current

import utils.Config
import controllers.tools.Paginated

case class Membre(
  id: OptLong,
  login: String,
  email: String,
  password: String,
  uid: String,
  dateInscriptionTs: Long = DateTime.now.getMillis,
  dateConnexionTs: Long = DateTime.now.getMillis,
  estActif: Boolean = true,
  sexe: OptString = None,
  prenom: OptString = None,
  nom: OptString = None,
  dateNaissanceTs: Option[Long] = None,
  ville: OptString = None,
  description: OptString = None,
  photoProfil: OptLong = None,
  securityKey: OptString = None,
  token: OptString = None
) {
  lazy val zodiac = dateNaissance.map(Zodiac.sign(_)).getOrElse("Inconnu")

  lazy val photoProfilPath = photoProfil.flatMap(idPhoto => Photo.findById(idPhoto).map(_.path))

  lazy val dateInscription = new DateTime(dateInscriptionTs)
  lazy val dateConnexion = new DateTime(dateConnexionTs)
  lazy val dateNaissance = dateNaissanceTs.map(new DateMidnight(_))
}

case class Token(token: String) // Add expiration?

object Membre extends MembreDB {

  lazy val secret = Config.getString("wenria.auth.secret")

  def search(search: MembreSearch)(implicit pagination: Paginated) = DB.withSession { implicit s =>
    MembreSearch(search)
      .sortBy(_.id.asc)
      .drop(pagination.offset)
      .take(pagination.limit)
      .list()
  }

  def authenticate(membre: Membre): Option[Token] = {
    membre.id.map { id =>
      val token = java.util.UUID.randomUUID().toString + "/" + Codecs.sha1(membre.login + secret)
      val authMembre = membre.copy(token = Some(token))
      update(id, authMembre)
      Token(token)
    }
  }

  def hashPassword(password: String, key: String) = {
    Codecs.sha1(password + key + secret)
  }

}

abstract class MembreDB extends Table[Membre]("membres") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def login = column[String]("login", O.NotNull, O.DBType("varchar(50)"))
  def email = column[String]("email", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def uid = column[String]("uid", O.NotNull)
  def sexe = column[String]("sexe", O.DBType("varchar(1)"), O.Nullable)
  def prenom = column[String]("prenom", O.Nullable)
  def nom = column[String]("nom", O.Nullable)
  def dateNaissance = column[Long]("dateNaissance", O.Nullable)
  def ville = column[String]("ville", O.DBType("varchar(100)"), O.Nullable)
  def descripton = column[String]("description", O.DBType("varchar(255)"), O.Nullable)
  def photoProfil = column[Long]("photoProfil", O.Nullable)
  def subscribeDate = column[Long]("dateInscription")
  def dateConnexion = column[Long]("dateConnexion")
  def estActif = column[Boolean]("estActif")
  def securityKey = column[String]("securityKey", O.Nullable)
  def token = column[String]("token", O.Nullable)
  // Index
  def idxLogin = index("idx_membre_login", login)
  def idxActif = index("idx_membre_actif", estActif)

  def * = (id.? ~ login ~ email ~ password ~ uid ~ subscribeDate ~ dateConnexion ~
    estActif ~ sexe.? ~ prenom.? ~ nom.? ~ dateNaissance.? ~
    ville.? ~ descripton.? ~ photoProfil.? ~ securityKey.? ~ token.?
    ) <> (Membre.apply _, Membre.unapply _)

  def insert(membre: Membre): Long = DB.withSession { implicit session =>
    (* returning id).insert(membre)
  }

  /** Used for mock login in dev mode */
  def findOneRandom(): Option[Membre] = DB.withSession { implicit session =>
    Query(Membre).take(1).firstOption()
  }

  def update(id: Long, membre: Membre) {
    DB.withSession { implicit session =>
      Query(Membre).where(_.id === id).update(membre.copy(Some(id)))
    }
  }

  def findById(id: Long): Option[Membre] = DB.withSession { implicit session =>
    Query(Membre).filter(t => t.id === id && t.estActif === true).firstOption // Handle active?
  }

  def findByLogin(login: String): Option[Membre] = DB.withSession { implicit session =>
    Query(Membre).filter(t => t.login === login && t.estActif === true).firstOption
  }

  def findByToken(token: String): Option[Membre] = DB.withSession { implicit session =>
    Query(Membre).filter(t => t.token === token && t.estActif === true/* && t.expirationToken > DateTime.now*/).firstOption
  }

  def validLogin(login: String): Boolean = DB.withSession { implicit session =>
    !Query(Membre.filter(_.login === login.trim).exists).first
  }

  def validEmail(email: String): Boolean = DB.withSession { implicit session =>
    !Query(Membre.filter(_.email === email.trim).exists).first
  }

}

object MembreJson {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  // Json writer for a public membre profile
  implicit val membreProfileWriter = (
    (__ \ "id").writeNullable[Long] and
    (__ \ "login").write[String] and
    (__ \ "prenom").write[OptString] and
    (__ \ "nom").write[OptString] and
    (__ \ "sexe").write[OptString] and
    (__ \ "ville").write[OptString] and
    (__ \ "description").write[OptString] and
    (__ \ "dateNaissance").write[Option[LocalDate]] and
    (__ \ "dateInscription").write[LocalDate] and
    (__ \ "signeZodiac").write[String] and
    (__ \ "photoProfil").write[OptString]
  )((m: Membre) =>
    (m.id, m.login, m.prenom, m.nom, m.sexe, m.ville, m.description,
      m.dateNaissanceTs.map(new LocalDate(_)), new LocalDate(m.dateInscription), m.zodiac,
      m.photoProfilPath))

  // Json writer for the private security token
  implicit val tokenWriter = (__ \ "token").write[String].contramap { t: Token => t.token }
}
