package ca.uhn.fhir.jpa.starter.datamart.r5;

import ca.uhn.fhir.jpa.starter.datamart.service.r5.CryptoUtils;
import ca.uhn.fhir.jpa.starter.datamart.service.r5.ResearchStudyUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResearchStudyUtilsTest {

	@Mock
	private Repository repository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testGetEvidenceVariableSuccess() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/1");
		Extension rootExt = new Extension(ResearchStudyUtils.EXT_URL);
		Extension varExt = new Extension(ResearchStudyUtils.VAR_EXT_NAME);
		varExt.setValue(new Reference("EvidenceVariable/ev1"));
		rootExt.addExtension(varExt);
		study.addExtension(rootExt);

		EvidenceVariable ev = new EvidenceVariable();
		ev.setId("ev1");
		when(repository.read(eq(EvidenceVariable.class), any(IIdType.class))).thenReturn(ev);

		EvidenceVariable result = ResearchStudyUtils.getEvidenceVariable(study, repository);

		assertNotNull(result);
		assertEquals("ev1", result.getId());
		ArgumentCaptor<IIdType> captor = ArgumentCaptor.forClass(IIdType.class);
		verify(repository).read(eq(EvidenceVariable.class), captor.capture());
		assertEquals("ev1", captor.getValue().getIdPart());
	}

	@Test
	void testGetEvidenceVariableMissingExtension() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/2");
		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
			ResearchStudyUtils.getEvidenceVariable(study, repository)
		);
		assertTrue(ex.getMessage().contains("does not contain extension"));
	}

	@Test
	void testGetEvidenceVariableMissingVarExtension() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/3");
		Extension rootExt = new Extension(ResearchStudyUtils.EXT_URL);
		study.addExtension(rootExt);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
			ResearchStudyUtils.getEvidenceVariable(study, repository)
		);
		assertTrue(ex.getMessage().contains("does not contain sub-extension"));
	}

	@Test
	void testGetEvidenceVariableInvalidRefType() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/4");
		Extension rootExt = new Extension(ResearchStudyUtils.EXT_URL);
		Extension varExt = new Extension(ResearchStudyUtils.VAR_EXT_NAME);
		varExt.setValue(new Reference("Observation/obs1"));
		rootExt.addExtension(varExt);
		study.addExtension(rootExt);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
			ResearchStudyUtils.getEvidenceVariable(study, repository)
		);
		assertTrue(ex.getMessage().contains("does not have a valid eligibility reference to an EvidenceVariable"));
	}

	@Test
	void testGetEligibleGroupSuccess() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/5");
		study.getRecruitment().setActualGroup(new Reference("Group/g1"));

		Group group = new Group();
		group.setId("g1");
		Group.GroupMemberComponent member = new Group.GroupMemberComponent();
		member.setEntity(new Reference("Patient/p1"));
		group.addMember(member);
		when(repository.read(eq(Group.class), any(IIdType.class))).thenReturn(group);

		Group result = ResearchStudyUtils.getEligibleGroup(study, repository);
		assertNotNull(result);
		assertEquals("g1", result.getId());
	}

	@Test
	void testGetEligibleGroupNoGroup() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/6");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			ResearchStudyUtils.getEligibleGroup(study, repository)
		);
		assertTrue(ex.getMessage().contains("does not have an actualGroup"));
	}

	@Test
	void testGetEligibleGroupInvalidRefType() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/7");
		study.getRecruitment().setActualGroup(new Reference("Patient/p1"));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			ResearchStudyUtils.getEligibleGroup(study, repository)
		);
		System.out.print(ex.getMessage());
		assertTrue(ex.getMessage().contains("has an invalid actualGroup reference"));
	}

	@Test
	void testGetEligibleGroupEmptyMembers() {
		ResearchStudy study = new ResearchStudy();
		study.setUrl("Study/8");
		study.getRecruitment().setActualGroup(new Reference("Group/g2"));
		Group emptyGroup = new Group();
		emptyGroup.setId("g2");
		when(repository.read(eq(Group.class), any(IIdType.class))).thenReturn(emptyGroup);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
			ResearchStudyUtils.getEligibleGroup(study, repository)
		);
		assertTrue(ex.getMessage().contains("contains no members"));
	}

	@Test
	void testGetSubjectReferencesSuccess() {
		Group group = new Group();
		group.setId("g3");
		Group.GroupMemberComponent member = new Group.GroupMemberComponent();
		member.setEntity(new Reference("Patient/enc1"));
		group.addMember(member);

		try (MockedStatic<CryptoUtils> crypto = Mockito.mockStatic(CryptoUtils.class)) {
			crypto.when(() -> CryptoUtils.decrypt("enc1")).thenReturn("real1");
			List<String> refs = ResearchStudyUtils.getSubjectReferences(group);
			assertEquals(1, refs.size());
			assertEquals("Patient/real1", refs.get(0));
		}
	}

	@Test
	void testGetSubjectReferencesEmptyGroup() {
		Group group = new Group();
		group.setId("g4");
		List<String> refs = ResearchStudyUtils.getSubjectReferences(group);
		assertTrue(refs.isEmpty());
	}

	@Test
	void testGetSubjectReferencesInvalidMember() {
		Group group = new Group();
		group.setId("g5");
		Group.GroupMemberComponent member = new Group.GroupMemberComponent();
		member.setEntity(new Reference("Observation/o1"));
		group.addMember(member);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
			ResearchStudyUtils.getSubjectReferences(group)
		);
		assertTrue(ex.getMessage().contains("invalid member reference"));
	}

	@Test
	void testDesanonmyseEncryptedIdSuccess() {
		try (MockedStatic<CryptoUtils> crypto = Mockito.mockStatic(CryptoUtils.class)) {
			crypto.when(() -> CryptoUtils.decrypt("enc123")).thenReturn("dec123");
			String result = ResearchStudyUtils.desanonmyseEncryptedId("enc123");
			assertEquals("dec123", result);
		}
	}

	@Test
	void testDesanonmyseEncryptedIdFailure() {
		RuntimeException cause = new RuntimeException("fail");
		try (MockedStatic<CryptoUtils> crypto = Mockito.mockStatic(CryptoUtils.class)) {
			crypto.when(() -> CryptoUtils.decrypt("bad")).thenThrow(cause);
			RuntimeException ex = assertThrows(RuntimeException.class, () ->
				ResearchStudyUtils.desanonmyseEncryptedId("bad")
			);
			assertSame(cause, ex.getCause());
		}
	}

	@Test
	void testPseudonymizeRealId() {
		try (MockedStatic<CryptoUtils> crypto = Mockito.mockStatic(CryptoUtils.class)) {
			crypto.when(() -> CryptoUtils.encrypt("id123")).thenReturn("encXYZ");
			String result = ResearchStudyUtils.pseudonymizeRealId("id123");
			assertEquals("encXYZ", result);
		}
	}
}

