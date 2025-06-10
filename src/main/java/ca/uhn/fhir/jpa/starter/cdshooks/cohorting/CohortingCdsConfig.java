package ca.uhn.fhir.jpa.starter.cdshooks.cohorting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CohortingCdsConfig {

	@Bean(name = "cdsServices")
	public List<Object> cdsServices() {
		List<Object> retVal = new ArrayList<>();
		retVal.add(new CohortingCdsService());
		return retVal;
	}
}
