package ai.daytrader

import ai.daytrader.rsi2.StrategyEngine
import ai.daytrader.rsi2.StrategyEngine._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  StrategyEngine.watchList.grouped(8).toList.foreach { watch =>

    val result = Await.result(StrategyEngine.reportRsi2( watch ), 10 seconds)
    println("Report")
    println(result)
    Thread.sleep(61 * 1000)  // sleep 20 seconds .. to get around BS throttle .. mother fucks

  }
}