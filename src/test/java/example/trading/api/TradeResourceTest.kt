package example.trading.api

import example.trading.domain.Ccy
import example.trading.domain.CcyPair
import org.junit.Test

class TradeResourceTest {

    private val resource = TradeResource()

    @Test
    public fun test() {
        resource.getQuote(CcyPair(Ccy.AUD, Ccy.USD))
    }
}