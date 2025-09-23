package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.service.DatamartTransformation;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.utils.ResearchStudyUtils;
import ca.uhn.fhir.model.api.IQueryParameterType;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatamartTransformationTest {

	private Repository repository;
	private DatamartTransformation transformation;

	@BeforeEach
	void setUp() {
		repository = mock(Repository.class);
		transformation = new DatamartTransformation(repository);
	}

	@Test
	void transformSuccess() {
		String listId = "LIST-123";
		ResearchStudy study = new ResearchStudy().setUrl("http://example.org/study/RS1");
		Extension ext = new Extension(ResearchStudyUtils.EXT_URL);
		Extension eval = new Extension(ResearchStudyUtils.EVAL_EXT_NAME, new Reference("List/" + listId));
		ext.addExtension(eval);
		study.addExtension(ext);

		StructureMap map = new StructureMap();

		Bundle dm = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(new Parameters()));
		when(repository.search(eq(Bundle.class), eq(Parameters.class), anyMap(), isNull())).thenReturn(dm);

		when(repository.fhirContext()).thenReturn(FhirContext.forR5());
		Binary expected = new Binary().setContentType("application/octet-stream").setData(new byte[]{1, 2, 3});
		Parameters out = new Parameters().addParameter(new Parameters.ParametersParameterComponent().setResource(expected));
		when(repository.invoke(eq("transform"), any(Parameters.class), eq(Parameters.class), isNull())).thenReturn(out);

		Binary result = transformation.transform(study, map);

		assertNotNull(result);
		assertSame(expected, result);
		verify(repository).invoke(eq("transform"), any(Parameters.class), eq(Parameters.class), isNull());
	}

	@Test
	void fetchDatamartBundleSuccess() {
		String listId = "LIST-999";
		Bundle expected = new Bundle();

		when(repository.search(eq(Bundle.class), eq(Parameters.class), anyMap(), isNull()))
			.thenReturn(expected);

		Bundle result = transformation.fetchDataMartBundle(listId);

		assertNotNull(result);
		assertEquals(expected, result);

		verify(repository).search(
			eq(Bundle.class),
			eq(Parameters.class),
			argThat((Map<String, List<IQueryParameterType>> m) -> {
				if (m == null) return false;
				List<IQueryParameterType> hasVals = m.get("_has:List:item:_id");
				if (hasVals == null || hasVals.isEmpty()) return false;
				boolean hasListId = hasVals.stream().anyMatch(p ->
					p != null && "LIST-999".equals(p.getValueAsQueryToken(null)));
				if (!hasListId) return false;

				List<IQueryParameterType> countVals = m.get("_count");
				return countVals != null && !countVals.isEmpty();
			}),
			isNull()
		);
	}
}
