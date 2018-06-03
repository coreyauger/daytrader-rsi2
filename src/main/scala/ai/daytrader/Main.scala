package ai.daytrader

import ai.daytrader.rsi2.StrategyEngine
import ai.daytrader.rsi2.StrategyEngine._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import io.surfkit.derpyhoves.flows._

object Main extends App {

  val results = StrategyEngine.watchList.grouped(8).toList.flatMap { watch =>

    val result = Await.result(StrategyEngine.reportRsi2( watch ), 10 seconds)
    println("Report")
    println(result)
    Thread.sleep(61 * 1000)  // sleep 20 seconds .. to get around BS throttle .. mother fucks
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
}