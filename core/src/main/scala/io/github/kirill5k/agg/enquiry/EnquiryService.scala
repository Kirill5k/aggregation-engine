package io.github.kirill5k.agg.enquiry

import fs2.Stream

trait EnquiryService[F[_]] {
  def create(query: Query): F[EnquiryId]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}
