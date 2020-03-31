package ca.uhn.fhir.jpa.empi.svc;

import ca.uhn.fhir.jpa.api.EmpiMatchResultEnum;
import ca.uhn.fhir.jpa.dao.DaoMethodOutcome;
import ca.uhn.fhir.jpa.empi.BaseEmpiR4Test;
import ca.uhn.fhir.jpa.empi.dao.IEmpiLinkDao;
import ca.uhn.fhir.jpa.empi.entity.EmpiLink;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class EmpiMatchLinkSvcTest extends BaseEmpiR4Test {
	private static final Logger ourLog = getLogger(EmpiMatchLinkSvcTest.class);

	@Autowired
	private EmpiMatchLinkSvc myEmpiMatchLinkSvc;
	@Autowired
	private ResourceTableHelper myResourceTableHelper;
	@Autowired
	IEmpiLinkDao myEmpiLinkDao;

	@Test
	public void testAddPatientLinksToNewPersonIfNoneFound() {
		createPatientAndUpdateLinks(buildJanePatient());
		assertLinkCount(1);
	}

	@Test
	public void testAddPatientLinksToNewPersonIfNoMatch() {
		Patient patient1 = createPatientAndUpdateLinks(buildJanePatient());
		Patient patient2 = createPatientAndUpdateLinks(buildPaulPatient());

		assertLinkCount(2);
		assertThat(patient1, is(not(samePersonAs(patient2))));
	}

	@Test
	public void testAddPatientLinksToExistingPersonIfMatch() {
		Patient patient1 = createPatientAndUpdateLinks(buildJanePatient());
		assertLinkCount(1);

		Patient patient2 = createPatientAndUpdateLinks(buildJanePatient());
		assertLinkCount(2);

		assertThat(patient1, is(samePersonAs(patient2)));
	}

	@Test
	public void testPatientLinksToPersonIfMatch() {
		Person janePerson = buildJanePerson();
		DaoMethodOutcome outcome = myPersonDao.create(janePerson);
		Long origPersonPid = myResourceTableHelper.getPidOrNull(outcome.getResource());

		createPatientAndUpdateLinks(buildJanePatient());
		assertLinkCount(1);
		List<EmpiLink> links = myEmpiLinkDao.findAll();
		EmpiLink link = links.get(0);
		Long linkedPersonPid = link.getPersonPid();
		assertEquals(EmpiMatchResultEnum.MATCH, link.getMatchResult());

		assertEquals(origPersonPid, linkedPersonPid);
	}

	@Test
	public void testMatchStatusSetPossibleIfMultiplePersonMatch() {
		//FIXME EMPI Question. How do I compare patient -> person attributes.
		Person janePerson1 = buildJanePerson();
		DaoMethodOutcome outcome1 = myPersonDao.create(janePerson1);
		Long origPersonPid1 = myResourceTableHelper.getPidOrNull(outcome1.getResource());

		Person janePerson2 = buildJanePerson();
		DaoMethodOutcome outcome2 = myPersonDao.create(janePerson2);
		Long origPersonPid2 = myResourceTableHelper.getPidOrNull(outcome2.getResource());

		createPatientAndUpdateLinks(buildJanePatient());
		assertLinkCount(2);
		List<EmpiLink> links = myEmpiLinkDao.findAll();
		{
			EmpiLink link = links.get(0);
			Long linkedPersonPid = link.getPersonPid();
			assertEquals(EmpiMatchResultEnum.POSSIBLE_MATCH, link.getMatchResult());
			assertEquals(origPersonPid1, linkedPersonPid);
		}
		{
			EmpiLink link = links.get(1);
			Long linkedPersonPid = link.getPersonPid();
			assertEquals(EmpiMatchResultEnum.POSSIBLE_MATCH, link.getMatchResult());
			assertEquals(origPersonPid2, linkedPersonPid);
		}
	}


	// FIXME EMPI
	// Test: empi link record exists with MANUAL NO_MATCH, that patient is updated, and it MATCH according to rules, the NO_MATCH blocks it

	// FIXME EMPI
	// Test: empi link record exists with MANUAL NO_MATCH, that patient is updated, and it POSSIBLE_MATCH according to rules, the NO_MATCH blocks it

	// FIXME EMPI
	// Test: it should be impossible to have a AUTO NO_MATCH record.  The only NO_MATCH records in the system must be MANUAL.

	// FIXME EMPI
	// Test: new Person is created from a Patient that has an EID, the Patient's EID is added to the Person

	// FIXME EMPI
	// Test: new Person is created from a Patient that doesn't have an EID, a new system-assigned (random uuid) EID is created for the Person

	// FIXME EMPI
	// Test: new Person is created from a Patient.  All available relevant Patient fields (name, addr, id) are copied from the Patient to the Person

	// FIXME EMPI
	// Test: Existing Person found linked from matched Patient.  incoming Patient has no EID.  Create link all done.

	// FIXME EMPI
	// Test: Existing Person with system-assigned EID found linked from matched Patient.  incoming Patient has EID.  Replace Person system-assigned EID with Patient EID.

	// FIXME EMPI
	// Test: Existing Person with legit EID (from a Patient) found linked from matched Patient.  incoming Patient has same EID.   Create link all done.

	// FIXME EMPI
	// Test: Existing Person with legit EID (from a Patient) found linked from matched Patient.  incoming Patient has different EID.   Create new Person with incoming EID and link.
	// Record somehow (design TBD) that these two Persons may be duplicates.  -- Maybe we have a special kind of EmpiLink table entry where the target also points to a Person and it's
	// flagged with a special PROBABLE_DUPLICATE match status?

	private Patient createPatientAndUpdateLinks(Patient thePatient) {
		//Note that since our empi-rules block on active=true, all patients must be active.
		thePatient.setActive(true);
		DaoMethodOutcome daoMethodOutcome = myPatientDao.create(thePatient);
		thePatient.setId(daoMethodOutcome.getId());
		myEmpiMatchLinkSvc.updateEmpiLinksForPatient(thePatient);
		return thePatient;
	}

}
