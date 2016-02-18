package testgrp

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.strands.channels.Channels

object QuasarIncreasingEchoKApp {
    //@Throws(ExecutionException::class, InterruptedException::class)
    fun doAll(): Int {
        val increasingToEcho = Channels.newIntChannel(0) // Synchronizing channel (buffer = 0)
        val echoToIncreasing = Channels.newIntChannel(0) // Synchronizing channel (buffer = 0)

        val increasing = fiber @Suspendable {
            var curr = 0
            for (i in 0..9) {
                Fiber.sleep(1000)
                println("INCREASER sending: " + curr)
                increasingToEcho.send(curr)
                curr = echoToIncreasing.receive()
                println("INCREASER received: " + curr)
                curr++
                println("INCREASER now: " + curr)
            }
            println("INCREASER closing channel and exiting")
            increasingToEcho.close()
            curr
        }

        val echo = fiber @Suspendable {
            while (!echoToIncreasing.isClosed) {
                Fiber.sleep(1000)
                val curr = increasingToEcho.receive()
                println("ECHO received: $curr")

                if (curr != null) {
                    println("ECHO sending: $curr")
                    echoToIncreasing.send(curr)
                } else {
                    println("ECHO detected closed channel, closing and exiting")
                    echoToIncreasing.close()
                }
            }
        }

        try {
            increasing.join()
            echo.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return increasing.get()
    }
}

fun main(args: Array<String>) {
    QuasarIncreasingEchoKApp.doAll()
}

