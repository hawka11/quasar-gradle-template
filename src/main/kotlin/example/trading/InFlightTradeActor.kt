package example.trading

import co.paralleluniverse.actors.behaviors.RequestMessage
import co.paralleluniverse.actors.behaviors.RequestReplyHelper
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import example.trading.domain.CcyPair
import java.math.BigDecimal

class InFlightTradeActor : Actor() {

    val _this = this

    val initialState: (Any) -> Any? = @Suspendable {
        when (it) {
            is Event.GetQuote -> {
                println("getQuote: ${it.ccyPair}")
                //requested.addAll(it.pins)
                //receive(_this.initialState)
                RequestReplyHelper.reply(it, "ok")
            }
            else -> println("ignoring: $it")
        }
    }

    @Suspendable override fun doRun() = receive (initialState)

    sealed class Event {
        class GetQuote(val ccyPair: CcyPair) : RequestMessage<Any>()

        class Accept(val rate: BigDecimal) : RequestMessage<Any>()

        object Reject : RequestMessage<Any>()
    }
}