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
    "ADP", "ADSK", "ATVI", "AVGO", "CSCO", "CTXS", "EA", "EXPE", "INFY", "ORCL", "QCOM", "NXPI"
  )

  val watchListIds = List(
    ("FB",2067121),
    ("BABA",7422546),
    ("GOOG",11419765),
    ("AAPL",8049),
    ("TSLA",38526),
    ("MSFT",27426),
    ("NVDA",29814),
    ("AMZN",7410),
    ("CRM",35327),
    ("GOOGL",11419766),
    ("ADBE",6635),
    ("NFLX",28768),
    ("INTC",23205),
    ("BIDU",9090),
    ("ADP",8689),
    ("ADSK",8674),
    ("ATVI",6543),
    ("AVGO",21109371),
    ("CSCO",13648),
    ("CTXS",13790),
    ("EA",17173),
    ("EXPE",1484558),
    ("INFY",23023),
    ("ORCL",30678),
    ("QCOM",33237),
    ("NXPI",46411))

  val decider: Supervision.Decider = {
    case _ => Supervision.Resume
  }
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  //val api = new AlphaVantageApi(API_KEY)

  val token = System.getenv("WARPONY_TOKEN")
  val tokenProvider =
    if(token == "")None
    else Some( () => Future.successful(Questrade.Login(
      access_token = "",
      token_type = "",
      expires_in = 900,
      refresh_token = token,
      api_server = "https://api03.iq.questrade.com/"
    ) ) )

  val api = new QuestradeApi(false, tokenProvider = tokenProvider)

  def reportRsi2( symbols: List[(String, Int)] =  watchListIds ): Future[List[Log]] = {
    val STRATEGY = "RSI2"
    // TODO: veiry this is the correct time to call this
    Future.sequence(symbols.map{ sym =>
      (for{
        prices <- api.candles(sym._2, DateTime.now.minusDays(30), DateTime.now, Questrade.Interval.OneHour )
      }yield {
        val closePrices =  prices.candles.map(_.close).reverse
        val price = closePrices.head
        // TODO: verify this is the correct day?
        val last200 = closePrices.take(200)
        val sma200 = last200.sum / 200.0
        val last5 = closePrices.take(5)
        val sma5 = last5.sum / 5.0
        // calculate the new RSI
        val rsi2 = calculateRsi(closePrices, 2)
        //println(s"RSI: ${rsi2s.`Technical Analysis: RSI`.rsi.head}")

        // debug...
        /*println(s"price: ${price}")
        println(s"sma200: ${sma200}")
        println(s"sma5: ${sma5}")
        println(s"MY RSI: ${calculateRsi(closePrices, 2)}")*/

        // first check if price > sma200
        if(price > sma200){
          // next we check if price is < then sma 5
          if( price < sma5){
            // now we check if the rsi2 is less then 10.0
            if(rsi2 < 10.0)BuyTrigger(sym._1, price, DateTime.now, STRATEGY)
            else Quote(sym._1, price, DateTime.now)
          }else SellTrigger(sym._1, price, DateTime.now, STRATEGY)
        }else Quote(sym._1, price, DateTime.now)
      }).recover{
        case t: Throwable =>
          t.printStackTrace()
          Error(sym._1, 0.0, DateTime.now)
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