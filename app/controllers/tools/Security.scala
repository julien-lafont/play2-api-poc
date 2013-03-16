package controllers.tools

import play.api.mvc._
import models.Member

sealed case class AuthenticatedRequest(user: Member, private val request: Request[AnyContent])
  extends WrappedRequest(request)

trait Security {
  self: Controller with Api =>

  /**
   * Execute this action in an authenticated context.
   * The token must be send with QueryString (?token=xxx) or Header (Token: xxx)
   * If the token is valid, add a user key to the request with the user data
   * Otherwise, return a forbidden error
   */
  def Authenticated(action: AuthenticatedRequest => Result) = Action { implicit request =>
    val token = request.getQueryString("token").orElse(request.headers.get("Token"))
    token.flatMap(t => Member.findByToken(t)).map(member =>
      action(AuthenticatedRequest(member, request))
    ).getOrElse(error("This request requires a valid token", FORBIDDEN))
  }

  implicit def me(implicit req: AuthenticatedRequest) = req.user
}