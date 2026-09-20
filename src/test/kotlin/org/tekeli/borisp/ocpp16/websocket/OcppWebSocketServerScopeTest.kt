package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.arc.Arc
import io.quarkus.test.junit.QuarkusTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class OcppWebSocketServerScopeTest {

    @Test
    fun `bean scope is not application scoped or singleton`() {
        val beanManager = Arc.container().beanManager()
        val bean = beanManager.resolve(beanManager.getBeans(OcppWebSocketServer::class.java))
        val scope = bean.scope
        assertNotEquals(ApplicationScoped::class.java, scope)
        assertNotEquals(Singleton::class.java, scope)
    }

    @Test
    fun `two lookups return distinct instances`() {
        val first = Arc.container().instance(OcppWebSocketServer::class.java).get()
        val second = Arc.container().instance(OcppWebSocketServer::class.java).get()
        assertNotSame(first, second)
    }
}
