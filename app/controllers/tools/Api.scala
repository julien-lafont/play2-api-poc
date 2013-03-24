package controllers.tools

import play.api.libs.json._
import play.api.mvc._
import scala.util.{Failure, Try, Success}
import org.joda.time.{LocalDate, DateTime}
import play.api.Logger

import play.api.Play.current
import scala.util.matching.Regex
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json.JsValueWrapper
import math._
import scala.util.Failure
import play.api.libs.json.JsString
import scala.Some
import scala.util.Success

trait Api extends Controller with JsonResult with ApiParameters with ApiPaginated {

  // Some shortcuts
  def post(key: String)(implicit request: Request[AnyContent]) = request.body.asFormUrlEncoded.flatMap(_.get(key).flatMap(_.headOption))
  def postLong(key: String)(implicit request: Request[AnyContent]) = post(key).map(_.toLong)
  def postInt(key: String)(implicit request: Request[AnyContent]) = post(key).map(_.toInt)
  def postDateTime(key: String)(implicit request: Request[AnyContent]) = post(key).map(toDate)

  def get(key: String)(implicit request: RequestHeader) = request.getQueryString(key)
  def getLong(key: String)(implicit request: RequestHeader) = get(key).map(_.toLong)
  def getInt(key: String)(implicit request: RequestHeader) = get(key).map(_.toInt)
  def getDateTime(key: String)(implicit request: RequestHeader) = get(key).map(toDate)

  private def toDate(date: String): DateTime = ISODateTimeFormat.date().parseDateTime(date)
}

case class ApiException(userError: String, techError: Option[String]) extends RuntimeException
case class Paginated(offset: Int, limit: Int)

trait JsonResult {
  this: Controller =>

  /**
   * Return success response with json body
   */
  def ok(response: => JsValue): Result = {
    Try(result(200, response = response))
    .recover {
      case ApiException(userError, techError) => {
        Logger.error(userError + techError.map("\n"+_).getOrElse(""))
        error(userError)
      }
      case e => {
        Logger.error("Unexpected error", e)
        error("Unexpected error")
      }
    }.get
  }

  def ok[A](r: => A)(implicit w : Writes[A]): Result = ok(Json.toJson(r))
  def ok(fields: (String, JsValueWrapper)*): Result = ok(Json.obj(fields:_*))
  def ok(): Result = result(200)

  // Simple error
  def error(error: String, code: Int = INTERNAL_SERVER_ERROR): Result = result(code, error = JsString(error))
  def errorForm(errors: JsValue, code: Int = BAD_REQUEST): Result = result(code, error = errors)

  // Special errors
  def forbidden(): Result = error("Forbidden", FORBIDDEN)
  def notfound(): Result = error("Not found", NOT_FOUND)
  def badrequest(msg: String = "Bad request"): Result = error(msg, BAD_REQUEST)

  /**
   * Return custom response
   */
  def result(code: Int, response: JsValue = JsNull, error: JsValue = JsNull) : Result = {
    Status(code)(Json.obj("meta" -> Json.obj("code" -> code, "error" -> error, "time" -> DateTime.now().getMillis), "response" -> response))
  }

}

/**
 * Api Transformers : transform String parameters to specific class
 */
trait ApiTransformers {
  abstract class ParamTransform[T](val transform: (String) => T, val typename: String)

  implicit object IntParamTransform extends ParamTransform[Int](_.toInt, "Integer")
  implicit object LongParamTransform extends ParamTransform[Long](_.toLong, "Long")
  implicit object DoubleParamTransform extends ParamTransform[Double](_.toDouble, "Double")
  implicit object StringParamTransform extends ParamTransform[String](_.toString, "String")
  implicit object BooleanParamTransform extends ParamTransform[Boolean](_.toBoolean, "Boolean")
  implicit object DateTimeParamTransform extends ParamTransform[DateTime](d => new DateTime(d.toLong), "DateTime")
}

/**
 * Api Validation : Check validity of fields
 */
trait ApiValidation {
  abstract class ParamValidation[T](val test: T => Boolean, val msg: String = "Invalid field")

  case class MinInt(min: Int) extends ParamValidation[Int](_ >= min, s"Minimum  $min")
  case class MaxInt(max: Int) extends ParamValidation[Int](_ <= max, s"Maxiumm $max")
  case class MinLong(min: Long) extends ParamValidation[Long](_ >= min, s"Minimum $min")
  case class MaxLong(max: Long) extends ParamValidation[Long](_ <= max, s"Maximum $max")
  case class MinLength(min: Long) extends ParamValidation[String](_.length >= min, s"Min length $min")
  case class MaxLength(max: Long) extends ParamValidation[String](_.length <= max, s"Max length $max")
  case class Pattern(pattern: Regex) extends RawPattern(pattern, s"Invalid pattern: ${pattern.pattern.toString}")
  case class Email(nothing: Boolean = false) extends RawPattern("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,4}".r, "Invalid email")
  case class Callback[T](callback: T => Boolean, override val msg: String) extends ParamValidation[T](callback)
  case class Coord(nothing: Boolean = false) extends ParamValidation[Double](c => c >= -180 && c <= 180, "Coordonnées GPS invalide")

  protected abstract class RawPattern(PatternValidation: Regex, msg: String) extends ParamValidation[String](_ match {
    case PatternValidation() => true
    case _ => false
  }, msg)

}

/**
 * Add an implicit Paginated to get pagination variables
 * offset: index of the first result (first: 0)
 * limit: number of object to return (max: 100, default: 20)
 */
trait ApiPaginated {
  this: Controller =>

