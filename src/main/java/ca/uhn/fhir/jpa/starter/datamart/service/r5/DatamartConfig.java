package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.cr.common.RepositoryFactoryForRepositoryInterface;
import ca.uhn.fhir.cr.config.CrBaseConfig;
import ca.uhn.fhir.cr.config.RepositoryConfig;
import ca.uhn.fhir.jpa.starter.datamart.provider.r5.DatamartProvider;
import ca.uhn.fhir.jpa.starter.datamart.provider.r5.ExportDatamartProvider;
import ca.uhn.fhir.jpa.starter.datamart.service.DatamartEvaluationOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Configuration
@Import({RepositoryConfig.class, CrBaseConfig.class})
public class DatamartConfig {

	@Bean
	DatamartServiceFactory datamartServiceFactory(@Lazy RepositoryFactoryForRepositoryInterface theRepositoryFactory) {
		return (rd) -> new DatamartService(theRepositoryFactory.create(rd), DatamartEvaluationOptions.defaultOptions());
	}

	@Bean
	public DatamartProvider datamartOperationProvider(DatamartServiceFactory datamartServiceFactory) {
		return new DatamartProvider(datamartServiceFactory);
	}

	@Bean
	DatamartExportServiceFactory datamartExportServiceFactory(@Lazy RepositoryFactoryForRepositoryInterface theRepositoryFactory) {
		return (rd) -> new DatamartExportService(theRepositoryFactory.create(rd));
	}

	@Bean
	public ExportDatamartProvider datamartExportOperationProvider(DatamartExportServiceFactory datamartExportServiceFactory) {
		return new ExportDatamartProvider(datamartExportServiceFactory);
	}
}
