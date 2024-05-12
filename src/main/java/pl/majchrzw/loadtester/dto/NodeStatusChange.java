package pl.majchrzw.loadtester.dto;

import java.util.UUID;

public record NodeStatusChange(
		UUID id,
		Action action
) {
}
