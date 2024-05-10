package pl.majchrzw.loadtester.master.broker;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@ConditionalOnClass({ConnectionFactory.class, AmqpAdmin.class, RabbitTemplate.class, RabbitProperties.class})
public class EmbeddedBrokerAutoConfiguration {
	
	/*The bean name used by the embedded broker and also for qualifying the name from other beans*/
	static final String EMBEDDED_QPID_BROKER_BEAN_NAME = "embeddedQpidBroker";
	
	@Bean(EMBEDDED_QPID_BROKER_BEAN_NAME)
	@ConditionalOnMissingBean(EmbeddedBroker.class)
	@Conditional(ProfileCondition.class)
	public EmbeddedBroker embeddedBroker() {
		return new EmbeddedBroker();
	}
	
	static class ProfileCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getEnvironment().matchesProfiles("master");
			// Zastąp "production" nazwą profilu, w którym ma być uruchamiany bean
		}
	}
}
