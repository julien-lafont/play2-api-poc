package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools.Api
import models.Member
import models.MemberJson._

object Members extends Api {

  val paramLogin = Param[String]("login", MinLength(4), Callback(Member.validLogin, "Login already registered"))
  val paramEmail = Param[String]("email", Email(), Callback(Member.validEmail, "Email already registered"))
  val paramPassword = Param[String]("password")

  /**
   * Check if required parameters for a subscription are valid
   *
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
      val member = Member(None, email, login, Member.hashPassword(password, uid), uid)
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
          Member.authenticate(member).map(token =>
            ok(token)
          ) getOrElse(forbidden)
        case None => forbidden
      }
    }
  }

  /**
   * Display member profile
   */
  def profile = Action { implicit request =>
    ApiParameters(Param[Long]("id"), GET) { id =>
      Member.findById(id).collect {
        case member if member.isActive => ok(member)
      } getOrElse(notfound)
    }
  }

  /**
   * Update a member profile. Each field is optional
   */
  def updateProfil(id: Long) = Action { implicit request =>
    Member.findById(id).collect {
      case member if member.id == member.id /* todo: get myId */ => {
        val updatedMember = member.copy(
          firstName = post.get("firstName").orElse(member.firstName),
          lastName = post.get("lastName").orElse(member.lastName),
          birthDate = post.get("birthDate").map(toDate(_)).orElse(member.birthDate),
          sex = post.get("sex").flatMap(sex => if (sex=="h" || sex=="f") Some(sex) else None).orElse(member.sex),
          city = post.get("city").orElse(member.city)
        )
        Member.update(id, updatedMember)
        ok
      }
    } getOrElse(forbidden)
  }

}
