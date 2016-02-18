package testgrp

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.actors.behaviors.RequestMessage
import co.paralleluniverse.actors.behaviors.RequestReplyHelper
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import org.junit.Test

class SimpleKotlinActorTest {

    @Test
    fun test() {

        spawnActor("test", object : Actor() {

            private val skipped = arrayListOf<Request>()

            val _r = this

            val requestedState: (Any) -> Any? = @Suspendable {
                when (it) {
                    is Request -> {
                        println("second time: ${it.txt}")
                        RequestReplyHelper.reply(it, Response("back at ya"));
                        receive(_r.requestedState)
                    }
                    else -> {
                        println("Not known second $it")
                        receive(initialState)
                    }
                }
            }

            val initialState: (Any) -> Any? = @Suspendable {
                when (it) {
                    is Request -> {
                        println("first time: ${it.txt}")
                        skipped.add(it)
                        receive(requestedState)
                    }
                    else -> {
                        println("Not known first $it")
                        receive(_r.initialState)
                    }
                }
            }

            @Suspendable override fun doRun() = receive (initialState)

        })

        spawnActor("reqReply", object : Actor() {
            @Suspendable override fun doRun() {
                val actorOneReg = ActorRegistry.getActor<ActorRef<Request>>("test")
                val resp: Response = RequestReplyHelper.call(actorOneReg, Request("wooooohoo"))
                println("resp: ${resp.txt}")
            }
        })

        spawnActor("multisend", object : Actor() {
            @Suspendable override fun doRun() {
                val actorOneReg = ActorRegistry.getActor<ActorRef<Any>>("test")
                (1..6).forEach {
                    actorOneReg.send("hello $it")
                    Fiber.sleep(800)
                }
            }
        })

        Thread.sleep(999999)
    }

    class Request(val txt: String) : RequestMessage<Response>()
    class Response(val txt: String)

    private fun spawnActor(name: String, actor: Actor): Actor {
        val fiber = Fiber("actor", actor)

        fiber.start()

        actor.register(name)

        return actor
    }
}