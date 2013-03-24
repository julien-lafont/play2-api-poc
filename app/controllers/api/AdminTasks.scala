package controllers.api

import play.api._
import play.api.mvc._
import models.{Lieu, Membre}

import utils.Config
import controllers.tools.Api

import play.api.libs.json._

object AdminTasks extends Api with AdminSecurity {

  def ajouterLieu = AdminRestricted { implicit req =>
    ApiParameters(
      Param[String]("nom"),
      Param[String]("adresse"),
      Param[String]("code_postal"),
      Param[String]("ville"),
      Param[Long]("latitude"),
      Param[Long]("longitude"))
    { (nom, adresse, cp, ville, lt, lg) =>
      val id = Lieu.insert(Lieu(None, nom, adresse, cp, ville, lt, lg))
      ok(Json.obj("id" -> id))
    }
  }

}

trait AdminSecurity {
  this: Controller with Api =>

  import play.api.Play.current

  def AdminRestricted[A](p: BodyParser[A])(action: Request[A] => Result) = Action(p) { implicit request =>
    val authCode = request.getQueryString("auth").orElse(request.headers.get("Auth"))

    if (authCode == Config.getString("wenria.auth.admin")) {
      action(request)
    } else if (play.api.Play.isDev) {
      Logger.warn(s"Invalid admin auth received, but access granted in DEV mode")
      action(request)
    } else {
      forbidden
    }
  }

  def AdminRestricted(f: Request[AnyContent] => Result): Action[AnyContent] = {
    AdminRestricted(parse.anyContent)(f)
  }
}