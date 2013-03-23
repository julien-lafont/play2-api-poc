package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools._

import play.api.Play.current
import models.Photo

object Assets extends Security with Api {

  def serve(path: String) = Action {
    val result = for {
      file <- current.getExistingFile(path)
      photo <- Photo.findByPath(path)
    } yield Ok.sendFile(file, true)
    result.getOrElse(NotFound)
  }
}
