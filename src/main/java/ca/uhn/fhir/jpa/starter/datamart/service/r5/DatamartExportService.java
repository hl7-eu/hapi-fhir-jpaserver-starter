package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.r5.model.*;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;

import java.util.Objects;

public class DatamartExportService {

	private final Repository repository;
	private final MappingEngine mappingEngine;
	FhirContext ctx = FhirContext.forR5();
	String studyRepo = "https://integ.fyrstain.com/r5-data";
	// OR https://integ.fyrstain.com/mapping ??
	IGenericClient client = ctx.newRestfulGenericClient(studyRepo);

	public DatamartExportService(Repository repository) {
		this.repository = repository;
		this.mappingEngine = new MappingEngine();
	}

	/**
	 * Exports a Datamart BinaryResource for the referenced ResearchStudy.
	 *
	 * @param researchStudyUrl      The canonical URL of the ResearchStudy for datamart export.
	 * @param researchStudyEndpoint The endpoint containing the ResearchStudy, inclusion variables,
	 *                              and CQL libraries defining research variables.
	 * @param structureMapEndpoint          Endpoint to access data referenced by the retrieval operations in the library.
	 * @param terminologyEndpoint   (Optional) Endpoint to access terminology (ValueSets, CodeSystems) referenced by the library.
	 * @param type                  The desired export format (e.g., text/csv, application/json).
	 * @param structureMapUrl       The canonical URL of the StructureMap to apply for transformation.
	 * @return A Binary resource containing the exported datamart.
	 */
	public Binary exportDatamart(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint structureMapEndpoint,
		Endpoint terminologyEndpoint,
		String type,
		CanonicalType structureMapUrl
	) {
		Repository repo = Repositories.proxy(repository, false, structureMapEndpoint, researchStudyEndpoint, null);
		Bundle b = repo.search(Bundle.class, ResearchStudy.class, Searches.byCanonical(researchStudyUrl.getCanonical()), null);
		if (b.getEntry().isEmpty()) {
			var errorMsg = String.format("Unable to find ResearchStudy with url: %s", researchStudyUrl.getCanonical());
			throw new ResourceNotFoundException(errorMsg);
		}
		ResearchStudy researchStudy = (ResearchStudy) b.getEntry().get(0).getResource();
		String phase = researchStudy.getPhase().getCode(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM);
		if (Objects.equals(
			researchStudy.getPhase().getCode(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM), ResearchStudyUtils.POST_DATAMART)) {
			var errorMsg = String.format("A datamart generation is needed before exporting the datamart for ResearchStudy with url: %s", researchStudyUrl);
			throw new ResourceNotFoundException(errorMsg);
		}

		Bundle structureMaps = repo.search(Bundle.class, StructureMap.class, Searches.byCanonical(structureMapUrl.getCanonical()), null);
		if (b.getEntry().isEmpty()) {
			var errorMsg = String.format("Unable to find ResearchStudy with url: %s", structureMapUrl.getCanonical());
			throw new ResourceNotFoundException(errorMsg);
		}
		repo.invoke("transform", new Parameters(), Binary.class, null);
		StructureMap structureMap = (StructureMap) b.getEntry().get(0).getResource();

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
