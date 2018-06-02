package ai.daytrader.rsi2

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import io.surfkit.derpyhoves.flows._
import scala.concurrent.ExecutionContext.Implicits.global


object StrategyEngine{

  val watchList = List(
    "FB", "BABA", "GOOG", "AAPL", "TSLA", "MSFT", "NVDA", "AMZN", "CRM", "GOOGL", "ADBE", "NFLX", "INTC", "BIDU",
    "ADP", "ADSK", "ATVI", "AVGO", "CSCO", "CTXS", "DELL", "EA", "EXPE", "INFY", "ORCL", "QCOM", "NXPI"
  )

  val API_KEY = "PMGX9ASKF5L4PW7E"

  val decider: Supervision.Decider = {
    case _ => Supervision.Resume
  }
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  /// TODO:
  // for each item in our watch list
  // get the 200 day SMA
  // get the 5 day SMA
  // get a 2 period RSI

  // TODO: need to add SMA and RSI into alpha vantage calls
  // TODO: see if the day call will get "todays" value... otherwise we will have to do some math to get the current value
  // TODO: we will want to date and time check before sending to slack..


}