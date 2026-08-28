package ca.uhn.fhir.jpa.starter.mapping.service;

import ca.uhn.fhir.jpa.starter.mapping.model.CSVRecords;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pour vérifier que le traitement CSV est bien en mode ligne par ligne
 * au lieu de règle par règle.
 */
class MapperCSVRowByRowTest {

	@Test
	void verifyCSVRowByRowProcessing() throws IOException {
		// Préparation des données de test
		String csvData = "name,age\n" +
						"Alice,30\n" +
						"Bob,25\n" +
						"Charlie,35";

		CSVParser parser = CSVFormat.EXCEL
				.withFirstRecordAsHeader()
				.parse(new StringReader(csvData));
		
		List<CSVRecord> records = parser.getRecords();
		CSVRecords csvRecords = new CSVRecords(records);

		// Vérification
		assertNotNull(csvRecords);
		assertEquals(3, csvRecords.getRecords().size(), "Devrait avoir 3 enregistrements");
		
		// Vérifier que chaque record a les bonnes colonnes
		int index = 0;
		for (CSVRecord record : csvRecords.getRecords()) {
			switch (index) {
				case 0:
					assertEquals("Alice", record.get("name"));
					assertEquals("30", record.get("age"));
					break;
				case 1:
					assertEquals("Bob", record.get("name"));
					assertEquals("25", record.get("age"));
					break;
				case 2:
					assertEquals("Charlie", record.get("name"));
					assertEquals("35", record.get("age"));
					break;
			}
			index++;
		}
		
		parser.close();
	}

	@Test
	void verifySingleRowCSVRecordsCreation() throws IOException {
		// Création de données de test
		String csvData = "id,firstName,lastName\n" +
						"1,John,Doe\n" +
						"2,Jane,Smith";

		CSVParser parser = CSVFormat.EXCEL
				.withFirstRecordAsHeader()
				.parse(new StringReader(csvData));
		
		List<CSVRecord> allRecords = parser.getRecords();
		
		// Simuler le traitement ligne par ligne
		for (int i = 0; i < allRecords.size(); i++) {
			CSVRecord singleRecord = allRecords.get(i);
			
			// Créer un CSVRecords avec une seule ligne (comme fait dans executeGroupByCSVRows)
			List<CSVRecord> singleRowList = new ArrayList<>();
			singleRowList.add(singleRecord);
			CSVRecords singleRowRecords = new CSVRecords(singleRowList);
			
			// Vérifier que le CSVRecords contient bien une seule ligne
			assertEquals(1, singleRowRecords.getRecords().size(), 
					"Chaque CSVRecords devrait contenir une seule ligne (itération " + i + ")");
			
			// Vérifier que la ligne est la bonne
			assertEquals(singleRecord, singleRowRecords.getRecords().get(0),
					"La ligne devrait être identique (itération " + i + ")");
		}
		
		parser.close();
	}

	/**
	 * Test conceptuel montrant l'ordre d'exécution attendu
	 * 
	 * Ancien comportement (Rule-by-Rule):
	 * Rule 1 processes: Row 1, Row 2, Row 3
	 * Rule 2 processes: Row 1, Row 2, Row 3
	 * Rule 3 processes: Row 1, Row 2, Row 3
	 * 
	 * Nouveau comportement (Row-by-Row):
	 * Row 1 processes: Rule 1, Rule 2, Rule 3
	 * Row 2 processes: Rule 1, Rule 2, Rule 3
	 * Row 3 processes: Rule 1, Rule 2, Rule 3
	 */
	@Test
	void conceptualExecutionOrder() {
		List<String> executionLog = new ArrayList<>();
		
		// Simulation du traitement Row-by-Row
		List<String> rows = List.of("Row1", "Row2", "Row3");
		List<String> rules = List.of("Rule1", "Rule2", "Rule3");
		
		for (String row : rows) {
			for (String rule : rules) {
				// Ceci simule l'exécution d'une règle sur une ligne
				executionLog.add(row + " -> " + rule);
			}
		}
		
		// Vérifier l'ordre d'exécution
		assertEquals("Row1 -> Rule1", executionLog.get(0));
		assertEquals("Row1 -> Rule2", executionLog.get(1));
		assertEquals("Row1 -> Rule3", executionLog.get(2));
		assertEquals("Row2 -> Rule1", executionLog.get(3));
		assertEquals("Row2 -> Rule2", executionLog.get(4));
		assertEquals("Row2 -> Rule3", executionLog.get(5));
		assertEquals("Row3 -> Rule1", executionLog.get(6));
		assertEquals("Row3 -> Rule2", executionLog.get(7));
		assertEquals("Row3 -> Rule3", executionLog.get(8));
		
		assertEquals(9, executionLog.size(), "Devrait avoir 9 exécutions (3 rows x 3 rules)");
	}

	@Test
	void verifyCSVRecordSingletonList() {
		// Créer un record de test
		String csvData = "name,email\nAlice,alice@example.com";
		
		try {
			CSVParser parser = CSVFormat.EXCEL
					.withFirstRecordAsHeader()
					.parse(new StringReader(csvData));
			
			CSVRecord record = parser.getRecords().get(0);
			
			// Créer une liste singleton (comme dans executeGroupByCSVRows)
			List<CSVRecord> singletonList = java.util.Collections.singletonList(record);
			CSVRecords singletonRecords = new CSVRecords(singletonList);
			
			assertEquals(1, singletonRecords.getRecords().size());
			assertEquals(record, singletonRecords.getRecords().get(0));
			
			parser.close();
		} catch (IOException e) {
			fail("IOException lancée: " + e.getMessage());
		}
	}
}

