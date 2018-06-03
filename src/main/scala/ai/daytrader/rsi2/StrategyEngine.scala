package ai.daytrader.rsi2

import org.joda.time.DateTime
import io.surfkit.derpyhoves.flows._
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import io.surfkit.derpyhoves.flows._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StrategyEngine{

  trait SE

  trait Log extends SE{
    def symbol: String
    def price: Double
    def datetime: DateTime
  }

  case class Quote(symbol: String, price: Double, datetime: DateTime) extends Log
  case class Error(symbol: String, price: Double, datetime: DateTime) extends Log

  trait Trigger extends Log{
    def strategy: String
  }

  case class BuyTrigger(
                 symbol: String,
                 price: Double,
                 datetime: DateTime,
                 strategy: String) extends Trigger

  case class SellTrigger(
                 symbol: String,
                 price: Double,
                 datetime: DateTime,
                 strategy: String) extends Trigger

  val API_KEY = "PMGX9ASKF5L4PW7E"

  val watchList = List(
    "FB", "BABA", "GOOG", "AAPL", "TSLA", "MSFT", "NVDA", "AMZN", "CRM", "GOOGL", "ADBE", "NFLX", "INTC", "BIDU",
    "ADP", "ADSK", "ATVI", "AVGO", "CSCO", "CTXS", "DELL", "EA", "EXPE", "INFY", "ORCL", "QCOM", "NXPI"
  )

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


  val api = new AlphaVantageApi(API_KEY)

  def reportRsi2( symbols: List[String] =  watchList ): Future[List[Log]] = {
    val STRATEGY = "RSI2"
    // TODO: veiry this is the correct time to call this
    Future.sequence(symbols.map{ symbol =>
      (for{
        prices <- api.daily(symbol)
        sma200s <- api.sma(symbol, AlphaVantage.Interval.daily, 200)
        rsi2s <- api.rsi(symbol, AlphaVantage.Interval.daily, 2)
      }yield {
        val last100Price =  prices.`Time Series`.series.map(_._2.`4. close`.toDouble)
        val price = last100Price.head
        // TODO: verify this is the correct day?
        val sma200 = sma200s.`Technical Analysis: SMA`.sma.head._2.SMA.toDouble
        val last5Days = last100Price.take(5)
        val sma5 = last5Days.sum / 5.0
        // calculate the new RSI
        // TODO: check if RSI daily gives you current day.. i suspect it does not
        val rsi2 = rsi2s.`Technical Analysis: RSI`.rsi.head._2.RSI.toDouble
        println(s"RSI: ${rsi2s.`Technical Analysis: RSI`.rsi.head}")

        // debug...
        println(s"price: ${price}")
        println(s"sma200: ${sma200}")
        println(s"sma5: ${sma5}")
        println(s"rsi2: ${rsi2}")
        println(s"MY RSI: ${calculateRsi(last100Price, 2)}")

        // first check if price > sma200
        if(price > sma200){
          // next we check if price is < then sma 5
          if( price < sma5){
            // now we check if the rsi2 is less then 10.0
            if(rsi2 < 10.0)BuyTrigger(symbol, price, DateTime.now, STRATEGY)
            else Quote(symbol, price, DateTime.now)
          }else SellTrigger(symbol, price, DateTime.now, STRATEGY)
        }else Quote(symbol, price, DateTime.now)
      }).recover{
        case t: Throwable =>
          t.printStackTrace()
          Error(symbol, 0.0, DateTime.now)
      }
    })
  }


  def calculateRsi(prices: Seq[Double], interval: Int): Double = {
    // price is most recent to past .. so first reverse it
    val xs = prices.reverse
    val (sma, rest) = xs.splitAt(interval)
    val (gains, losses) = sma.zip(sma.drop(1)).map{ case (nMinus1, n) =>
      if(n > nMinus1){
        (n-nMinus1, 0.0)
      }else (0.0, nMinus1-n)
    }.unzip
    val gainSma = gains.sum / interval
    val lossSma = losses.sum / interval
    val (up, down, latest) = rest.drop(1).foldLeft( (gainSma, lossSma, rest.head) ){ case ((gainAvr: Double, lossAvr: Double, lastPrice: Double), price: Double) =>
      val (u,d) = if(price > lastPrice){
        (price-lastPrice, 0.0)
      }else (0.0, lastPrice-price)
      val upAvr = (gainAvr*(interval-1.0)+u)/interval
      val downAvr = (lossAvr*(interval-1.0)+d)/interval
      (upAvr, downAvr, price)
    }

    // RSI ..
    100.0 * (up / (up+down))
  }


}