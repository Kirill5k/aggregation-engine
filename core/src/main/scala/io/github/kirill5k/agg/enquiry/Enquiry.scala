package io.github.kirill5k.agg.enquiry

final case class EnquiryId(value: String) extends AnyVal

final case class Query(
    firstName: String,
    lastName: String
)

final case class Quote(
    providerName: String,
    price: BigDecimal
)

final case class Enquiry(
    id: EnquiryId,
    status: String,
    query: Query,
    quotes: List[Quote]
)
