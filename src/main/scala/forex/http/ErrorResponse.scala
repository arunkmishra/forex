package forex.http

import io.circe.Encoder
import io.circe.generic.semiauto._

/**
  * Model used for client facing errors
  * @param message error message returned to client
  * */
final case class ErrorResponse(message: String)
object ErrorResponse {
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}
