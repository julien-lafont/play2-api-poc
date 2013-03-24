package controllers.api

import play.api._
import play.api.mvc._
import controllers.tools._
import play.api.Play.current
import net.coobird.thumbnailator.Thumbnails
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import models.Photo

object Assets extends Security with Api {

  // TODO : Handle cache (server & client)
  def serve(path: String) = Action { implicit request =>
    current.getExistingFile(path).map { file =>
      val width = getInt("width")
      val height = getInt("height")

      // Handle Resizing
      if (width.isEmpty && height.isEmpty)
        Ok.sendFile(file, true)
      else if (width.isEmpty && height.isDefined) {
        val img = Thumbnails.of(file).height(height.get).asBufferedImage()
        serveImage(img)
      } else if (width.isDefined && height.isEmpty) {
        val img = Thumbnails.of(file).width(width.get).asBufferedImage()
        serveImage(img)
      } else {
        val img = Thumbnails.of(file).width(width.get).height(height.get).asBufferedImage()
        serveImage(img)
      }
    } getOrElse(NotFound)
  }

  private def serveImage(image: BufferedImage) = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(image, "jpg", baos);
    baos.flush();
    val imageInByte = baos.toByteArray();
    baos.close();
    Ok(imageInByte).withHeaders("Content-Type" -> "image/jpeg")
  }
}
