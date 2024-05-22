package pl.majchrzw.loadtester;

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
			// TODO - tutaj wykonuje się część mastera
			context.getBean(MasterService.class).run();
			// TODO - trzeba wczytać zadanie z JSONa, podzielić, wysłać do node-ów, po otrzymaniu ACK, startują zapytania
		} else if (env.matchesProfiles("node")) {
			// TODO - tutaj wykonuje się część node-a
			context.getBean(NodeService.class).run();
			// TODO - node oczekuje na konfigurację od mastera, po wysłaniu ACK zaczyna wysyłać zapytania
		} else {
			context.close();
			System.out.println("Wrong profile - must be either 'master' or 'node'");
			System.exit(-1);
		}
		context.close();
		System.exit(0);
	}
}
