package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools._
import models.{MemberSearch, Photo, Member}
import models.MemberJson._

object Members extends Security with Api {

  val paramLogin = Param[String]("login", MinLength(4), Callback(Member.validLogin, "Login already registered"))
  val paramEmail = Param[String]("email", Email(), Callback(Member.validEmail, "Email already registered"))
  val paramPassword = Param[String]("password")

  /**
   * Check if required parameters for a subscription are valid
   */
  def checkSubscription = Action { implicit request =>
    ApiParameters(paramLogin, paramEmail, POST) { (login, email) => ok }
  }

  /**
   * Register a new user in the app
   */
  def subscribe = Action { implicit request =>
    ApiParameters(paramLogin, paramEmail, paramPassword) { (login, email, password) =>
      val uid = java.util.UUID.randomUUID().toString
      val member = Member(None, login, email, Member.hashPassword(password, uid), uid)
      val insertedId = Member.insert(member)
      ok(Json.obj("id" -> insertedId))
    }
  }

  /**
   * Authenticate a user with a login and a password (hashed).
   * @return (token, expiration in ms) in valid, else Forbidden
   */
  def authenticate = Action { implicit request =>
    ApiParameters(Param[String]("login"), Param[String]("password")) { (login, password) =>
      Member.findByLogin(login) match {
        case Some(member) if Member.hashPassword(password, member.uid) == member.password =>
          Member.authenticate(member)
            .map(token => ok(token))
            .getOrElse(forbidden)
        case None => badrequest()
      }
    }
  }

  /**
   * Display member profile
   */
  def profile(id: Long) = Action { implicit request =>
    Member.findById(id)
      .map(member => ok(member))
      .getOrElse(notfound)
  }

  /**
   * Display my own profile
   */
  def myProfile = Authenticated { implicit request =>
    profile(me.id.get)(request)
  }

  /**
   * Update a member profile. Each field is optional
   */
  def updateMyProfil = Authenticated { implicit request =>
      Member.update(me.id.get, me.copy(
        firstName   = post("firstName").orElse(me.firstName),
        lastName    = post("lastName").orElse(me.lastName),
        birthDate   = postDateTime("birthDate").map(_.getMillis).orElse(me.birthDate),
        sex         = post("sex").flatMap(sex => if (sex=="h" || sex=="f") Some(sex) else None).orElse(me.sex),
        city        = post("city").orElse(me.city),
        description = post("description").orElse(me.description)
      ))
      myProfile(request)
  }

  /**
   * Search a member from multiple criterias. Each field is optional
   */
  def search = Authenticated { implicit request =>
    val filter = MemberSearch(get("login"), get("firstname"), get("lastname"), get("sex"), get("city"), getInt("age"))
    ok(Member.search(filter))
  }

  /**
   * Upload and save a new main picture
   */
  def updateMainPicture = Authenticated(parse.multipartFormData) { req =>
    req.body.file("picture").map { picture =>
      Photo.uploadAndSaveFile(picture, "picture/" + req.me.id.get + "/").fold(
        errorMsg => error(errorMsg),
        idPhoto => {
          Member.update(req.me.id.get, req.me.copy(mainPicture = Some(idPhoto)))
          Redirect(routes.Members.myProfile())
        })
    }.getOrElse(badrequest("Unable to find file. It must be sent as `picture`"))
  }
}
