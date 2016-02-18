package testgrp

import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Receive
import co.paralleluniverse.kotlin.Send
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.kotlin.select
import co.paralleluniverse.strands.channels.Channels
import org.junit.Test
import kotlin.test.assertTrue

class ChannelSelectTest {

    @Test
    fun test() {
        val ch1 = Channels.newChannel<Int>(1)
        val ch2 = Channels.newChannel<Int>(1)

        assertTrue (
                fiber @Suspendable {
                    select(Receive(ch1), Send(ch2, 2)) {
                        it
                    }
                }.get() is Send
        )

        ch1.send(1)

        assertTrue (
                fiber @Suspendable {
                    select(Receive(ch1), Send(ch2, 2)) {
                        when (it) {
                            is Receive -> it.msg
                            is Send -> 0
                            else -> -1
                        }
                    }
                }.get() == 1
        )
    }
}