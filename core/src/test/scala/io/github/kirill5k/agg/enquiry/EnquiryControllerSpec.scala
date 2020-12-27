package io.github.kirill5k.agg.enquiry

import cats.effect.IO
import io.github.kirill5k.agg.common.errors.EnquiryNotFound
import org.http4s.implicits._
import org.http4s.{Method, Request, Status}

class EnquiryControllerSpec extends ControllerSpec {

  "An EnquiryController" should {

    val enquiryId = EnquiryId("enquiry-1")

    "POST /enquiries" should {

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

    "GET /enquiries/:id" should {

      "return 404 when enquiry not found" in {
        val service = mock[EnquiryService[IO]]
        when(service.get(any[EnquiryId])).thenReturn(IO.raiseError(EnquiryNotFound(enquiryId)))

        val controller = new EnquiryController[IO](service)

        val request  = Request[IO](uri = uri"/enquiries/enquiry-1", method = Method.GET)
        val response = controller.routes.orNotFound.run(request)

        val expected = """{"message":"enquiry with id enquiry-1 does not exist"}"""
        verifyJsonResponse(response, Status.NotFound, Some(expected))
        verify(service).get(EnquiryId("enquiry-1"))
      }

      "return enquiry" in {
        val service = mock[EnquiryService[IO]]
        when(service.get(any[EnquiryId])).thenReturn(IO.pure(Enquiry(enquiryId, "processing", Query("Foo", "Bar"), Nil)))

        val controller = new EnquiryController[IO](service)

        val request  = Request[IO](uri = uri"/enquiries/enquiry-1", method = Method.GET)
        val response = controller.routes.orNotFound.run(request)

        val expected = """{"status":"processing","quotes":[],"id":"enquiry-1","query":{"firstName":"Foo","lastName":"Bar"}}"""
        verifyJsonResponse(response, Status.Ok, Some(expected))
        verify(service).get(EnquiryId("enquiry-1"))
      }
    }

    "GET /enquiries/:id/quotes" should {

      "return 404 when enquiry not found" in {
        val service = mock[EnquiryService[IO]]
        when(service.exists(any[EnquiryId])).thenReturn(IO.pure(false))

        val controller = new EnquiryController[IO](service)

        val request  = Request[IO](uri = uri"/enquiries/enquiry-1/quotes", method = Method.GET)
        val response = controller.routes.orNotFound.run(request)

        val expected = """{"message":"enquiry with id enquiry-1 does not exist"}"""
        verifyJsonResponse(response, Status.NotFound, Some(expected))
        verify(service).exists(EnquiryId("enquiry-1"))
      }
    }
  }
}
