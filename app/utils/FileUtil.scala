package utils

import java.io.File
import scalax.file.ImplicitConversions.jfile2path

import scalax.file.Path
import util.Try
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile

object FileUtil {

  def createDirectoryIfNotExist(file: File) = {
    val folder = if (file.isFile) file.getParentFile else file
    Try { Path(folder).createDirectory() }
  }

  def checkFile(file: MultipartFormData.FilePart[TemporaryFile], extensions: List[String], maxSizeMo: Int): Either[String, String] = {

    def checkExtension: Either[String, String] = Path(file.filename).extension.filter(ext => extensions.contains(ext)).map(Right(_))
      .getOrElse(Left("Extension not authorized"))
    def checkSize: Either[String, Long] = file.ref.file.size.filter(size => size <= maxSizeMo * 1024 * 1024).map(Right(_))
      .getOrElse(Left(s"File too big (max authorized: $maxSizeMo mo)"))

    for {
      _ <- checkExtension.right.map(e => play.api.Logger.debug(e)).right
      _ <- checkSize.right.map(e => play.api.Logger.debug(e.toString)).right
    } yield "ILoveChicken"
  }
}
