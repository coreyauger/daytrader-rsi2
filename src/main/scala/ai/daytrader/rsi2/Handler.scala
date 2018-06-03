package ai.daytrader.rsi2

import java.util.concurrent.atomic.AtomicInteger

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import ai.daytrader.rsi2.StrategyEngine._

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

    val atomic = new AtomicInteger(0)

    val results: List[StrategyEngine.Log] = StrategyEngine.watchList.grouped(8).toList.flatMap { watch =>
      if(atomic.getAndAdd(1) > 0)Thread.sleep(61 * 1000)  // sleep 20 seconds .. to get around BS throttle .. hookerz!
      val result = Await.result(StrategyEngine.reportRsi2( watch ), 10 seconds)
      println("Report")
      println(result)
      result
    }
    val attachments = results.map{
      case x:StrategyEngine.BuyTrigger =>
        AlphaVantage.SlackAttachment(
          fallback = s"${x.datetime} : ${x.strategy} - BUY [${x.symbol}] @ ${x.price}.",
          color = Some("#36a64f"),
          author_name = Some(x.symbol),
          author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
          author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
          title = s"${x.strategy} - BUY [${x.symbol}]",
          fields = Seq(
            AlphaVantage.SlackField(
              title = "BUY",
              value = x.price.toString,
              short = false
            )
          ),
          ts = Some(x.datetime.getMillis() / 1000)
        )

      case x:StrategyEngine.SellTrigger =>
        AlphaVantage.SlackAttachment(
          fallback = s"${x.datetime} : ${x.strategy} - SELL [${x.symbol}] @ ${x.price}.",
          color = Some("#000000"),
          author_name = Some(x.symbol),
          author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
          author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
          title = s"${x.strategy} - SELL [${x.symbol}]",
          fields = Seq(
            AlphaVantage.SlackField(
              title = "SELL",
              value = x.price.toString,
              short = false
            )
          ),
          ts = Some(x.datetime.getMillis() / 1000)
        )

      case x:StrategyEngine.Quote =>
        AlphaVantage.SlackAttachment(
          fallback = s"${x.datetime} : [${x.symbol}] @ ${x.price}.",
          color = Some("#cccccc"),
          author_name = Some(x.symbol),
          author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
          author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
          title = s"${x.symbol}]",
          text = s"${x.price}",
          ts = Some(x.datetime.getMillis() / 1000)
        )
      case x =>
        AlphaVantage.SlackAttachment(
          fallback = s"${x.datetime} : [${x.symbol}] ERROR.",
          color = Some("#ff0000"),
          author_name = Some(x.symbol),
          author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
          author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
          title = s"${x.symbol}]",
          text = s"ERROR GETTING DATA",
          ts = Some(x.datetime.getMillis() / 1000)
        )

    }

    api.sendSlack("https://hooks.slack.com/services/", AlphaVantage.SlackAttachments(attachments))

    val headers = Map[String, Object]("x-custom-response-header" -> "my custom response header value")
    ApiGatewayResponse(200, "Go Serverless v1.0! Your function executed successfully!",headers.asJava,true)
  }
}

