package ca.uhn.fhir.cql.r4;

import ca.uhn.fhir.cql.BaseCqlR4Test;
import ca.uhn.fhir.cql.r4.provider.MeasureOperationsProvider;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class CqlHivTest extends BaseCqlR4Test {
	public static final String HIV_INDICATORS_ID = "measure-EXM349-2.9.000"; // hiv-indicators
	public static final IdType hivMeasureId = new IdType("Measure", HIV_INDICATORS_ID);

	@Autowired
	IFhirResourceDao<Measure> myMeasureDao;
	@Autowired
	IFhirResourceDao<Library> myLibraryDao;
	@Autowired
	MeasureOperationsProvider myMeasureOperationsProvider;

	@Test
	public void testGeneratedContent() throws IOException {
		//loadResource("r4/EXM349/library-EXM349_FHIR3-2.9.000.json");
		loadBundle("r4/EXM349/EXM349_FHIR4-2.9.000-bundle.json");

		myMeasureOperationsProvider.refreshGeneratedContent(null, hivMeasureId);
		Measure measure = myMeasureDao.read(hivMeasureId);
		// FIXME KBD Add asserts--e.g. on generated elm
	}
}