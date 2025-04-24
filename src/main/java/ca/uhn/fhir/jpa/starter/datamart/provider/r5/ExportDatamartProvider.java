package ca.uhn.fhir.jpa.starter.datamart.provider.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartExportService;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r5.model.*;
import org.opencds.cqf.fhir.utility.repository.RestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExportDatamartProvider {
	@Value("${remote.url}")
	private String remoteUrl;
	@Autowired
	private FhirContext context;

	/**
	 * Provides the implementation of the FHIR operation
	 * <a href="https://www.centreantoinelacassagne.org/OperationDefinition/OD-ExportDatamart">
	 * $export-datamart</a>.
	 * This operation export a datamart based on a ResearchStudy resource and returns a
	 * {@link Binary} containing the evaluated research-variable parameters for a list of subjects in a specific format.
	 *
	 * @param researchStudyUrl      The canonical URL of the ResearchStudy for datamart generation.
	 * @param researchStudyEndpoint The endpoint containing the ResearchStudy, inclusion variables,
	 *                              and CQL libraries defining research variables.
	 * @param dataEndpoint          Endpoint to access data referenced by the retrieval operations in the library.
	 * @param terminologyEndpoint   (Optional) Endpoint to access terminology (ValueSets, CodeSystems) referenced by the library.
	 * @param type                  The format of the datamart export (e.g., CSV, JSON, XML).
	 * @param stuctureMapUrl        The canonical URL of the StructureMap to be used for the export.
	 * @return A {@link Binary} with the evaluated datamart parameters.
	 */
	@Operation(name = "$export-datamart", idempotent = true, type = ResearchStudy.class)
	public Binary exportDatamart(
		@OperationParam(name = "researchStudyUrl") CanonicalType researchStudyUrl,
		@OperationParam(name = "researchStudyEndpoint") Endpoint researchStudyEndpoint,
		@OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
		@OperationParam(name = "type") String type,
		@OperationParam(name = "structureMapUrl") CanonicalType stuctureMapUrl,
		RequestDetails requestDetails
	) {
		DatamartExportService datamartExportService = new DatamartExportService(new RestRepository(context.getRestfulClientFactory().newGenericClient(remoteUrl)));
		return datamartExportService.exportDatamart(
			researchStudyUrl,
			researchStudyEndpoint,
			dataEndpoint,
			terminologyEndpoint,
			type,
			stuctureMapUrl
		);
	}
}