package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.r5.model.*;

import java.util.Objects;

public class DatamartExportService {
	private final MappingEngine mappingEngine;
	FhirContext ctx = FhirContext.forR5();
	String studyRepo = "https://integ.fyrstain.com/r5-data";
	// OR https://integ.fyrstain.com/mapping ??
	IGenericClient client = ctx.newRestfulGenericClient(studyRepo);

	public DatamartExportService() {
		this.mappingEngine = new MappingEngine();
	}

	/**
	 * Exports a Datamart BinaryResource for the referenced ResearchStudy.
	 *
	 * @param researchStudyUrl      The canonical URL of the ResearchStudy for datamart export.
	 * @param researchStudyEndpoint The endpoint containing the ResearchStudy, inclusion variables,
	 *                              and CQL libraries defining research variables.
	 * @param dataEndpoint          Endpoint to access data referenced by the retrieval operations in the library.
	 * @param terminologyEndpoint   (Optional) Endpoint to access terminology (ValueSets, CodeSystems) referenced by the library.
	 * @param type                  The desired export format (e.g., text/csv, application/json).
	 * @param structureMapUrl       The canonical URL of the StructureMap to apply for transformation.
	 * @return A Binary resource containing the exported datamart.
	 */
	public Binary exportDatamart(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint dataEndpoint,
		Endpoint terminologyEndpoint,
		String type,
		CanonicalType structureMapUrl
	) {

		// Step 2: Retrieve the ResearchStudy
		String searchUrl = String.format("ResearchStudy?url=%s", researchStudyUrl.getValue());
		Bundle b = client.search().byUrl(searchUrl).returnBundle(Bundle.class).execute();
		if (b.getEntry().isEmpty()) {
			var errorMsg = String.format("Unable to find ResearchStudy with url: %s", researchStudyUrl.getValue());
			throw new ResourceNotFoundException(errorMsg);
		}
		ResearchStudy researchStudy = (ResearchStudy) b.getEntry().get(0).getResource();

		// Step 3: Validate the study phase
		if (Objects.equals(
			researchStudy.getPhase().getCode(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM), ResearchStudyUtils.POST_DATAMART)) {
			var errorMsg = String.format("A datamart is needed before exporting the datamart for ResearchStudy with url: %s", researchStudyUrl);
			throw new ResourceNotFoundException(errorMsg);
		}

		DatamartExportation datamartExportation = new DatamartExportation(mappingEngine, client);

		// Convert the R5 CanonicalType to an R5 CanonicalType
		org.hl7.fhir.r4.model.CanonicalType structureMapUrlR4 = (org.hl7.fhir.r4.model.CanonicalType) VersionConvertorFactory_40_50.convertType(structureMapUrl);

		org.hl7.fhir.r4.model.Binary export = datamartExportation.exportDatamart(researchStudy, type, structureMapUrlR4);

		// Convert the R4 Binary to an R5 Binary
		org.hl7.fhir.r5.model.Resource convertedBinaryResource = VersionConvertorFactory_40_50.convertResource(export);
		org.hl7.fhir.r5.model.Binary exportR5;
		if (convertedBinaryResource instanceof org.hl7.fhir.r5.model.Binary) {
			exportR5 = (org.hl7.fhir.r5.model.Binary) convertedBinaryResource;
		} else {
			throw new IllegalArgumentException("Converted resource is not of type Binary");
		}
		return exportR5;
	}
}
