package controllers.tools

import play.api.mvc._
import models.Member
import play.api.Play.current
import play.api._

sealed case class AuthenticatedRequest[A](me: Member, private val request: Request[A])
  extends WrappedRequest(request)

trait Security {
  self: Controller with Api =>

  /**
   * Execute this action in an authenticated context.
   * The token must be send with QueryString (?token=xxx) or Header (Token: xxx)
   * If the token is valid, add a user key to the request with the user data
   * Otherwise, return a forbidden error
   */
  def Authenticated[A](p: BodyParser[A])(action: AuthenticatedRequest[A] => Result) = Action(p) { implicit request =>
    val token = request.getQueryString("token").orElse(request.headers.get("Token"))

    token.flatMap(t => Member.findByToken(t)).map(member =>
      action(AuthenticatedRequest(member, request))
    ) orElse { // Mock in dev mode
      if (play.api.Play.isDev) {
        Member.findOneRandom().map { user =>
          Logger.warn(s"Invalid token received, mock logged user to #${user.id.get}: ${user.login}")
          action(AuthenticatedRequest(user, request))
        }
      } else {
        None
      }
    } getOrElse(error("This request requires a valid token", FORBIDDEN))
  }

  /**
   * Shortcut with default BodyParser
   */
  def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent]  = {
    Authenticated(parse.anyContent)(f)
  }

  /**
   * Put the logged user in implicit context
   */
  implicit def me(implicit req: AuthenticatedRequest[AnyContent]) = req.me
}