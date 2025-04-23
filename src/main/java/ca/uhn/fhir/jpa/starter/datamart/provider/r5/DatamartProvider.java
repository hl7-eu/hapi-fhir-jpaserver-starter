package ca.uhn.fhir.jpa.starter.datamart.provider.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.DatamartEvaluationOptions;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartService;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.ListResource;
import org.hl7.fhir.r5.model.ResearchStudy;
import org.opencds.cqf.fhir.utility.repository.RestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatamartProvider {
	@Autowired
	private FhirContext context;

	/**
	 * Provides the implementation of the FHIR operation
	 * <a href="https://www.centreantoinelacassagne.org/OperationDefinition/OD-GenerateDatamart">
	 * $generate-datamart</a>.
	 * This operation generates a datamart based on a ResearchStudy resource and returns a
	 * {@link ListResource} containing the evaluated research-variable parameters for a list of subjects.
	 *
	 * @param researchStudyUrl      The canonical URL of the ResearchStudy for datamart generation.
	 * @param researchStudyEndpoint The endpoint containing the ResearchStudy, inclusion variables,
	 *                              and CQL libraries defining research variables.
	 * @param dataEndpoint          Endpoint to access data referenced by the retrieval operations in the library.
	 * @param terminologyEndpoint   (Optional) Endpoint to access terminology (ValueSets, CodeSystems) referenced by the library.
	 * @return A {@link ListResource} with the evaluated datamart parameters.
	 */
	@Operation(name = "$generate-datamart", idempotent = true, type = ResearchStudy.class)
	public ListResource generateDatamart(
		@OperationParam(name = "researchStudyUrl") CanonicalType researchStudyUrl,
		@OperationParam(name = "researchStudyEndpoint") Endpoint researchStudyEndpoint,
		@OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
		RequestDetails requestDetails
	) {
		return new DatamartService(new RestRepository(context.getRestfulClientFactory().newGenericClient(requestDetails.getFhirServerBase())), DatamartEvaluationOptions.defaultOptions()).generateDatamart(
			researchStudyUrl,
			researchStudyEndpoint,
			dataEndpoint,
			terminologyEndpoint
		);
	}
}
