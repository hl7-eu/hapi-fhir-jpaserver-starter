package ca.uhn.fhir.jpa.starter.mapping.service;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XMLDataReaderTest {

	@Test
	void shouldParseSimpleXml() throws JSONException {
		String xml = """
            <patient>
                <name>John Doe</name>
                <age>42</age>
            </patient>
            """;

		String encoded = Base64.getEncoder()
			.encodeToString(xml.getBytes(StandardCharsets.UTF_8));

		JSONObject result = XMLDataReader.parseXMLData(encoded);

		assertThat(result.getString("name")).isEqualTo("John Doe");
		assertThat(result.getString("age")).isEqualTo("42");
	}

	@Test
	void shouldParseAttributes() throws JSONException {
		String xml = """
            <patient id="123" status="active"/>
            """;

		String encoded = Base64.getEncoder()
			.encodeToString(xml.getBytes(StandardCharsets.UTF_8));

		JSONObject result = XMLDataReader.parseXMLData(encoded);

		assertThat(result.getString("id")).isEqualTo("123");
		assertThat(result.getString("status")).isEqualTo("active");
	}

	@Test
	void shouldParseNestedElements() throws JSONException {
		String xml = """
            <patient>
                <address>
                    <city>Paris</city>
                    <country>France</country>
                </address>
            </patient>
            """;

		String encoded = Base64.getEncoder()
			.encodeToString(xml.getBytes(StandardCharsets.UTF_8));

		JSONObject result = XMLDataReader.parseXMLData(encoded);

		JSONObject address = result.getJSONObject("address");

		assertThat(address.getString("city")).isEqualTo("Paris");
		assertThat(address.getString("country")).isEqualTo("France");
	}

	@Test
	void shouldParseRepeatedElementsAsArray() throws JSONException {
		String xml = """
            <patient>
                <phone>111</phone>
                <phone>222</phone>
                <phone>333</phone>
            </patient>
            """;

		String encoded = Base64.getEncoder()
			.encodeToString(xml.getBytes(StandardCharsets.UTF_8));

		JSONObject result = XMLDataReader.parseXMLData(encoded);

		JSONArray phones = result.getJSONArray("phone");

		assertThat(phones.length()).isEqualTo(3);
		assertThat(phones.getString(0)).isEqualTo("111");
		assertThat(phones.getString(1)).isEqualTo("222");
		assertThat(phones.getString(2)).isEqualTo("333");
	}

	@Test
	void shouldParseComplexRepeatedElementsAsArray() throws JSONException {
		String xml = """
            <patient>
                <contact>
                    <name>Alice</name>
                </contact>
                <contact>
                    <name>Bob</name>
                </contact>
            </patient>
            """;

		String encoded = Base64.getEncoder()
			.encodeToString(xml.getBytes(StandardCharsets.UTF_8));

		JSONObject result = XMLDataReader.parseXMLData(encoded);

		JSONArray contacts = result.getJSONArray("contact");

		assertThat(contacts.length()).isEqualTo(2);
		assertThat(((JSONObject) contacts.get(0)).getString("name"))
			.isEqualTo("Alice");
		assertThat(((JSONObject) contacts.get(1)).getString("name"))
			.isEqualTo("Bob");
	}

	@Test
	void shouldThrowInternalErrorExceptionWhenBase64IsInvalid() {
		assertThrows(
			InternalErrorException.class,
			() -> XMLDataReader.parseXMLData("not-base64")
		);
	}

	@Test
	void shouldThrowInternalErrorExceptionWhenXmlIsInvalid() {
		String invalidXml = "<patient><name>John</patient>";

		String encoded = Base64.getEncoder()
			.encodeToString(invalidXml.getBytes(StandardCharsets.UTF_8));

		assertThrows(
			InternalErrorException.class,
			() -> XMLDataReader.parseXMLData(encoded)
		);
	}
}