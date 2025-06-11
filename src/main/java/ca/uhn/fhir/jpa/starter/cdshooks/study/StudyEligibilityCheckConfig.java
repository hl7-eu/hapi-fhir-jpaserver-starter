package ca.uhn.fhir.jpa.starter.cdshooks.study;

import ca.uhn.fhir.jpa.starter.cdshooks.CdsHooksServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class StudyEligibilityCheckConfig {

	@Bean(name = "cdsServices")
	public List<Object> cdsServices() {
		List<Object> retVal = new ArrayList<>();
		retVal.add(new StudyEligibilityCheckService());
		return retVal;
	}

	@Bean
	public ServletRegistrationBean<CdsHooksServlet> cdsHooksServlet() {
		ServletRegistrationBean<CdsHooksServlet> reg = new ServletRegistrationBean<>(
				new CdsHooksServlet(), "/cds-services/*"
		);
		reg.setName("cdsServices");
		// Si nécessaire, vous pouvez passer en init-param le nom de votre @Configuration
		reg.addInitParameter(
				"springContextConfigClass",
				"com.exemple.cds.CdsHooksConfig"
		);
		return reg;
	}
}
