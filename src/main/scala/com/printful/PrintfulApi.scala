package com.printful

import com.typesafe.scalalogging.StrictLogging
import sttp.client.Request
import sttp.tapir._
import sttp.tapir.model.UsernamePassword
import sttp.tapir.openapi.Server
import sttp.tapir.swagger.akkahttp.SwaggerAkka

object PrintfulApi extends App with StrictLogging {
  type Limit = Option[Int]

  case class SyncProduct(id: Int,
                         external_id: String,
                         name: String,
                         variants: Int,
                         synced: Int,
                         thumbnail_url: String)

  case class SyncProductInfo(sync_product: SyncProduct,
                             sync_variants: List[SyncVariant])
  case class SyncVariant(id: Int,
                         external_id: String,
                         sync_product_id: Int,
                         name: String,
                         synced: Boolean,
                         variant_id: Int)

  case class Page(total: Int, offset: Int, limit: Int)
  case class ProductsResponse(code: Int,
                              result: List[SyncProduct],
                              paging: Page)
  case class ProductResponse(code: Int, result: SyncProductInfo)

  case class PutRequestProductBody(sync_product: PutRequestProduct,
                                   sync_variants: List[PutRequestVariant])
  case class PutRequestProduct(external_id: String,
                               name: String,
                               thumbnail: String)

  case class PutRequestVariant(id: String,
                               external_id: String,
                               variant_id: Int,
                               retail_price: Double,
                               files: List[RequestFile],
                               options: List[ItemOption])

  case class RequestFile(`type`: String, id: Int, url: String)

  case class ItemOption(id: String, value: Map[String, Any])

  case class PutProductResponse(code: Int, result: RequestProductResponse)
  case class RequestProductResponse(id: Int,
                                    external_id: String,
                                    name: String,
                                    variants: Int,
                                    synced: Integer)

  /**
    * Descriptions of endpoints used in the example.
    */
  object Endpoints {
    import io.circe.generic.auto._
    import sttp.tapir._
    import sttp.tapir.json.circe._

    // All endpoints report errors as strings, and have the common path prefix '/books'
    private val baseEndpoint = endpoint.errorOut(stringBody).in("store")

    val productsEndpoint
      : Endpoint[UsernamePassword, String, ProductsResponse, Nothing] =
      baseEndpoint.get
        .in(auth.basic)
        .in("products")
        .out(jsonBody[ProductsResponse])

    val productEndpoint
      : Endpoint[(UsernamePassword, Int), String, ProductResponse, Nothing] =
      baseEndpoint.get
        .in(auth.basic)
        .in("products" / path[Int]("productId"))
        .out(jsonBody[ProductResponse])
  }

  import Endpoints._
  import akka.http.scaladsl.server.Route

  def openapiYamlDocumentation: String = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._

    // interpreting the endpoint description to generate yaml openapi documentation
    val docs =
      List(productsEndpoint, productEndpoint)
        .toOpenAPI("Printful Api", "1.0")
        .servers(
          List(
            Server("http://localhost:8080", None),
            Server("http://api.printful.com", None)
          )
        )
    docs.toYaml
  }

  def printfulRoutes: Route = {
    import akka.http.scaladsl.server.Directives._
    import sttp.tapir.server.akkahttp._

    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future

    import sttp.client._
    import sttp.tapir.client.sttp._

    implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      HttpURLConnectionBackend()
    val productRequest =
      productEndpoint.toSttpRequestUnsafe(uri"http://api.printful.com")

    val productsRequest =
      productsEndpoint.toSttpRequestUnsafe(uri"http://api.printful.com")

    def product(auth: UsernamePassword,
                productId: Int): Future[Either[String, ProductResponse]] =
      Future {
        productRequest.apply(auth, productId).send().body
      }

    def products(
      auth: UsernamePassword
    ): Future[Either[String, ProductsResponse]] = Future {
      productsRequest(auth).send().body
    }

    // interpreting the endpoint description and converting it to an akka-http route, providing the logic which
    // should be run when the endpoint is invoked.
    productEndpoint.toRoute((product _).tupled) ~
      productsEndpoint.toRoute(products)
  }

  def startServer(route: Route, yaml: String): Unit = {
    import akka.actor.ActorSystem
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.server.Directives._

    import scala.concurrent.Await
    import scala.concurrent.duration._
    val routes = route ~ new SwaggerAkka(yaml).routes
    implicit val actorSystem: ActorSystem = ActorSystem()
    Await.result(Http().bindAndHandle(routes, "localhost", 8080), 1.minute)

    logger.info("Server started")
  }

  logger.info("Welcome to the Tapir Library example!")

  logger.info("Starting the server ...")
  startServer(printfulRoutes, openapiYamlDocumentation)

  logger.info("Making a request to the listing endpoint ...")

  logger.info(
    "Try out the API by opening the Swagger UI: http://localhost:8080/docs"
  )
}
