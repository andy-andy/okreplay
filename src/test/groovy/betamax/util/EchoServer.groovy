package betamax.util

import groovy.util.logging.Log4j
import java.util.concurrent.CountDownLatch
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener
import org.eclipse.jetty.util.component.LifeCycle
import static java.net.HttpURLConnection.HTTP_OK
import javax.servlet.http.*
import org.eclipse.jetty.server.*

@Log4j
class EchoServer extends AbstractLifeCycleListener {

	private final String host
	private final int port
	private Server server
	private CountDownLatch startedLatch
	private CountDownLatch stoppedLatch

	EchoServer() {
		host = InetAddress.localHost.hostAddress
		port = 5000
	}

	String getUrl() {
		"http://$host:$port/"
	}

	void start() {
		startedLatch = new CountDownLatch(1)
		stoppedLatch = new CountDownLatch(1)

		server = new Server(port)
		server.handler = new EchoHandler()
		server.addLifeCycleListener(this)
		server.start()

		startedLatch.await()
	}

	void stop() {
		if (server) {
			server.stop()
			stoppedLatch.await()
		}
	}

	@Override
	void lifeCycleStarted(LifeCycle event) {
		startedLatch.countDown()
	}

	@Override
	void lifeCycleStopped(LifeCycle event) {
		stoppedLatch.countDown()
	}

}

@Log4j
class EchoHandler extends AbstractHandler {

	void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		log.debug "received $request.method request for $target"
		response.status = HTTP_OK
		response.contentType = "text/plain"
		response.writer.withWriter { writer ->
			writer << request.method << " " << request.requestURI
			if (request.queryString) {
				writer << "?" << request.queryString
			}
			writer << " " << request.protocol << "\n"
			for (headerName in request.headerNames) {
				for (header in request.getHeaders(headerName)) {
					writer << headerName << ": " << header << "\n"
				}
			}
			request.reader.withReader { reader ->
				while (reader.ready()) {
					writer << (char) reader.read()
				}
			}
		}
	}

}