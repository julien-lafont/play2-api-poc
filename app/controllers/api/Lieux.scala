package controllers.api

import play.api._
import libs.json.Json
import play.api.mvc._
import controllers.tools._
import models.Lieu
import models.LieuJson._

object Lieux extends Security with Api {

  val paramLatitude = Param[Double]("lt", Coord())
  val paramLongitude = Param[Double]("lg", Coord())

  def search = Authenticated { implicit session =>
    ApiParameters(paramLatitude, paramLongitude, GET) { (lt, lg) =>
      ok(Lieu.searchLieuxProchesPosition(lt, lg))
    }
  }

}
