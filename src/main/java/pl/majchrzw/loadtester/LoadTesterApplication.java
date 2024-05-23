package pl.majchrzw.loadtester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import pl.majchrzw.loadtester.master.MasterService;
import pl.majchrzw.loadtester.node.NodeService;

@SpringBootApplication
public class LoadTesterApplication implements CommandLineRunner {
	
	ConfigurableApplicationContext context;
	Logger logger = LoggerFactory.getLogger(LoadTesterApplication.class);
	
	public LoadTesterApplication(ConfigurableApplicationContext context) {
		this.context = context;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(LoadTesterApplication.class, args);
	}
	
	@Override
	public void run(String... args) {
		Environment env = context.getEnvironment();
		if (env.matchesProfiles("master")) {
			context.getBean(MasterService.class).run();
		} else if (env.matchesProfiles("node")) {
			context.getBean(NodeService.class).run();
		} else {
			logger.error("Wrong profile chosen - must be either 'master' or 'node'");
			context.close();
			System.exit(-1);
		}
		logger.info("Application run successful, executing shutdown now.");
		context.close();
		System.exit(0);
	}
}