  implicit def bindPaginationFromRequest(implicit req: RequestHeader): Paginated = {
    val offset = req.getQueryString("offset").map(_.toInt).map(o => max(0, o)) // Min=0
    val limit = req.getQueryString("limit").map(_.toInt).map(l => min(max(1, l), 100)) // Min=1, Max=100
    Paginated(offset.getOrElse(0), limit.getOrElse(20))
  }

}

/**
 * Extract and check parameters
 */
trait ApiParameters extends JsonResult with ApiTransformers with ApiValidation {
  this: Controller =>

  abstract trait Method
  case object POST extends Method
  case object GET extends Method
  case object ALL extends Method

  class Param[T](val name: String, val validations: ParamValidation[T]*)(implicit conv: ParamTransform[T]) {
    def transformer(value: String): T =
      Try(conv.transform(value))
        .getOrElse(throw new ApiParamException(s"Bad parameter `$name` (cannot convert `$value` to ${conv.typename})"))
  }
  object Param {
    def apply[T](name: String, validations: ParamValidation[T]*)(implicit conv: ParamTransform[T]) = new Param[T](name, validations:_*)(conv)
  }

  class ParamOpt[T](name: String, validation: ParamValidation[T]*)(implicit conv: ParamTransform[T]) extends Param[T](name, validation:_*)
  object ParamOpt {
    def apply[T](name: String, validations: ParamValidation[T]*)(implicit conv: ParamTransform[T]) = new ParamOpt[T](name, validations:_*)(conv)
  }

  def ApiParameters[A](p1: Param[A], method: Method = POST)(block: (A) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method)), block)
  }
  def ApiParameters[A,B](p1: Param[A], p2: Param[B], method: Method = POST)(block: (A,B) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method)), block.tupled)
  }
  def ApiParameters[A,B,C](p1: Param[A], p2: Param[B], p3: Param[C], method: Method = POST)(block: (A,B,C) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method), valid[C](p3, method)), block.tupled)
  }
  def ApiParameters[A,B,C,D](p1: Param[A], p2: Param[B], p3: Param[C], p4: Param[D], method: Method = POST)(block: (A,B,C,D) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method), valid[C](p3, method), valid[D](p4, method)), block.tupled)
  }
  def ApiParameters[A,B,C,D,E](p1: Param[A], p2: Param[B], p3: Param[C], p4: Param[D], p5: Param[E], method: Method = POST)(block: (A,B,C,D,E) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method), valid[C](p3, method), valid[D](p4, method), valid[E](p5, method)), block.tupled)
  }
  def ApiParameters[A,B,C,D,E,F](p1: Param[A], p2: Param[B], p3: Param[C], p4: Param[D], p5: Param[E], p6: Param[F], method: Method = POST)(block: (A,B,C,D,E,F) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method), valid[C](p3, method), valid[D](p4, method), valid[E](p5, method), valid[F](p6, method)), block.tupled)
  }
  def ApiParameters[A,B,C,D,E,F,G](p1: Param[A], p2: Param[B], p3: Param[C], p4: Param[D], p5: Param[E], p6: Param[F], p7: Param[G], method: Method = POST)(block: (A,B,C,D,E,F,G) => Result)(implicit request: play.api.mvc.Request[_]) = {
    check((valid[A](p1, method), valid[B](p2, method), valid[C](p3, method), valid[D](p4, method), valid[E](p5, method), valid[F](p6, method), valid[G](p7, method)), block.tupled)
  }

  private final case class ApiParamException(message: String) extends Exception(message)

  /**
   * Extract a parameter from Post or GET
   */
  private def getParamValue(name: String, method: Method)(implicit request: play.api.mvc.Request[_]): Option[String] = {
    request.body match {
      case body: play.api.mvc.AnyContent if (method == POST || method == ALL) && body.asFormUrlEncoded.isDefined =>
        body.asFormUrlEncoded.get.get(name).map(_.headOption).flatten
      case body: Map[_, _] if (method == POST || method == ALL) =>
        body.asInstanceOf[Map[String, Seq[String]]].get(name).map(_.headOption).flatten
      case get if (method == GET || method == ALL) =>
        request.getQueryString(name)
      // TODO : add json support
      case _ => None
    }
  }

  /**
   * Validate a field according to its ParamValidation
   */
  private def valid[T](param: Param[T], method: Method = ALL)(implicit request: play.api.mvc.Request[_]) : T = {
    getParamValue(param.name, method).map(paramValue =>
      if (paramValue.isEmpty)
        throw new ApiParamException(s"Parameter `${param.name}` is empty")
      else {
        val value = param.transformer(paramValue)
        val errors = param.validations.map(validation => if (validation.test(value)) None else Some(validation.msg)).flatten
        if (errors.nonEmpty) throw new ApiException(errors.mkString(", "), Some(s"Invalid value `$value` for parameter `${param.name}` : ${errors.mkString(", ")}"))
        value
      }
    ).getOrElse(throw new ApiParamException(s"Parameter `${param.name}` is missing"))
  }

  private def check[TUPLE](params: => TUPLE, block: TUPLE => Result) = {
    Try(params) match {
      case Success(s) => block(s)
      case Failure(e: ApiParamException) => {
        Logger.error("Invalid parameters", e)
        error(if (play.api.Play.isDev) e.getMessage else "Invalid request", 400)
      }
      case Failure(e: ApiException) => {
        Logger.error(s"Invalid parameters : ${e.techError.getOrElse(e.userError)}", e)
        error(e.userError, 400)
      }
      case Failure(e) => {
        Logger.error("Error while checking parameters", e)
        error("Unexpected error", 500)
      }
    }
  }
}
