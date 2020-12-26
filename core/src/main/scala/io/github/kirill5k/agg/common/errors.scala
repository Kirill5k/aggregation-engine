package io.github.kirill5k.agg.common

import io.github.kirill5k.agg.enquiry.EnquiryId

object errors {

  sealed trait AggError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  final case class EnquiryNotFound(id: EnquiryId) extends AggError {
    val message: String = s"enquiry with id ${id.value} does not exist"
  }
}
