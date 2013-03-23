import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData

package object models {
  // Flemmard level 60
  type OptString = Option[String]
  type OptInt = Option[Int]
  type OptLong = Option[Long]
  type UploadFile = MultipartFormData.FilePart[TemporaryFile]
}
