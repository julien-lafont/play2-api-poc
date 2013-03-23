package models

import org.joda.time.{DateMidnight, LocalDate, LocalDateTime, DateTime}

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.libs.Codecs

import play.api.libs.json._
import play.api.libs.functional.syntax._
import utils.Config
import controllers.tools.Paginated

case class Member(
  id: OptLong,
  login: String,
  email: String,
  password: String,
  uid: String,
  subscribeDate: Long = DateTime.now.getMillis,
  lastConnectDate: Long = DateTime.now.getMillis,
  isActive: Boolean = true,
  sex: OptString = None,
  firstName: OptString = None,
  lastName: OptString = None,
  birthDate: Option[Long] = None,
  city: OptString = None,
  description: OptString = None,
  mainPicture: OptLong = None,
  securityKey: OptString = None,
  token: OptString = None
) {
  lazy val zodiac = birthDateTime.map(Zodiac.sign(_)).getOrElse("Inconnu")

  lazy val mainPicturePath = mainPicture.flatMap(idPhoto => Photo.findById(idPhoto).map(_.path))

  lazy val subscribeDateTime = new DateTime(subscribeDate)
  lazy val lastConnectDateTime = new DateTime(lastConnectDate)
  lazy val birthDateTime = birthDate.map(new DateMidnight(_))
}

case class Token(token: String) // Add expiration?

object Member extends MemberDB {

  lazy val secret = Config.getString("wenria.auth.secret")

  def search(search: MemberSearch)(implicit pagination: Paginated) = DB.withSession { implicit s =>
    MemberSearch(search)
      .sortBy(_.id.asc)
      .drop(pagination.offset)
      .take(pagination.limit)
      .list()
  }

  def authenticate(member: Member): Option[Token] = {
    member.id.map { id =>
      val token = java.util.UUID.randomUUID().toString + "/" + Codecs.sha1(member.login + secret)
      val authMember = member.copy(token = Some(token))
      update(id, authMember)
      Token(token)
    }
  }

  def hashPassword(password: String, key: String) = {
    Codecs.sha1(password + key + secret)
  }

}

abstract class MemberDB extends Table[Member]("members") with CustomTypes {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def login = column[String]("login", O.NotNull, O.DBType("varchar(50)"))
  def email = column[String]("email", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def uid = column[String]("uid", O.NotNull)
  def sex = column[String]("sex", O.DBType("varchar(1)"), O.Nullable)
  def firstName = column[String]("firstName", O.Nullable)
  def lastName = column[String]("lastName", O.Nullable)
  def birthDate = column[Long]("birthDate", O.Nullable)
  def city = column[String]("city", O.DBType("varchar(100)"), O.Nullable)
  def descripton = column[String]("description", O.DBType("varchar(255)"), O.Nullable)
  def mainPicture = column[Long]("mainPicture", O.Nullable)
  def subscribeDate = column[Long]("suscribeDate")
  def lastConnectDate = column[Long]("lastConnectDate")
  def isActive = column[Boolean]("isActive")
  def securityKey = column[String]("securityKey", O.Nullable)
  def token = column[String]("token", O.Nullable)

  def * = (id.? ~ login ~ email ~ password ~ uid ~ subscribeDate ~ lastConnectDate ~
    isActive ~ sex.? ~ firstName.? ~ lastName.? ~ birthDate.? ~
    city.? ~ descripton.? ~ mainPicture.? ~ securityKey.? ~ token.?
    ) <> (Member.apply _, Member.unapply _)

  def insert(member: Member): Long = DB.withSession { implicit session =>
    (* returning id).insert(member)
  }

  /** Used for mock login in dev mode */
  def findOneRandom(): Option[Member] = DB.withSession { implicit session =>
    Query(Member).take(1).firstOption()
  }

  def update(id: Long, member: Member) {
    DB.withSession { implicit session =>
      Query(Member).where(_.id === id).update(member.copy(Some(id)))
    }
  }

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

}

case class MemberSearch(
  login: OptString = None,
  firstName: OptString = None,
  lastName: OptString = None,
  sex: OptString = None,
  city: OptString = None,
  age: OptInt = None
)

object MemberSearch {

  def apply(filters: MemberSearch) = {

    val slickFilters = List[Condition[_]](
      Condition[String](filters.firstName, (t, v) => t.firstName.toLowerCase like '%' + v.toLowerCase + '%'),
      Condition[String](filters.lastName, (t, v) => t.lastName.toLowerCase like  '%' + v.toLowerCase + '%'),
      Condition[String](filters.sex, (t, v) => t.sex === v),
      Condition[String](filters.city, (t, v) => t.city === v),
      Condition[Int](filters.age, (t, v) =>
        t.birthDate >= DateTime.now.minusYears(v+1).getMillis &&
        t.birthDate <= DateTime.now.minusYears(v).getMillis)
    )

    val baseQuery = Query(Member).filter(_.isActive === true)
    slickFilters
      .filter(_.value.isDefined)
      .foldLeft(baseQuery)((query, condition) => query.filter(condition.toFilter))
  }

  private case class Condition[T](val value: Option[T], val condition: (Member.type, T) => Column[Boolean]) {
    def toFilter(t: Member.type): Column[Boolean] = condition(t, value.get)
  }
}

object MemberJson {

  // Json writer for a public member profile
  implicit val membreProfileWriter = (
    (__ \ "id").writeNullable[Long] and
    (__ \ "login").write[String] and
    (__ \ "firstName").write[OptString] and
    (__ \ "lastName").write[OptString] and
    (__ \ "sex").write[OptString] and
    (__ \ "city").write[OptString] and
    (__ \ "description").write[OptString] and
    (__ \ "birthDate").write[Option[LocalDate]] and
    (__ \ "subscribeDate").write[LocalDate] and
    (__ \ "zodiac").write[String] and
    (__ \ "mainPicture").write[OptString]
  )((m: Member) =>
    (m.id, m.login, m.firstName, m.lastName, m.sex, m.city, m.description,
      m.birthDate.map(new LocalDate(_)), new LocalDate(m.subscribeDate), m.zodiac,
      m.mainPicturePath))

  // Json writer for the private security token
  implicit val tokenWriter = (__ \ "token").write[String].contramap { t: Token => t.token }
}