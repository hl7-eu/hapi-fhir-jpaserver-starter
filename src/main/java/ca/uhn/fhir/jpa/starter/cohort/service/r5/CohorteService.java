package ca.uhn.fhir.jpa.starter.cohort.service.r5;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.*;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;

import java.util.Objects;

public class CohorteService {

	private final Repository repository;
	private final CohorteEvaluationOptions settings;

	public CohorteService(Repository repository, CohorteEvaluationOptions settings) {
		this.settings = settings;
		this.repository = Objects.requireNonNull(repository);
	}

	public Group cohorting(
		CanonicalType researchStudyUrl,
		Endpoint researchStudyEndpoint,
		Endpoint dataEndpoint,
		Endpoint terminologyEndpoint) {
		Repository repo = Repositories.proxy(repository, false, dataEndpoint, researchStudyEndpoint, terminologyEndpoint);
		Bundle b = repo.search(Bundle.class, ResearchStudy.class, Searches.byCanonical(researchStudyUrl.getCanonical()), null);
		if (b.getEntry().isEmpty()) {
			var errorMsg = String.format("Unable to find ResearchStudy with url: %s", researchStudyUrl);
			throw new ResourceNotFoundException(errorMsg);
		}
		ResearchStudy researchStudy = (ResearchStudy) b.getEntry().get(0).getResource();
		CohorteProcessor cohorteProcessor = new CohorteProcessor(repo, settings, new RepositorySubjectProvider());
		Group group = cohorteProcessor.cohorting(researchStudy, new Group());
		buildAndSaveGroup(repo, group, researchStudy);
		updateResearchStudyWithGroup(repo, researchStudy, group);
		return group;
	}

	private void updateResearchStudyWithGroup(Repository repo, ResearchStudy researchStudy, Group group) {
		researchStudy.getRecruitment().setActualGroup(new Reference(group));
		CodeableConcept phase = new CodeableConcept();
		phase.addCoding()
			.setCode("post-cohorting")
			.setSystem("https://www.centreantoinelacassagne.org/CodeSystem/COS-CustomStudyPhases");
		researchStudy.setPhase(phase);

		repo.update(researchStudy);
	}

	private void buildAndSaveGroup(Repository repo, Group group, ResearchStudy researchStudy) {
		group.setId("group" + "-" + researchStudy.getIdElement().getIdPart());
		group.setType(Group.GroupType.PERSON);
		group.setMembership(Group.GroupMembershipBasis.ENUMERATED);
		group.setActive(true);
		group.setDescription(researchStudy.getDescription());
		group.setName("Patient Eligible for: " + researchStudy.getName());
		repo.update(group);
	}
}
