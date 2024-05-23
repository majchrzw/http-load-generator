package pl.majchrzw.loadtester.dto.config;

public record ConfigValidationStatus(
		boolean valid,
		String message
) {
}
