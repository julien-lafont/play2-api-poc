package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools._
import models.Member
import models.MemberJson._

import play.api.data._
import play.api.data.Forms._

object MembersComparaison extends Api with Security {

  // ============== Check Subscription : Comparaisons ===================

  // ----- From Scratch
  def checkSubscriptionNative(login: String, email: String) = Action { implicit request =>
    if (login.length < 8) badrequest("Login size must be > 8 characters")
    if ("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,4}".r.findAllIn(email).isEmpty) badrequest("Invalid email")
    if (Member.validEmail(email)) badrequest("Email already registered")
    if (Member.validLogin(login)) badrequest("Login already registered")
    ok
  }

  // ----- With Play Forms
  def checkSubscriptionForm = Action { implicit request =>
    val checkSubscriptionForm = Form(tuple(
      "login" -> nonEmptyText(4),
      "email" -> email)
      verifying("Login already registered", _ match {
        case (l, _) => !Member.validLogin(l)
      })
      verifying("Email already registered", _ match {
        case (_, e) => !Member.validEmail(e)
      })
    )
    checkSubscriptionForm.bindFromRequest.fold(
      errors => errorForm(errors.errorsAsJson),
      success => ok
    )
  }

  // ------ With my custom API
  val paramLogin = Param[String]("login", MinLength(4), Callback(Member.validLogin, "Login already registered"))
  val paramEmail = Param[String]("email", Email(), Callback(Member.validEmail, "Email already registered"))
  val paramPassword = Param[String]("password")

  def checkSubscriptionCustomAPI = Action { implicit request =>
    ApiParameters(paramLogin, paramEmail, POST) { (login, email) => ok }
  }



  // ========== Subscribe new member : Comparaisons ===================

  // ----- From scratch
  def subscribeNative = Action { implicit request =>
    val uid = java.util.UUID.randomUUID().toString
    val member = for {
      login <- post("login") if (login.length > 4 && Member.validLogin(login))
      email <- post("email") if (!"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,4}".r.findAllIn(email).isEmpty && Member.validEmail(email))
      password <- post("password") if (!password.isEmpty)
    } yield Member(None, login, email, Member.hashPassword(password, uid), uid)

    member
      .map { m => ok(Json.obj("id" -> Member.insert(m))) }
      .getOrElse(error("Wrong form")) // TODO : Handle different errors !!
  }

  // ----- With Play forms
  def subscribeForm = Action { implicit request =>
    val uid = java.util.UUID.randomUUID().toString
    val subscribtionForm = Form(tuple(
      "login" -> nonEmptyText(4),
      "email" -> email,
      "password" -> nonEmptyText)
      verifying("Login already registered", _ match {
        case (l, _, _) => !Member.validLogin(l)
      })
      verifying("Email already registered", _ match {
        case (_, e, _) => !Member.validEmail(e)
      })
    )
    subscribtionForm.bindFromRequest.fold(
      errors => errorForm(errors.errorsAsJson),
      data => data match {
        case (login, email, password) => {
          val member = Member(None, login, email, Member.hashPassword(password, uid), uid)
          ok(Json.obj("id" -> Member.insert(member)))
        }
      }
    )
  }

  // ----- With my custom API
  def subscribeCustomAPI = Action { implicit request =>
    ApiParameters(paramLogin, paramEmail, paramPassword) { (login, email, password) =>
      val uid = java.util.UUID.randomUUID().toString
      val member = Member(None, login, email, Member.hashPassword(password, uid), uid)
      val insertedId = Member.insert(member)
      ok(Json.obj("id" -> insertedId))
    }
  }



  // ========== Show profile of one user ==================================

  // ----- From Scratch
  def profileNative(id: Long) = Action { implicit request =>
    Member.findById(id).collect {
      case member if member.isActive => ok(member)
    } getOrElse(notfound)
    // 404 if id not present
  }

  // ----- With my custom API
  def profileCustomAPI = Action { implicit request =>
    ApiParameters(Param[Long]("id"), GET) { id =>
      Member.findById(id).collect {
        case member if member.isActive => ok(member)
      } getOrElse(notfound)
    }
  }

}