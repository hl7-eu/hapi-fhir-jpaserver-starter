package ca.uhn.fhir.jpa.starter.cohort.service.r5;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.ExpressionDef;
import org.hl7.fhir.r5.model.EvidenceVariable;
import org.hl7.fhir.r5.model.Group;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.ResearchStudy;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CohorteEvaluation {
	private static final Logger logger = LoggerFactory.getLogger(CohorteEvaluation.class);
	private final CqlEngine context;

	public CohorteEvaluation(CqlEngine context) {
		this.context = context;
	}

	protected Group evaluate(ResearchStudy researchStudy, EvidenceVariable evidenceVariable, List<String> subjectIds, Group group) {
		logger.info("Evaluating Cohort {} with {} subject(s)", researchStudy.getUrl(), subjectIds.size());

		for (String subjectId : subjectIds) {
			if (subjectId == null) {
				throw new RuntimeException("SubjectId is required in order to calculate.");
			}

			Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
			String subjectTypePart = subjectInfo.getLeft();
			String subjectIdPart = subjectInfo.getRight();
			this.context.getState().setContextValue(subjectTypePart, subjectIdPart);
			this.evaluateVariableDefinition(group, evidenceVariable, subjectTypePart, subjectIdPart);
		}

		return group;
	}

	protected void evaluateVariableDefinition(Group group, EvidenceVariable evidenceVariable, String subjectType, String subjectId) {
		String definitionExpression = evidenceVariable.getCharacteristic().get(0).getDefinitionByCombination().getCharacteristic().get(0).getDefinitionExpression().getExpression();
		if (definitionExpression != null && !definitionExpression.isEmpty()) {
			Object result = this.evaluateDefinitionExpression(definitionExpression);
			if (result instanceof Boolean) {
				if (Boolean.TRUE.equals(result)) {
					group.addMember().setEntity(new Reference(String.format("%s/%s", subjectType, subjectId)));
				}
			}
		}
	}

	protected Object evaluateDefinitionExpression(String criteriaExpression) {
		ExpressionDef ref = Libraries.resolveExpressionRef(criteriaExpression, this.context.getState().getCurrentLibrary());
		Object result = this.context.getEvaluationVisitor().visitExpressionDef(ref, this.context.getState());
		return result;
	}

	protected Pair<String, String> getSubjectTypeAndId(String subjectId) {
		if (subjectId.contains("/")) {
			String[] subjectIdParts = subjectId.split("/");
			return Pair.of(subjectIdParts[0], subjectIdParts[1]);
		} else {
			throw new IllegalArgumentException(String.format("Unable to determine Subject type for id: %s. SubjectIds must be in the format {subjectType}/{subjectId} (e.g. Patient/123)", subjectId));
		}
	}

}
