package pl.majchrzw.loadtester.dto;

import java.util.UUID;

public record NodeExecutionStatistics (
		UUID nodeId
		// TODO - tutaj będzie reszta statystyk dotyczących wysłanych request-ów przez jakiś node
){
}