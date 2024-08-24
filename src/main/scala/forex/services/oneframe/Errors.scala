package forex.services.oneframe

object Errors {
  sealed trait OneFrameError {
    def message: String
  }

  object OneFrameError {
    final case object OneFrameTimeoutError extends OneFrameError {
      val message: String = "OneFrame request timed out"
    }

    final case class OneFrameUnreachableError(message: String) extends OneFrameError

    final case class OneFrameUnknownError(message: String) extends OneFrameError
  }
  final case class OneFrameServiceErrorResponse(error: String) extends Exception(error)
}
