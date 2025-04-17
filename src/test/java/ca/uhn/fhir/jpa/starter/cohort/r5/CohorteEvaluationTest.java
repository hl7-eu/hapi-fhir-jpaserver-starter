package ca.uhn.fhir.jpa.starter.cohort.r5;

import ca.uhn.fhir.jpa.starter.cohort.service.r5.CohorteEvaluation;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.elm.r1.Library;
import org.hl7.fhir.r5.model.EvidenceVariable;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Group;
import org.hl7.fhir.r5.model.ResearchStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationVisitor;
import org.opencds.cqf.cql.engine.execution.State;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CohorteEvaluationTest {

	@Mock
	private CqlEngine cqlEngine;
	@Mock
	private State engineState;
	@Mock
	private EvaluationVisitor evaluationVisitor;
	@Mock
	private org.hl7.elm.r1.Library elmLibrary;
	@Mock
	private Library.Statements elmStatements;
	@Mock
	private ExpressionDef expressionDef;

	private CohorteEvaluation cohorteEvaluation;
	private ResearchStudy researchStudy;
	private EvidenceVariable evidenceVariable;

	@BeforeEach
	void setUp() {
		// CQL Configuration
		when(cqlEngine.getState()).thenReturn(engineState);
		when(cqlEngine.getEvaluationVisitor()).thenReturn(evaluationVisitor);
		when(engineState.getCurrentLibrary()).thenReturn(elmLibrary);
		when(elmLibrary.getStatements()).thenReturn(elmStatements);
		when(elmStatements.getDef()).thenReturn(List.of(expressionDef));
		when(expressionDef.getName()).thenReturn("TestExpression");

		cohorteEvaluation = new CohorteEvaluation(cqlEngine);
		researchStudy = new ResearchStudy().setUrl("http://example.com/study");
		evidenceVariable = new EvidenceVariable().setUrl("http://example.com/evidence");
	}

	@Test
	void EvaluateEligiblePatientsAddsAllMembers() {
		when(evaluationVisitor.visitExpressionDef(any(), any())).thenReturn(true);

		EvidenceVariable.EvidenceVariableCharacteristicComponent characteristic =
			evidenceVariable.addCharacteristic();
		characteristic.getDefinitionByCombination()
			.addCharacteristic(new EvidenceVariable.EvidenceVariableCharacteristicComponent()
				.setDefinitionExpression(new Expression().setExpression("TestExpression")));

		List<String> subjects = List.of("Patient/123", "Patient/456");

		Group result = cohorteEvaluation.evaluate(researchStudy, evidenceVariable, subjects);

		assertEquals(2, result.getMember().size());
	}

	@Test
	void EvaluateMixedResultsAddsOnlyMatching() {
		when(evaluationVisitor.visitExpressionDef(any(), any())).thenReturn(true).thenReturn(false);

		EvidenceVariable.EvidenceVariableCharacteristicComponent characteristic =
			evidenceVariable.addCharacteristic();
		characteristic.getDefinitionByCombination()
			.addCharacteristic(new EvidenceVariable.EvidenceVariableCharacteristicComponent()
				.setDefinitionExpression(new Expression().setExpression("TestExpression")));

		List<String> subjects = List.of("Patient/123", "Patient/456");

		Group result = cohorteEvaluation.evaluate(researchStudy, evidenceVariable, subjects);

		assertEquals(1, result.getMember().size());
		assertEquals("Patient/" + cohorteEvaluation.pseudonymizeRealId("123"), result.getMemberFirstRep().getEntity().getReference());
	}

	@Test
	void EvaluateIneligiblePatientsAddsNone() {
		when(evaluationVisitor.visitExpressionDef(any(), any())).thenReturn(false);
		EvidenceVariable.EvidenceVariableCharacteristicComponent characteristic =
			evidenceVariable.addCharacteristic();
		characteristic.getDefinitionByCombination()
			.addCharacteristic(new EvidenceVariable.EvidenceVariableCharacteristicComponent()
				.setDefinitionExpression(new Expression().setExpression("TestExpression")));

		List<String> subjects = List.of("Patient/123", "Patient/456");

		Group result = cohorteEvaluation.evaluate(researchStudy, evidenceVariable, subjects);

		assertTrue(result.getMember().isEmpty());
	}

	@Test
	void EvaluateExpressionValidReturnsBooleanResult() {
		when(expressionDef.getName()).thenReturn("MyCriteria");
		when(evaluationVisitor.visitExpressionDef(any(), any())).thenReturn(false);

		Object result = cohorteEvaluation.evaluateDefinitionExpression("MyCriteria");

		assertFalse((Boolean) result);
	}
}