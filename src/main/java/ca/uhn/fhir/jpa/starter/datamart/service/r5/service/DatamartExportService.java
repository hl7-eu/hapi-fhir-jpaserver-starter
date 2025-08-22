package ca.uhn.fhir.jpa.starter.datamart.service.r5.service;

import ca.uhn.fhir.jpa.starter.datamart.service.Repositories;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.utils.ResearchStudyUtils;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.impl.DatamartExportServiceImpl;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.*;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;

import java.util.Objects;

public class DatamartExportService implements DatamartExportServiceImpl {

    private final Repository repository;

    public DatamartExportService(Repository repository) {
        this.repository = repository;
    }

    /**
     * Exports a datamart as a FHIR Binary resource for the specified ResearchStudy.
     *
     * @param researchStudyUrl      the canonical URL of the ResearchStudy to export
     * @param researchStudyEndpoint the FHIR Endpoint containing the ResearchStudy,
     *                              associated CQL libraries, and inclusion criteria
     * @param structureMapEndpoint  the FHIR Endpoint used to resolve StructureMap resources
     *                              referenced in the transformation
     * @param terminologyEndpoint   (optional) the FHIR Endpoint used to resolve terminology
     *                              resources (ValueSet, CodeSystem) referenced in CQL
     * @param type                  the desired export MIME type (e.g. "text/csv" or "application/json")
     * @param structureMapUrl       the canonical URL of the StructureMap to apply
     * @return a Binary resource containing the exported datamart
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
        if (!Objects.equals(
                researchStudy.getPhase().getCode(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM), ResearchStudyUtils.POST_DATAMART)) {
            var errorMsg = String.format("A datamart generation is needed before exporting the datamart for ResearchStudy with url: %s", researchStudyUrl);
            throw new ResourceNotFoundException(errorMsg);
        }

        Bundle structureMaps = repo.search(Bundle.class, StructureMap.class, Searches.byCanonical(structureMapUrl.getCanonical()), null);
        if (structureMaps.getEntry().isEmpty()) {
            var errorMsg = String.format("Unable to find StructureMap with url: %s", structureMapUrl.getCanonical());
            throw new ResourceNotFoundException(errorMsg);
        }
        StructureMap structureMap = (StructureMap) structureMaps.getEntry().get(0).getResource();

        return new DatamartTransformation(repo).transform(researchStudy, structureMap);
    }
}
