package ca.uhn.fhir.jpa.starter.datamart.service.r5;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatamartExportation {
	private static final Logger logger = LoggerFactory.getLogger(DatamartExportation.class);
	private final MappingEngine mappingEngine;
	private final IGenericClient client;

	public DatamartExportation(MappingEngine mappingEngine, IGenericClient client) {
		this.mappingEngine = mappingEngine;
		this.client = client;
	}

	/**
	 * Export the datamart for the provided ResearchStudy.
	 *
	 * @param researchStudy The ResearchStudy that is the basis for datamart export.
	 * @return A Binary containing the export of the datamart.
	 */
	public org.hl7.fhir.r4.model.Binary exportDatamart(ResearchStudy researchStudy, String type, org.hl7.fhir.r4.model.CanonicalType structureMapUrl) {
		logger.info("Exporting Datamart {}", researchStudy.getUrl());
//		Binary export = new Binary();
//		if (Objects.equals(type, "text/csv")){
//			export.setContentType("text/csv");
//		} else if (Objects.equals(type, "application/json")) {
//			export.setContentType("application/json");
//		} else if (Objects.equals(type, "application/xml")) {
//			export.setContentType("application/xml");
//		} else {
//			throw new IllegalArgumentException("Unsupported export type: " + type);
//		}

		ListResource datamartList = ResearchStudyUtils.getEvaluationList(researchStudy, client);

		Bundle datamartListBundle = fetchListWithInclude(datamartList.getId());
		// Convert the R5 Bundle to an R4 Bundle
		//org.hl7.fhir.r4.model.Bundle datamartListBundleR4 = (Bundle) VersionConvertorFactory_40_50.convertResource(datamartListBundle);

		// Convert the R5 Bundle to an R4 Bundle
		org.hl7.fhir.r4.model.Resource convertedResource = VersionConvertorFactory_40_50.convertResource(datamartListBundle);
		org.hl7.fhir.r4.model.Bundle datamartListBundleR4;
		if (convertedResource instanceof org.hl7.fhir.r4.model.Bundle) {
			datamartListBundleR4 = (org.hl7.fhir.r4.model.Bundle) convertedResource;
		} else {
			throw new IllegalArgumentException("Converted resource is not of type Bundle");
		}

		org.hl7.fhir.r4.model.Binary export = this.mappingEngine.transform(datamartListBundleR4, structureMapUrl);
		return export;
	}

	/**
	 * Fetches a Bundle with an include for List:item.
	 *
	 * @param listId The ID of the List resource to fetch.
	 * @return A Bundle containing the List resource and included items.
	 */
	public Bundle fetchListWithInclude(String listId) {
//		var searchParams = Searches.byId(listId);
//		searchParams.put("_include", Collections.singletonList(new StringParam("List:item")));
		// /List?_id=EXP-List1&_include=List:item
		String searchUrl = String.format("List?_id=%s&_include=List:item", listId);
		Bundle b = client.search().byUrl(searchUrl).returnBundle(Bundle.class).execute();
		return b;
//		return client
//			.search()
//			.forResource(ListResource.class)
//			.where(ListResource.IDENTIFIER.exactly().code(listId))
//			.include(ListResource.INCLUDE_ITEM)
//			.returnBundle(Bundle.class)
//			.execute();

		//return this.client.search(Bundle.class, ListResource.class, searchParams, null);
	}
}
