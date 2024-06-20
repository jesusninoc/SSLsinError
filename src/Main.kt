import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

fun main() {
    val keystoreFile = File("keystore.pfx")
    val keystorePassword = "123456789"

    // Load the keystore
    val keyStore = KeyStore.getInstance("PKCS12").apply {
        load(keystoreFile.inputStream(), keystorePassword.toCharArray())
    }

    // Create an SSL context
    val sslContext = SSLContext.getInstance("TLS").apply {
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, keystorePassword.toCharArray())
        }
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }
        init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
    }

    val environment = applicationEngineEnvironment {
        sslConnector(
            keyStore = keyStore,
            keyAlias = keyStore.aliases().nextElement(), // Use the first alias
            keyStorePassword = { keystorePassword.toCharArray() },
            privateKeyPassword = { keystorePassword.toCharArray() }
        ) {
            port = 8443
            keyStorePath = keystoreFile.absoluteFile
        }

        module(Application::module)
    }

    embeddedServer(Netty, environment).start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    routing {
        get("/") {
            call.respondText("<html><body><h1>Hello, world!</h1></body></html>", ContentType.Text.Html)
        }
    }
}
