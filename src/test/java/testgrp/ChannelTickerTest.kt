package testgrp

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.strands.SuspendableAction2
import co.paralleluniverse.strands.channels.Channels.*
import co.paralleluniverse.strands.channels.ReceivePort
import co.paralleluniverse.strands.channels.SendPort
import org.junit.Test

class ChannelTickerTest {

    @Test
    fun test() {
        val rawStockPriceChannel = newChannel<Int>(1, OverflowPolicy.DISPLACE)
        val outboundStocker = newChannel<String>(1)

        val fiberOne = fiber @Suspendable {
            IntRange(0, 10).forEach {
                rawStockPriceChannel.send(it)
                Fiber.sleep(500)
            }
            rawStockPriceChannel.close()
        }

        val fiberTwo = fiber @Suspendable {
            val mapped = map(newTickerConsumerFor(rawStockPriceChannel), { it?.times(2) })

            while (!mapped.isClosed) {
                val nxt = mapped.receive()
                println("time2 nxt: $nxt")
                if (nxt == null) mapped.close()
            }
        }

        val fiberThree = fiber @Suspendable {
            val mapped = map(newTickerConsumerFor(rawStockPriceChannel), { it?.times(3) })

            while (!mapped.isClosed) {
                val nxt = mapped.receive()
                println("time3 nxt: $nxt")
                if (nxt == null) mapped.close()
            }
        }

        val fiberFour = fiber @Suspendable {
            fiberTransform(newTickerConsumerFor(rawStockPriceChannel), outboundStocker,
                    SuspendableAction2 { inChannel: ReceivePort<in Int?>, outChannel: SendPort<String?> ->
                        outChannel.send("r: ${inChannel.receive()}")
                    })
        }

        fiberOne.join()
        fiberTwo.join()
        fiberThree.join()
        fiberFour.join()
    }
}