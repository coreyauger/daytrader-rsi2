package ai.daytrader.rsi2

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.beans.BeanProperty

class Request(@BeanProperty var key1: String, @BeanProperty var key2: String, @BeanProperty var key3: String) {
  def this() = this("", "", "")
}

case class Response(@BeanProperty message: String, @BeanProperty request: Request)


class Handler extends RequestHandler[Request, Response] {

	def handleRequest(input: Request, context: Context): Response = {
		Response("Go Serverless v1.0! Your function executed successfully!", input)
	}
}

class ApiGatewayHandler extends RequestHandler[Request, ApiGatewayResponse] {
  import scala.collection.JavaConverters._
  def handleRequest(input: Request, context: Context): ApiGatewayResponse = {
    val headers = Map[String, Object]("x-custom-response-header" -> "my custom response header value")
    ApiGatewayResponse(200, "Go Serverless v1.0! Your function executed successfully!",headers.asJava,true)
  }
}

