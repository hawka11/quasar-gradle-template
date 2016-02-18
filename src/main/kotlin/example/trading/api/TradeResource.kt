package example.trading.api

import co.paralleluniverse.actors.Actor
import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.behaviors.RequestReplyHelper
import co.paralleluniverse.fibers.Fiber
import example.trading.InFlightTradeActor
import example.trading.domain.CcyPair

class TradeResource {

    public fun getQuote(ccyPair: CcyPair): Any {
        val actor = spawnActor("", InFlightTradeActor())
        return RequestReplyHelper.call(actor, InFlightTradeActor.Event.GetQuote(ccyPair));
    }

    private fun spawnActor(name: String, actor: Actor): ActorRef<Any?> {
        val fiber = Fiber("actor - $name", actor)
        fiber.start()
        actor.register(name)
        return actor.ref()
    }
}