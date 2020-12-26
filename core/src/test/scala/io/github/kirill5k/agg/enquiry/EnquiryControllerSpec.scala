package io.github.kirill5k.agg.enquiry

import cats.effect.IO
import org.http4s.{Method, Request, Status}
import org.http4s.implicits._

class EnquiryControllerSpec extends ControllerSpec {

  "An EnquiryController" should {

    val enquiryId = EnquiryId("enquiry-1")

    "POST /enquiry" should {

      "create new enquiry" in {
        val service = mock[EnquiryService[IO]]
        when(service.create(any[Query])).thenReturn(IO.pure(enquiryId))

        val controller = new EnquiryController[IO](service)

        val requestBody = """{"query":{"firstName": "Foo","lastName": "Bar"}}"""
        val request  = Request[IO](uri = uri"/enquiries", method = Method.POST).withEntity(requestBody)
        val response = controller.routes.orNotFound.run(request)

        val expected = """{"enquiryId":"enquiry-1"}"""
        verifyJsonResponse(response, Status.Created, Some(expected))
        verify(service).create(Query("Foo", "Bar"))
      }
    }
  }
}
