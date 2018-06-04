package ai.daytrader

import ai.daytrader.rsi2.StrategyEngine
import ai.daytrader.rsi2.StrategyEngine._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import io.surfkit.derpyhoves.flows._
import java.util.concurrent.atomic.AtomicInteger


object Main extends App {

  val atomic = new AtomicInteger(0)

  /*val results = StrategyEngine.watchListIds.grouped(9).toList.flatMap { watch =>
  //val results = List(List("MSFT")).flatMap { watch =>

    Thread.sleep(61 * 1000) // sleep 20 seconds .. to get around BS throttle .. hookerz
    val result = Await.result(StrategyEngine.reportRsi2( watch ), 45 seconds)
    println("Report")
    println(result)
    result
  }*/

  val results = Await.result(StrategyEngine.reportRsi2( StrategyEngine.watchListIds ), 45 seconds)

  println(s"results: ${results}")

  val attachments = results.sortBy(_.symbol).map{
    case x:StrategyEngine.BuyTrigger =>
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

    case x:StrategyEngine.SellTrigger =>
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

    case x:StrategyEngine.Quote =>
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

  println("calling slack!")
  attachments.foreach(println)
}