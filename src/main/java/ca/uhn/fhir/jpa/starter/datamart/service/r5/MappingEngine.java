package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.*;
import java.util.Base64;

public class MappingEngine {
	FhirContext ctx = FhirContext.forR4();
	String mappingEngine = "https://janus.ovh.fyrstain.com/orchestrator";

	IGenericClient client = ctx.newRestfulGenericClient(mappingEngine);
	// OR https://integ.fyrstain.com/mapping ??

	/**
	 * Loads a StructureMap resource from a FHIR server using its canonical URL.
	 *
	 * @param structureMapUrl The canonical URL of the StructureMap to load.
	 * @return The StructureMap resource matching the provided URL.
	 * @throws ResourceNotFoundException If no StructureMap is found for the given URL.
	 */
	public StructureMap loadStructureMapByUrl(CanonicalType structureMapUrl) {
		String searchUrl = String.format("StructureMap?url=%s", structureMapUrl.getValue());
		Bundle bundle = client.search()
			.byUrl(searchUrl)
			.returnBundle(Bundle.class)
			.execute();

		if (bundle.getEntry().isEmpty()) {
			throw new ResourceNotFoundException("No StructureMap found for URL: " + structureMapUrl);
		}

		return (StructureMap) bundle.getEntryFirstRep().getResource();
	}

	/**
	 * Transforms a Bundle containing a list of Parameters using the provided StructureMap URL.
	 *
	 * @param listResource    The Bundle containing the ListResource of Parameters to transform.
	 * @param structureMapUrl The canonical URL of the StructureMap to apply.
	 * @return The transformed ListResource.
	 * @throws ResourceNotFoundException If the StructureMap cannot be found.
	 */
	public Binary transform(Bundle listResource, CanonicalType structureMapUrl) {
		// Load the StructureMap using the MappingEngine
		StructureMap structureMap = loadStructureMapByUrl(structureMapUrl);

		// Convert the Bundle to a JSON string
		String bundleJson = FhirContext.forR4().newJsonParser().encodeResourceToString(listResource);
		// Encode the JSON string in Base64
		String base64EncodedBundle = Base64.getEncoder().encodeToString(bundleJson.getBytes());

		// Create the Binary resource
		Binary binaryInput = new Binary();
		binaryInput.setContentType("application/json");
		binaryInput.setData(bundleJson.getBytes());

		// Create the input parameters to pass to the server
		Parameters inParams = new Parameters();
		inParams.addParameter().setName("structureMap").setResource(structureMap);
		inParams.addParameter().setName("input").addPart().setName("bundle").setResource(binaryInput);

		// Invoke $tranform operation on the server"
		Parameters outParams = client.operation()
			.onServer()
			.named("$transform")
			.withParameters(inParams)
			.execute();

		return (Binary) outParams.getParameterFirstRep().getResource();
	}
}