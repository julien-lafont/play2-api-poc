package models

import org.joda.time.DateTime

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.libs.Crypto

import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._
import play.api.Logger

case class Member(
  id: OptLong,
  login: String,
  email: String,
  password: String,
  uid: String,
  subscribeDate: DateTime = DateTime.now(),
  lastConnectDate: DateTime = DateTime.now(),
  isActive: Boolean = true,
  sex: OptString = None,
  firstName: OptString = None,
  lastName: OptString = None,
  birthDate: Option[DateTime] = None,
  city: OptString = None,
  securityKey: OptString = None,
  token: OptString = None,
  expirationToken: Option[DateTime] = None
) {
  lazy val zodiac = birthDate.map(Zodiac.sign(_)).getOrElse("Inconnu")
}

object Member extends Table[Member]("members") with CustomTypes {

  lazy val secret = play.api.Play.current.configuration.getString("application.auth.secret").getOrElse(throw new Exception("No application.auth.secret defined"))
  val DEFAULT_PAGINATION = 20

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def login = column[String]("login", O.NotNull, O.DBType("varchar(50)"))
  def email = column[String]("email", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def uid = column[String]("uid", O.NotNull)
  def sex = column[String]("sex", O.DBType("varchar(1)"), O.Nullable)
  def firstName = column[String]("firstName", O.Nullable)
  def lastName = column[String]("lastName", O.Nullable)
  def birthDate = column[DateTime]("birthDate", O.Nullable)
  def city = column[String]("city", O.DBType("varchar(100)"), O.Nullable)
  def subscribeDate = column[DateTime]("suscribeDate")
  def lastConnectDate = column[DateTime]("lastConnectDate")
  def isActive = column[Boolean]("isActive")
  def securityKey = column[String]("securityKey", O.Nullable)
  def token = column[String]("token", O.Nullable)
  def expirationToken = column[DateTime]("expirationToken", O.Nullable)

  def findById(id: Long): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.id === id && t.isActive === true).firstOption // Handle active?
  }

  def findByLogin(login: String): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.login === login && t.isActive === true).firstOption
  }

  def findByToken(token: String): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.token === token && t.isActive === true/* && t.expirationToken > DateTime.now*/).firstOption
  }

  def validLogin(login: String): Boolean = DB.withSession { implicit session =>
    !Query(Member.filter(_.login === login.trim).exists).first
  }

  def validEmail(email: String): Boolean = DB.withSession { implicit session =>
    !Query(Member.filter(_.email === email.trim).exists).first
  }

  def search( login: OptString = None,
              firstName: OptString = None,
              lastName: OptString = None,
              sex: OptString = None,
              city: OptString = None,
              age: OptInt = None,
              page: OptInt = None,
              per_page: OptInt = None) = DB.withSession { implicit s =>

    var q = Query(Member).filter(_.isActive === true) // Not found a better way do do this...
    if (login.isDefined)      q = q.filter(_.login like '%' + login.get + '%')
    if (firstName.isDefined)  q = q.filter(_.firstName like '%' + firstName.get + '%')
    if (lastName.isDefined)   q = q.filter(_.lastName like '%' + lastName.get + '%')
    if (sex.isDefined)        q = q.filter(_.sex === sex.get)
    if (city.isDefined)       q = q.filter(_.city === city.get)
    if (age.isDefined)        q = q.filter(m => m.birthDate > DateTime.now().minusYears(age.get+1) &&
                                                m.birthDate < DateTime.now().minusYears(age.get))

    q = q.drop((page.getOrElse(1) - 1) * per_page.getOrElse(DEFAULT_PAGINATION))
    q = q.take(per_page.getOrElse(DEFAULT_PAGINATION))

    Logger.debug("SEARCH : "+q._selectStatement)

    q.list()
  }

  //--------- CRUD Operations ----------------

  def * = (id.? ~ login ~ email ~ password ~ uid ~ subscribeDate ~ lastConnectDate ~
    isActive ~ sex.? ~ firstName.? ~ lastName.? ~ birthDate.? ~
    city.? ~ securityKey.? ~ token.? ~ expirationToken.?
    ) <> (Member.apply _, Member.unapply _)

  def insert(member: Member): Long = DB.withSession { implicit session =>
    (* returning id).insert(member)
  }

  def authenticate(member: Member): Option[JsValue] = {
    member.id.map { id =>
      val token = java.util.UUID.randomUUID().toString + "/" + Crypto.sign(member.login, secret.getBytes)
      val authMember = member.copy(expirationToken = Some(DateTime.now.plusHours(1)), token = Some(token))
      update(id, authMember)
      Json.obj("token" -> token, "expiration" -> authMember.expirationToken.get)
    }
  }

  def update(id: Long, member: Member) {
    DB.withSession { implicit session =>
      Query(Member).where(_.id === id).update(member.copy(Some(id)))
    }
  }

  def hashPassword(password: String, key: String) = {
    // TODO
    password+key
  }

}

object MemberJson {
  implicit val memberProfile = (
    (__ \ "id").writeNullable[Long] and
    (__ \ "login").write[String] and
    (__ \ "firstName").write[OptString] and
    (__ \ "lastName").write[OptString] and
    (__ \ "sex").write[OptString] and
    (__ \ "city").write[OptString] and
    (__ \ "birthDate").write[Option[DateTime]] and
    (__ \ "subscribeDate").write[DateTime] and
    (__ \ "zodiac").write[String]
  )((m: Member) => (m.id, m.login, m.firstName, m.lastName, m.sex, m.city, m.birthDate, m.subscribeDate, m.zodiac))

}