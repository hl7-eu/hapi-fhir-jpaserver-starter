package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.jpa.starter.datamart.service.Repositories;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartExportService;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartTransformation;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.ResearchStudyUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatamartExportServiceTest {

	@Mock
	private Repository repository;

	private DatamartExportService service;
	private CanonicalType studyUrl;
	private Endpoint studyEndpoint;
	private Endpoint mapEndpoint;
	private Endpoint termEndpoint;
	private String type;
	private CanonicalType mapUrl;

	@BeforeEach
	void setUp() {
		service = new DatamartExportService(repository);
		studyUrl = new CanonicalType("http://example.com/study");
		studyEndpoint = new Endpoint();
		mapEndpoint = new Endpoint();
		termEndpoint = new Endpoint();
		type = "application/json";
		mapUrl = new CanonicalType("http://example.com/StructureMap/map1");
	}

	@Test
	void exportDatamartResearchStudyNotFound() {
		try (MockedStatic<Repositories> repo = Mockito.mockStatic(Repositories.class)) {
			repo.when(() -> Repositories.proxy(any(), anyBoolean(), (IBaseResource) any(), any(), any()))
				.thenReturn(repository);
			when(repository.search(eq(Bundle.class), eq(ResearchStudy.class), any(), isNull()))
				.thenReturn(new Bundle());

			ResourceNotFoundException ex = assertThrows(
				ResourceNotFoundException.class,
				() -> service.exportDatamart(studyUrl, studyEndpoint, mapEndpoint, termEndpoint, type, mapUrl)
			);
			assertTrue(ex.getMessage().contains("Unable to find ResearchStudy with url: " + studyUrl.getCanonical()));
		}
	}

	@Test
	void exportDatamartNotPostDatamartPhaseException() {
		ResearchStudy researchStudy = new ResearchStudy().setUrl(studyUrl.getValue());
		researchStudy.getPhase().addCoding()
			.setSystem(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM)
			.setCode(ResearchStudyUtils.INITIAL_PHASE);
		Bundle studyBundle = new Bundle();
		studyBundle.addEntry(new Bundle.BundleEntryComponent().setResource(researchStudy));

		try (MockedStatic<Repositories> repo = Mockito.mockStatic(Repositories.class)) {
			repo.when(() -> Repositories.proxy(any(), anyBoolean(), any(), (IBaseResource) any(), any()))
				.thenReturn(repository);
			when(repository.search(eq(Bundle.class), eq(ResearchStudy.class), any(), isNull()))
				.thenReturn(studyBundle);

			ResourceNotFoundException ex = assertThrows(
				ResourceNotFoundException.class,
				() -> service.exportDatamart(studyUrl, studyEndpoint, mapEndpoint, termEndpoint, type, mapUrl)
			);
			assertTrue(ex.getMessage().contains("A datamart generation is needed before exporting the datamart"));
		}
	}

	@Test
	void exportDatamartStructureMapNotFound() {
		ResearchStudy researchStudy = new ResearchStudy().setUrl(studyUrl.getValue());
		researchStudy.getPhase().addCoding()
			.setSystem(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM)
			.setCode(ResearchStudyUtils.POST_DATAMART);
		Bundle studyBundle = new Bundle();
		studyBundle.addEntry(new Bundle.BundleEntryComponent().setResource(researchStudy));

		try (MockedStatic<Repositories> reps = Mockito.mockStatic(Repositories.class)) {
			reps.when(() -> Repositories.proxy(any(), anyBoolean(), (IBaseResource) any(), any(), any()))
				.thenReturn(repository);
			when(repository.search(eq(Bundle.class), eq(ResearchStudy.class), any(), isNull()))
				.thenReturn(studyBundle);
			when(repository.search(eq(Bundle.class), eq(StructureMap.class), any(), isNull()))
				.thenReturn(new Bundle());

			ResourceNotFoundException ex = assertThrows(
				ResourceNotFoundException.class,
				() -> service.exportDatamart(studyUrl, studyEndpoint, mapEndpoint, termEndpoint, type, mapUrl)
			);
			assertTrue(ex.getMessage().contains("Unable to find StructureMap with url: " + mapUrl.getCanonical()));
		}
	}

	@Test
	void exportDatamartSuccess() {
		ResearchStudy researchStudy = new ResearchStudy().setUrl(studyUrl.getValue());
		researchStudy.getPhase().addCoding()
			.setSystem(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM)
			.setCode(ResearchStudyUtils.POST_DATAMART);
		researchStudy.addExtension()
			.setUrl(ResearchStudyUtils.EXT_URL)
			.addExtension()
			.setUrl(ResearchStudyUtils.EVAL_EXT_NAME)
			.setValue(new Reference("List/List123"));
		Bundle studyBundle = new Bundle();
		studyBundle.addEntry(new Bundle.BundleEntryComponent().setResource(researchStudy));
		StructureMap structureMap = new StructureMap().setUrl(mapUrl.getValue());
		Bundle structureMapBundle = new Bundle();
		structureMapBundle.addEntry(new Bundle.BundleEntryComponent().setResource(structureMap));

		try (MockedStatic<Repositories> reps = Mockito.mockStatic(Repositories.class)) {
			reps.when(() -> Repositories.proxy(any(), anyBoolean(), (IBaseResource) any(), any(), any()))
				.thenReturn(repository);
			when(repository.search(eq(Bundle.class), eq(ResearchStudy.class), any(), isNull()))
				.thenReturn(studyBundle);
			when(repository.search(eq(Bundle.class), eq(StructureMap.class), any(), isNull()))
				.thenReturn(structureMapBundle);

			Binary expectedBinary = new Binary();

			try (MockedConstruction<DatamartTransformation> cons = Mockito.mockConstruction(DatamartTransformation.class,
				(mockTrans, ctx) -> {
					when(mockTrans.transform(eq(researchStudy), eq(structureMap))).thenReturn(expectedBinary);
				})) {
				Binary result = service.exportDatamart(studyUrl, studyEndpoint, mapEndpoint, termEndpoint, type, mapUrl);
				assertSame(expectedBinary, result);
				assertEquals(1, cons.constructed().size());
			}
		}
	}
}
