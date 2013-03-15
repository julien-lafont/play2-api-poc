package models

import org.joda.time.DateTime

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.libs.Crypto

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Member(
  id: Option[Long],
  login: String,
  email: String,
  password: String,
  uid: String,
  subscribeDate: DateTime = DateTime.now(),
  lastConnectDate: DateTime = DateTime.now(),
  isActive: Boolean = true,
  sex: Option[String] = None,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  birthDate: Option[DateTime] = None,
  city: Option[String] = None,
  securityKey: Option[String] = None,
  token: Option[String] = None,
  expirationToken: Option[DateTime] = None
) {
  lazy val zodiac = birthDate.map(Zodiac.sign(_)).getOrElse("Inconnu")
}

object Member extends Table[Member]("members") with CustomTypes {

  lazy val secret = play.api.Play.current.configuration.getString("application.auth.secret").getOrElse(throw new Exception("No application.auth.secret defined"))

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

  def * = (id.? ~ login ~ email ~ password ~ uid ~ subscribeDate ~ lastConnectDate ~
    isActive ~ sex.? ~ firstName.? ~ lastName.? ~ birthDate.? ~
    city.? ~ securityKey.? ~ token.? ~ expirationToken.?
  ) <> (Member.apply _, Member.unapply _)

  def findById(id: Long): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.id === id && t.isActive === true).firstOption // Handle active?
  }

  def findByLogin(login: String): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.login === login && t.isActive === true).firstOption
  }

  def findByToken(token: String): Option[Member] = DB.withSession { implicit session =>
    Query(Member).filter(t => t.token === token && t.isActive === true && t.expirationToken > DateTime.now).firstOption
  }

  def validLogin(login: String): Boolean = DB.withSession { implicit session =>
    !Query(Member.filter(_.login === login.trim).exists).first
  }

  def validEmail(email: String): Boolean = DB.withSession { implicit session =>
    !Query(Member.filter(_.email === email.trim).exists).first
  }

  //--------------------------

  def insert(member: Member): Long = DB.withSession { implicit session =>
    (* returning id).insert(member)
  }

  def authenticate(member: Member): Option[JsValue] = {
    member.id.map { id =>
      val token = java.util.UUID.randomUUID().toString+"/"+Crypto.sign(member.login, secret.getBytes)
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

object Zodiac {
  private val signs = Map(
    "0120" -> "Capricorne",
    "0218" -> "Verseau",
    "0320" -> "Poisson",
    "0420" -> "Bélier",
    "0521" -> "Taureau",
    "0621" -> "Gémeaux",
    "0722" -> "Cancer",
    "0822" -> "Lion",
    "0922" -> "Vierge",
    "1022" -> "Balance",
    "1122" -> "Scorpion",
    "1221" -> "Sagittaire",
    "1300" -> "Capricorne")

  def sign(date: DateTime) = {
    val dateStr = "%02d%02d".format(date.getMonthOfYear, date.getDayOfMonth)
    signs.dropWhile(e => e._1 < dateStr).head._2
  }

}

object MemberJson {
  implicit val memberProfile = (
    (__ \ "id").writeNullable[Long] and
    (__ \ "login").write[String] and
    (__ \ "firstName").write[Option[String]] and
    (__ \ "lastName").write[Option[String]] and
    (__ \ "sex").write[Option[String]] and
    (__ \ "birthDate").write[Option[DateTime]] and
    (__ \ "subscribeDate").write[DateTime] and
    (__ \ "zodiac").write[String]
  )((m: Member) => (m.id, m.login, m.firstName, m.lastName, m.sex, m.birthDate, m.subscribeDate, m.zodiac))

}