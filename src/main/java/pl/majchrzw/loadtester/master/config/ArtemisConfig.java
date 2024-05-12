package pl.majchrzw.loadtester.master.config;

import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("master")
@Configuration
public class ArtemisConfig implements ArtemisConfigurationCustomizer {
	@Override
	public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {
		try {
			configuration.addAcceptorConfiguration("remote", "tcp://0.0.0.0:61616");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
