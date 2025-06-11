package ca.uhn.fhir.jpa.starter.cdshooks.study;

import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestContextJson;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.hapi.fhir.cdshooks.api.CdsService;
import ca.uhn.hapi.fhir.cdshooks.api.CdsServiceFeedback;
import ca.uhn.hapi.fhir.cdshooks.api.json.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class StudyEligibilityCheckService {
	@CdsService(
		value = "research-eligibility-check", hook = "patient-view", title = "Research study eligibility verification.", description = "Evaluate the inclusion criteria defined in a CQL Library.", prefetch = {})
	public CdsServiceResponseJson exampleService(CdsServiceRequestJson theCdsRequest) {
		CdsServiceResponseJson response = new CdsServiceResponseJson();
		CdsServiceResponseCardJson card = new CdsServiceResponseCardJson();

		String fhirServer = theCdsRequest.getFhirServer();
		CdsServiceRequestContextJson context = theCdsRequest.getContext();
		String patientId = context.getString("patientId");
		String libraryId = context.getString("libraryId");
		String studyId = context.getString("studyId");
		String inclusionExpression = context.getString("inclusionExpression");
		String contentServer = context.getString("contentServer");
		String terminologyServer = context.getString("terminologyServer");
		String CQLEngineServer = context.getString("CQLEngineServer");

		RemoteCqlClient cqlClient = new RemoteCqlClient(CQLEngineServer);
		boolean isEligible = cqlClient.evaluateInclusionExpression(patientId, libraryId, inclusionExpression, contentServer, fhirServer, terminologyServer);

		if (isEligible) {
			card.setIndicator(CdsServiceIndicatorEnum.INFO);
			card.setSummary("Patient eligible for clinical study " + (studyId != null ? studyId : ""));
			card.setDetail("Patient meets all inclusion criteria and has no exclusion conditions. Consider including this patient in the study.");
		} else {
			card.setSummary("Patient not eligible for study " + (studyId != null ? studyId : ""));
			card.setIndicator(CdsServiceIndicatorEnum.WARNING);
			card.setDetail("Patient does not meet all inclusion criteria or has an exclusion condition.");
		}
		response.addCard(card);

		CdsServiceResponseCardSourceJson source = new CdsServiceResponseCardSourceJson();
		source.setLabel("Research Eligibility CDS Service");
		source.setUrl("https://www.centreantoinelacassagne.org");
		card.setSource(source);
		return response;
	}

	@CdsServiceFeedback("research-eligibility-check")
	public CdsServiceFeedbackJson feedback(CdsServiceFeedbackJson theFeedback) {
		theFeedback.setAcceptedSuggestions(List.of(new CdsServiceAcceptedSuggestionJson().setId(UUID.randomUUID().toString())));
		return theFeedback;
	}
}
