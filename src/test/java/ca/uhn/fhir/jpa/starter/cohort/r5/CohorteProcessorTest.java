package ca.uhn.fhir.jpa.starter.cohort.r5;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.jpa.starter.cohort.service.r5.CohorteEvaluationOptions;
import ca.uhn.fhir.jpa.starter.cohort.service.r5.CohorteProcessor;
import ca.uhn.fhir.jpa.starter.cohort.service.r5.RepositorySubjectProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;

class CohorteProcessorTest {

    private Repository repository;
    private CohorteEvaluationOptions options;
    private RepositorySubjectProvider subjectProvider;
    private CohorteProcessor processor;

    @BeforeEach
    void setup() {
        repository = mock(Repository.class);
        options = mock(CohorteEvaluationOptions.class);
        subjectProvider = mock(RepositorySubjectProvider.class);
        processor = new CohorteProcessor(repository, options, subjectProvider);
    }

    @Test
    void testCohortingThrowsExceptionWhenNoEligibility() {
        // Arrange
        ResearchStudy study = createResearchStudyWithoutEligibility();

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> processor.cohorting(study));
        assertTrue(exception.getMessage().contains("does not have an eligibility variable specified"));
    }

    @Test
    void testCohortingThrowsExceptionWhenEligibilityReferenceInvalid() {
        // Arrange
        ResearchStudy study = createResearchStudyWithInvalidEligibility();

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> processor.cohorting(study));
        assertTrue(exception.getMessage().contains("does not have a valid eligibility reference"));
    }

    @Test
    void testCohortingThrowsExceptionWhenLibraryNotFound() {
        // Arrange
        String libraryUrl = "http://library-url";
        ResearchStudy study = createResearchStudyWithValidEligibility("study1");

        EvidenceVariable evidenceVariable = createValidEvidenceVariable(libraryUrl);
        when(repository.read(eq(EvidenceVariable.class), any(IdType.class))).thenReturn(evidenceVariable);

        Bundle emptyBundle = new Bundle();
        when(repository.search(eq(Bundle.class), eq(Library.class), any(), isNull()))
                .thenReturn(emptyBundle);

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> processor.cohorting(study));
        assertTrue(exception.getMessage().contains("Unable to find Library with url: " + libraryUrl));
    }

    private ResearchStudy createResearchStudyWithoutEligibility() {
        ResearchStudy study = new ResearchStudy();
        study.setRecruitment(new ResearchStudy.ResearchStudyRecruitmentComponent());
        return study;
    }

    private ResearchStudy createResearchStudyWithInvalidEligibility() {
        ResearchStudy study = new ResearchStudy();
        ResearchStudy.ResearchStudyRecruitmentComponent recruitment = new ResearchStudy.ResearchStudyRecruitmentComponent();
        recruitment.setEligibility(new Reference("Patient/1"));
        study.setRecruitment(recruitment);
        return study;
    }

    private ResearchStudy createResearchStudyWithValidEligibility(String studyUrl) {
        ResearchStudy study = new ResearchStudy();
        study.setUrl(studyUrl);
        ResearchStudy.ResearchStudyRecruitmentComponent recruitment = new ResearchStudy.ResearchStudyRecruitmentComponent();
        recruitment.setEligibility(new Reference("EvidenceVariable/ev1"));
        study.setRecruitment(recruitment);
        return study;
    }

    private EvidenceVariable createValidEvidenceVariable(String libraryUrl) {
        EvidenceVariable ev = new EvidenceVariable();
        ev.setUrl("ev1");
        Extension ext = new Extension();
        ext.setUrl("http://hl7.org/fhir/StructureDefinition/cqf-library");
        ext.setValue(new CanonicalType(libraryUrl));
        ev.addExtension(ext);
        return ev;
    }
}
