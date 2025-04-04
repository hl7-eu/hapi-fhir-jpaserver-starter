package ca.uhn.fhir.jpa.starter.cohort.service.r5;

import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.Group;
import org.springframework.stereotype.Service;

@Service
public class CohorteService {

	public Group cohorting(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint dataEndpoint,
		Endpoint theTerminologyEndpoint){
		return new Group();
	}
}
