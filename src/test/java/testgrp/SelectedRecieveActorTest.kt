package testgrp

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.actors.BasicActor
import co.paralleluniverse.actors.MessageProcessor
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import org.junit.Test
import java.util.concurrent.TimeUnit

class SelectedRecieveActorTest {

    @Test
    fun test() {

        spawnActor("test", object : BasicActor<Message, Message>() {

            val initialState = object : MessageProcessor<Message, Message> {
                @Suspendable override fun process(it: Message?): Message? {
                    when (it) {
                        is Message.Request -> {
                            println("first time: ${it.txt}")
                            return null
                        }
                        is Message.Request2 -> {
                            println("first time 2: ${it.txt}")
                            return it
                        }
                        else -> {
                            println("Not known first $it")
                            return receive(this)
                        }
                    }
                }
            }

            @Suspendable override fun doRun(): Message? {
                while (true) {
                    val nxt = receive(0, TimeUnit.SECONDS, initialState)
                    println("nxt: $nxt")
                    Fiber.sleep(1000)
                }
            }

        })

        spawnActor("test2", object : BasicActor<Message, Message>() {
            @Suspendable override fun doRun(): Message.Response? {
                val actorOneReg = ActorRegistry.getActor<ActorRef<Any>>("test")
                actorOneReg.send(Message.Request("wooooohoo"))
                println("resp:")
                Fiber.sleep(500)
                actorOneReg.send(Message.Request2("wooooohoo2"))
                return null
            }
        })

        Thread.sleep(999999)
    }

    sealed class Message {
        class Request(val txt: String) : Message()
        class Request2(val txt: String) : Message()
        class Response(val txt: String) : Message()
    }

    private fun spawnActor(name: String, actor: BasicActor<Message, Message>): BasicActor<Message, Message> {
        val fiber = Fiber("actor", actor)

        fiber.start()

        actor.register(name)

        return actor
    }
}