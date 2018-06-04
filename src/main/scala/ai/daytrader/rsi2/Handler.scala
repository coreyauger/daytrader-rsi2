package ai.daytrader.rsi2

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.beans.BeanProperty
import ai.daytrader.rsi2.StrategyEngine._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import io.surfkit.derpyhoves.flows._
import scala.language.postfixOps
import java.util.concurrent.atomic.AtomicInteger

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

  implicit val timeout = 45 seconds

  def handleRequest(input: Request, context: Context): ApiGatewayResponse = {

    System.out.println("handleRequest has been envoked")
    try {
      //val atomic = new AtomicInteger(0)



      val results = Await.result(StrategyEngine.reportRsi2( StrategyEngine.watchListIds ), 45 seconds)
      System.out.println("creating attachments")
      val attachments = results.sortBy(_.symbol).map {
        case x: StrategyEngine.BuyTrigger =>
          Questrade.SlackAttachment(
            fallback = s"${x.datetime} : ${x.strategy} - BUY [${x.symbol}] @ ${x.price}.",
            color = Some("#36a64f"),
            author_name = Some(x.symbol),
            author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
            author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
            title = s"${x.strategy} - BUY [${x.symbol}]",
            fields = Seq(
              Questrade.SlackField(
                title = "BUY",
                value = x.price.toString,
                short = false
              )
            ),
            ts = Some(x.datetime.getMillis() / 1000)
          )

        case x: StrategyEngine.SellTrigger =>
          Questrade.SlackAttachment(
            fallback = s"${x.datetime} : ${x.strategy} - SELL [${x.symbol}] @ ${x.price}.",
            color = Some("#000000"),
            author_name = Some(x.symbol),
            author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
            author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
            title = s"${x.strategy} - SELL [${x.symbol}]",
            fields = Seq(
              Questrade.SlackField(
                title = "SELL",
                value = x.price.toString,
                short = false
              )
            ),
            ts = Some(x.datetime.getMillis() / 1000)
          )

        case x: StrategyEngine.Quote =>
          Questrade.SlackAttachment(
            fallback = s"${x.datetime} : [${x.symbol}] @ ${x.price}.",
            color = Some("#cccccc"),
            author_name = Some(x.symbol),
            author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
            author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
            title = s"${x.symbol}]",
            text = Some(s"${x.price}"),
            ts = Some(x.datetime.getMillis() / 1000)
          )
        case x =>
          Questrade.SlackAttachment(
            fallback = s"${x.datetime} : [${x.symbol}] ERROR.",
            color = Some("#ff0000"),
            author_name = Some(x.symbol),
            author_link = Some(s"https://www.nasdaq.com/symbol/${x.symbol}"),
            author_icon = Some(s"https://www.nasdaq.com/logos/${x.symbol}.GIF"),
            title = s"${x.symbol}",
            text = Some(s"ERROR GETTING DATA"),
            ts = Some(x.datetime.getMillis() / 1000)
          )

      }

      val slackUrl = System.getenv("SLACK_URL")
      System.out.println("sending to slack")
      api.sendSlack(slackUrl, Questrade.SlackAttachments(attachments))
    }catch{
      case t: Throwable =>
        t.printStackTrace()
        System.out.println(s"ERROR: ${t.getMessage}")
    }
    val headers = Map[String, Object]("x-custom-response-header" -> "my custom response header value")
    ApiGatewayResponse(200, "Go Serverless v1.0! Your function executed successfully!",headers.asJava,true)
  }
}

