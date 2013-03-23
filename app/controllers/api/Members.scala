package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools._
import models.{MembreSearch, Photo, Membre}
import models.MembreJson._

object Members extends Security with Api {

  val paramLogin = Param[String]("login", MinLength(4), Callback(Membre.validLogin, "Login already registered"))
  val paramEmail = Param[String]("email", Email(), Callback(Membre.validEmail, "Email already registered"))
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
      val membre = Membre(None, login, email, Membre.hashPassword(password, uid), uid)
      val insertedId = Membre.insert(membre)
      ok(Json.obj("id" -> insertedId))
    }
  }

  /**
   * Authenticate a user with a login and a password (hashed).
   * @return (token, expiration in ms) in valid, else Forbidden
   */
  def authenticate = Action { implicit request =>
    ApiParameters(Param[String]("login"), Param[String]("password")) { (login, password) =>
      println(Membre.findByLogin(login))
      Membre.findByLogin(login) match {
        case Some(membre) if Membre.hashPassword(password, membre.uid) == membre.password =>
          Membre.authenticate(membre)
            .map(token => ok(token))
            .getOrElse(forbidden)
        case None => error("Invalid credentials")
      }
    }
  }

  /**
   * Display Membre profile
   */
  def profile(id: Long) = Action { implicit request =>
    Membre.findById(id)
      .map(membre => ok(membre))
      .getOrElse(notfound)
  }

  /**
   * Display my own profile
   */
  def myProfile = Authenticated { implicit request =>
    profile(me.id.get)(request)
  }

  /**
   * Update a Membre profile. Each field is optional
   */
  def updateMyProfil = Authenticated { implicit request =>
      Membre.update(me.id.get, me.copy(
        prenom          = post("prenom").orElse(me.prenom),
        nom             = post("nom").orElse(me.nom),
        dateNaissanceTs = postDateTime("dateNaissance").map(_.getMillis).orElse(me.dateNaissanceTs),
        sexe            = post("sexe").flatMap(sex => if (sex=="h" || sex=="f") Some(sex) else None).orElse(me.sexe),
        ville           = post("ville").orElse(me.ville),
        description     = post("description").orElse(me.description)
      ))
      myProfile(request)
  }

  /**
   * Search a Membre from multiple criterias. Each field is optional
   */
  def search = Authenticated { implicit request =>
    val filter = MembreSearch(get("login"), get("prenom"), get("nom"), get("sexe"), get("ville"), getInt("age"))
    ok(Membre.search(filter))
  }

  /**
   * Upload and save a new main picture
   */
  def uploadProfilPicture = Authenticated(parse.multipartFormData) { req =>
    req.body.file("picture").map { picture =>
      Photo.uploadAndSaveFile(picture, "picture/" + req.me.id.get + "/").fold(
        errorMsg => error(errorMsg),
        idPhoto => {
          Membre.update(req.me.id.get, req.me.copy(photoProfil = Some(idPhoto)))
          Redirect(routes.Members.myProfile())
        })
    }.getOrElse(badrequest("Unable to find file. It must be sent as `picture`"))
  }
}
