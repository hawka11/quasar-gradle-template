package testgrp

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.strands.Strand
import org.junit.Test
import java.util.concurrent.TimeUnit

class KotlinRechargeActorTest {

    fun pathToResource(name: String): String {
        return ClassLoader.getSystemClassLoader().getResource(name)!!.path
    }

    init {
        val nodeId = 1
        System.setProperty("co.paralleluniverse.galaxy.configFile", pathToResource("config/peer.xml"))
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId))
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId))
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId))
        //System.setProperty("galaxy.server_port", Integer.toString(8060 + nodeId))
        System.setProperty("galaxy.multicast.address", "225.0.0.1")
        System.setProperty("galaxy.multicast.port", Integer.toString(7050))
        System.setProperty("galaxy.zkServers", "127.0.0.1:2181")
        // System.setProperty("galaxy.ip_server_port", "127.0.0.1:2181")
    }

    var i = 0

    @Test
    fun test() {

        spawnActor("test", object : Actor() {

            val _this = this

            val requested = arrayListOf<String>()
            val confirmed = arrayListOf<String>()

            val finalState: (Any) -> Any? = @Suspendable {
                println("ignoring (in final): $it")
                i += 1
                if ((i % 2) == 0) {
                    Strand.sleep(1000)
                    receive(_this.finalState)
                } else {
                    defer()
                }
            }

            val requestedState: (Any) -> Any? = @Suspendable {
                when (it) {
                    is Event.Confirm -> {
                        println("confirmed: ${it.pin}")
                        confirmed.add(it.pin)

                        if (confirmed.size == requested.size)
                            receive(finalState) else
                            receive(2, TimeUnit.SECONDS, _this.requestedState)
                    }
                    else -> defaultHandle(it)
                }
            }

            val initialState: (Any) -> Any? = @Suspendable {
                when (it) {
                    is Event.Recharge -> {
                        println("recharging: ${it.pins}")
                        requested.addAll(it.pins)
                        receive(requestedState)
                    }
                    else -> defaultHandle(it)
                }
            }

            private fun defaultHandle(event: Any) {
                when (event) {
                    is Timeout -> {
                        println("timed-out")
                        receive(finalState)
                    }
                    else -> println("ignoring: $event")
                }
            }

            @Suspendable override fun doRun() = receive (initialState)

        })

        fun get() = ActorRegistry.getActor<ActorRef<Any>>("test")

        spawnActor("requester", object : Actor() {
            @Suspendable override fun doRun(): Any? {
                Fiber.sleep(1000)
                get().send(Event.Recharge(listOf("2", "5")))
                return null;
            }
        })

        spawnActor("confirmer", object : Actor() {
            @Suspendable override fun doRun(): Any? {
                Fiber.sleep(1500)
                get().send(Event.Confirm("2"))
                Fiber.sleep(1500)
                get().send(Event.Confirm("5"))
                Fiber.sleep(1500)
                get().send(Event.Confirm("5"))
                Fiber.sleep(1500)
                get().send(Event.Confirm("5"))
                return null;
            }
        })

        Thread.sleep(999999)
    }

    sealed class Event {
        class Recharge(val pins: List<String>)
        class Confirm(val pin: String)
    }

    private fun spawnActor(name: String, actor: Actor): Actor {
        val fiber = Fiber("actor", actor)
        fiber.start()
        actor.register(name)
        return actor
    }
}