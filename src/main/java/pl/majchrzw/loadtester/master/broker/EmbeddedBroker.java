package pl.majchrzw.loadtester.master.broker;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.SystemLauncherListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Profile(value = "master")
public class EmbeddedBroker implements SmartLifecycle {
	
	private final SystemLauncher qpidLauncher;
	
	private Integer port;
	private final boolean isAutoStart = true;
	private String configFilePath = "qpid-config.json";
	
	private final Logger log = LoggerFactory.getLogger(EmbeddedBroker.class);
	
	private boolean running;
	
	public EmbeddedBroker() {
		this.qpidLauncher = new SystemLauncher(new SystemLauncherListener.DefaultSystemLauncherListener());
		
		if (isAutoStart) {
			start();
		}
	}
	
	@Override
	public void start() {
		log.info("Starting Embedded Qpid broker");
		try {
			this.qpidLauncher.startup(createSystemConfig());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.running = true;
		log.info("Started Embedded Qpid broker");
	}
	
	@Override
	public void stop() {
		log.info("Stopping Embedded Qpid broker");
		this.qpidLauncher.shutdown();
		this.running = false;
		log.info("Stopped Embedded Qpid broker");
	}
	
	@Override
	public void stop(final Runnable runnable) {
		log.trace("Stopping Embedded Broker Asynchronously");
		CompletableFuture
				.runAsync(this::stop)
				.thenRunAsync(runnable);
	}
	
	@Override
	public boolean isRunning() {
		return this.running;
	}
	
	@Override
	public boolean isAutoStartup() {
		return this.isAutoStart;
	}
	
	@Override
	public int getPhase() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	private Map<String, Object> createSystemConfig() {
		final Map<String, Object> attributes = new HashMap<>();
		final URL initialConfig = EmbeddedBroker.class.getClassLoader().getResource(configFilePath);
		attributes.put("type", "JSON");
		attributes.put("port", port);
		attributes.put("initialConfigurationLocation", initialConfig.toExternalForm());
		attributes.put("startupLoggedToSystemOut", true);
		
		return attributes;
	}
	
}
