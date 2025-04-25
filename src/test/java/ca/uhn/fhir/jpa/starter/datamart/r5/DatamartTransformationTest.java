package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.Repositories;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartTransformation;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.ResearchStudyUtils;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opencds.cqf.fhir.api.Repository;

import java.util.Collections;
import java.util.HashMap;
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
        ResearchStudy researchStudy = new ResearchStudy();
        researchStudy.getPhase().addCoding()
                .setSystem(ResearchStudyUtils.CUSTOM_PHASE_SYSTEM)
                .setCode(ResearchStudyUtils.POST_DATAMART);
        researchStudy.addExtension()
                .setUrl(ResearchStudyUtils.EXT_URL)
                .addExtension()
                .setUrl(ResearchStudyUtils.EVAL_EXT_NAME)
                .setValue(new Reference("List/List123"));

        StructureMap map = new StructureMap();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new ListResource());

        when(repository.fhirContext()).thenReturn(FhirContext.forR5());

        try (MockedStatic<Repositories> repo = Mockito.mockStatic(Repositories.class)) {
            repo.when(() -> Repositories.proxy(any(), anyBoolean(), (IBaseResource) any(), any(), any()))
                    .thenReturn(repository);
            when(repository.search(eq(Bundle.class), eq(ListResource.class), anyMap(), isNull())).thenReturn(bundle);

            Binary expectedBinary = new Binary();
            try (MockedStatic<DatamartTransformation> transformationMock = Mockito.mockStatic(DatamartTransformation.class)) {
                when(repository.invoke(eq("transform"), any(Parameters.class), eq(Parameters.class), isNull()))
                        .thenReturn(new Parameters().addParameter(new Parameters.ParametersParameterComponent().setResource(expectedBinary)));

                Binary result = transformation.transform(researchStudy, map);

                assertNotNull(result);
                assertSame(expectedBinary, result);
                verify(repository).invoke(eq("transform"), any(Parameters.class), eq(Parameters.class), isNull());
            }
        }
    }

    @Test
    void fetchDatamartBundleSuccess() {
        String listId = "List123";
        Bundle expectedBundle = new Bundle();

        try (MockedStatic<Repositories> repo = Mockito.mockStatic(Repositories.class)) {
            repo.when(() -> Repositories.proxy(any(), anyBoolean(), (IBaseResource) any(), any(), any()))
                    .thenReturn(repository);
            when(repository.search(eq(Bundle.class), eq(ListResource.class), anyMap(), isNull())).thenReturn(expectedBundle);

            Bundle result = transformation.fetchDataMartBundle(listId);

            assertNotNull(result);
            assertEquals(expectedBundle, result);

            Map<String, List<IQueryParameterType>> expectedParams = new HashMap<>();
            expectedParams.put("_id", Collections.singletonList(new StringParam(listId)));
            expectedParams.put("_include", Collections.singletonList(new StringParam("List:item")));
            verify(repository).search(eq(Bundle.class), eq(ListResource.class), eq(expectedParams), isNull());
        }
    }
}