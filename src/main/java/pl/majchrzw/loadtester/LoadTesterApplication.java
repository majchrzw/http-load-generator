package pl.majchrzw.loadtester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import pl.majchrzw.loadtester.master.MasterService;
import pl.majchrzw.loadtester.node.NodeService;

@SpringBootApplication
public class LoadTesterApplication {
	
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LoadTesterApplication.class, args);
		
		Environment env = context.getEnvironment();
		if (env.matchesProfiles("master")) {
			// TODO - tutaj wykonuje się część mastera
			MasterService masterService = context.getBean(MasterService.class);
			// TODO - trzeba wczytać zadanie z JSONa, podzielić, wysłać do node-ów, po otrzymaniu ACK, startują zapytania
			masterService.run();
		} else if (env.matchesProfiles("node")) {
			// TODO - tutaj wykonuje się część node-a
			NodeService nodeService = context.getBean(NodeService.class);
			// TODO - node oczekuje na konfigurację od mastera, po wysłaniu ACK zaczyna wysyłać zapytania
		}
	}
	
}
