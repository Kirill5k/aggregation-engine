package io.github.kirill5k.agg.enquiry

import fs2.Stream

trait EnquiryStore[F[_]] {
  def save(enquiry: Enquiry): F[Unit]
  def addQuote(enquiryId: EnquiryId, quote: Quote): F[Unit]
  def getQuotes(enquiryId: EnquiryId): Stream[F, Quote]
}
