package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.Repositories;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartExportService;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartTransformation;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.ResearchStudyUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatamartExportServiceTest {

	@Mock
	private Repository repository;
	@Mock
	private DatamartExportService service;
	@Mock
	private DatamartTransformation transformation;
	private Endpoint rsEndpoint;
	private Endpoint dataEndpoint;
	private Endpoint termEndpoint;

	@BeforeEach
	void setUp() {
		service = new DatamartExportService(repository);
		rsEndpoint = new Endpoint();
		dataEndpoint = new Endpoint();
		termEndpoint = new Endpoint();
	}

	@Test
	void exportDatamartResearchStudyNotFound() {
		CanonicalType researchStudyUrl = new CanonicalType("http://example.com/study");
		when(repository.search(any(), eq(ResearchStudy.class), any(), any()))
			.thenReturn(new Bundle());

		ResourceNotFoundException exception = assertThrows(
			ResourceNotFoundException.class,
			() -> service.exportDatamart(researchStudyUrl, null, null, null, "application/json", null)
		);

		assertTrue(exception.getMessage().contains("Unable to find ResearchStudy with url"));
	}

	@Test
	void exportDatamartStructureMapNotFound() {
		CanonicalType researchStudyUrl = new CanonicalType("http://example.com/study");
		CanonicalType structureMapUrl = new CanonicalType("http://example.com/structureMap");

		Bundle researchStudyBundle = new Bundle();
		researchStudyBundle.addEntry().setResource(new ResearchStudy());
		when(repository.search(any(), eq(ResearchStudy.class), any(), any()))
			.thenReturn(researchStudyBundle);

		when(repository.search(any(), eq(StructureMap.class), any(), any()))
			.thenReturn(new Bundle());

		ResourceNotFoundException exception = assertThrows(
			ResourceNotFoundException.class,
			() -> service.exportDatamart(researchStudyUrl, null, null, null, "application/json", structureMapUrl)
		);

		assertTrue(exception.getMessage().contains("Unable to find StructureMap with url"));
	}

	@Test
	void exportDatamartSuccess() {
		CanonicalType researchStudyUrl = new CanonicalType("http://example.com/study");
		CanonicalType structureMapUrl = new CanonicalType("http://example.com/structureMap");

		EvidenceVariable evidenceVariable = new EvidenceVariable();
		evidenceVariable.setId("EV1");
		evidenceVariable.setUrl(researchStudyUrl.getValue());

		ListResource list = new ListResource();
		list.setId("List/List123");

		ResearchStudy researchStudy = new ResearchStudy();
		researchStudy.setPhase(new CodeableConcept(
			new Coding(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM, ResearchStudyUtils.POST_DATAMART, "Post-Datamart Phase")
		));
		researchStudy.setUrl(researchStudyUrl.getValue());
		researchStudy.addExtension()
			.setUrl(ResearchStudyUtils.EXT_URL)
			.addExtension()
			.setUrl(ResearchStudyUtils.EVAL_EXT_NAME)
			.setValue(new Reference("List/List123"));
		researchStudy.getRecruitment().setEligibility(new Reference("EvidenceVariable/EV1"));

		Bundle researchStudyBundle = new Bundle();
		researchStudyBundle.addEntry().setResource(researchStudy);

		StructureMap structureMap = new StructureMap();
		Bundle structureMapBundle = new Bundle();
		structureMapBundle.addEntry().setResource(structureMap);

		try (MockedStatic<Repositories> reps = Mockito.mockStatic(Repositories.class)) {
			reps.when(() -> Repositories.proxy(eq(repository), anyBoolean(), eq(dataEndpoint), eq(rsEndpoint), eq(termEndpoint)))
					.thenReturn(repository);
			when(repository.search(any(), eq(ResearchStudy.class), any(), any()))
					.thenReturn(researchStudyBundle);
			when(repository.search(any(), eq(StructureMap.class), any(), any()))
					.thenReturn(structureMapBundle);
		}


		Binary expectedBinary = new Binary();
		when(transformation.transform(any(), any())).thenReturn(expectedBinary);
		Binary result = service.exportDatamart(researchStudyUrl, null, null, null, "application/json", structureMapUrl);

		assertNotNull(result);
		verify(repository, times(1)).search(any(), eq(ResearchStudy.class), any(), any());
		verify(repository, times(1)).search(any(), eq(StructureMap.class), any(), any());
	}
}