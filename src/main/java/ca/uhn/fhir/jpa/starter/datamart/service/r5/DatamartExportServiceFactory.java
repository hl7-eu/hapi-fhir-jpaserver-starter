package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface DatamartExportServiceFactory {
	DatamartExportServiceImpl create(RequestDetails requestDetails);
}
