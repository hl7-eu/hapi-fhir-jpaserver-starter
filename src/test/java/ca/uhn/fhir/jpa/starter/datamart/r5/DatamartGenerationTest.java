package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.DatamartGeneration;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.State;
import org.opencds.cqf.fhir.api.Repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatamartGenerationTest {

    @Mock
    private CqlEngine cqlEngine;
    @Mock
    private Repository repository;
    @Mock
    private MethodOutcome methodOutcome;
    @Mock
    private State engineState;


    private DatamartGeneration service;
    private ResearchStudy researchStudy;
    private EvidenceVariable evidenceVariable;
    private VersionedIdentifier versionedIdentifier;

    @BeforeEach
    void setUp() {
        service = new DatamartGeneration(cqlEngine, repository);

        researchStudy = new ResearchStudy().setUrl("http://example.com/study");
        evidenceVariable = new EvidenceVariable().setUrl("http://example.com/evidence");
        versionedIdentifier = new VersionedIdentifier()
                .withId("TestLib")
                .withVersion("1.0.0");
    }

    @Test
    void generateDatamartAddsOneEntryPerSubject() {
        //CQL configuration
        when(cqlEngine.getState()).thenReturn(engineState);
        when(cqlEngine.evaluate(eq(versionedIdentifier), eq(Collections.singleton("MyExpression"))))
                .thenReturn(new EvaluationResult());
        when(repository.fhirContext()).thenReturn(FhirContext.forR5());


        List<String> subjects = List.of("Patient/123", "Patient/456");

        when(repository.create(any(Parameters.class))).thenReturn(methodOutcome);

        var id = new IdType("Parameters", "999");
        when(methodOutcome.getId()).thenReturn(id);

        evidenceVariable.addCharacteristic()
                .getDefinitionByCombination()
                .addCharacteristic(new EvidenceVariable.EvidenceVariableCharacteristicComponent()
                        .setDefinitionExpression(new Expression().setExpression("MyExpression")));

        ListResource result = service.generateDatamart(
                researchStudy,
                evidenceVariable,
                subjects,
                versionedIdentifier
        );

        assertEquals(2, result.getEntry().size());
        assertEquals("Parameters/999", result.getEntryFirstRep().getItem().getReference());
    }

    @Test
    void generateDatamartWithNullSubjectThrows() {
        List<String> subjects = Arrays.asList(null, "Patient/123");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.generateDatamart(
                        researchStudy,
                        evidenceVariable,
                        subjects,
                        versionedIdentifier
                )
        );
        assertTrue(ex.getMessage().contains("SubjectId is required"));
    }

    @Test
    void evaluateVariableDefinitionMissingExpressionThrows() {
        ListResource listParams = new ListResource();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluateVariableDefinition(
                        listParams,
                        evidenceVariable,
                        "Patient",
                        "123",
                        versionedIdentifier
                )
        );
        assertTrue(ex.getMessage().contains("DefinitionExpression is missing"));
    }

	@Test
	void evaluateVariableDefinitionWithNoCharacteristics() {
		ListResource listParams = new ListResource();
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.evaluateVariableDefinition(listParams, evidenceVariable, "Patient", "123", versionedIdentifier)
		);
		assertTrue(ex.getMessage().contains("DefinitionExpression is missing"));
	}

    @Test
    void evaluateDefinitionExpressionValidExpression() {
        when(repository.fhirContext()).thenReturn(FhirContext.forR5());
        when(cqlEngine.evaluate(eq(versionedIdentifier), eq(Collections.singleton("Expr"))))
                .thenReturn(new EvaluationResult());

        Parameters params = service.evaluateDefinitionExpression("Expr", versionedIdentifier);

        verify(cqlEngine).evaluate(versionedIdentifier, Collections.singleton("Expr"));
        assertNotNull(params);
    }

    @Test
    void getSubjectTypeAndIdValid() {
        Pair<String, String> pair = service.getSubjectTypeAndId("Patient/ABC");
        assertEquals("Patient", pair.getLeft());
        assertEquals("ABC", pair.getRight());
    }

    @Test
    void getSubjectTypeAndIdInvalidThrows() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getSubjectTypeAndId("NoSlash")
        );
        assertTrue(ex.getMessage().contains("SubjectIds must be in the format"));
    }
}
