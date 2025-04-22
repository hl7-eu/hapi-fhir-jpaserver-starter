package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import org.hl7.fhir.r5.model.Binary;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.ListResource;
import org.springframework.stereotype.Service;

@Service
public class DatamartService {

	public ListResource generateDatamart(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint dataEndpoint,
		Endpoint terminologyEndpoint
	) {
		return new ListResource();
	}

	public Binary exportDatamart(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint dataEndpoint,
		Endpoint terminologyEndpoint,
		String type,
		CanonicalType stuctureMapUrl
	) {
		return new Binary();
	}
}
