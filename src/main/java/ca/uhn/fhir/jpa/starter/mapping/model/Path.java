package ca.uhn.fhir.jpa.starter.mapping.model;

import ca.uhn.hl7v2.HL7Exception;

import java.util.ArrayList;
import java.util.List;

public class Path {

	private final List<String> groups = new ArrayList<>();
	private final List<Integer> groupRepetitions = new ArrayList<>();

	private final String segment;
	private final Integer segmentRepetition;

	private final String terminalGroup;
	private final Integer terminalGroupRepetition;

	private final Integer field;
	private final Integer fieldRepetition;
	private final Integer component;
	private final Integer subComponent;

	public Path(String path) throws HL7Exception {
		if (path == null || path.isBlank()) {
			throw new HL7Exception("Invalid path: " + path);
		}

		String trimmed = path.trim();
		if (trimmed.isEmpty()) {
			throw new HL7Exception("Invalid path: " + path);
		}

		// Refuse explicit malformed dotted paths
		if (trimmed.startsWith(".") || trimmed.endsWith(".") || trimmed.contains("..")) {
			throw new HL7Exception("Invalid path: " + path);
		}

		String head = trimmed;
		String tail = null;

		int dash = trimmed.indexOf('-');
		if (dash >= 0) {
			head = trimmed.substring(0, dash);
			tail = trimmed.substring(dash + 1);

			if (tail.isBlank()) {
				throw new HL7Exception("Invalid path: " + path);
			}
		}

		String[] tokens = head.split("\\.", -1);
		if (tokens.length == 0) {
			throw new HL7Exception("Invalid path: " + path);
		}

		for (String token : tokens) {
			if (token == null || token.isBlank()) {
				throw new HL7Exception("Invalid path: " + path);
			}
		}

		String lastToken = tokens[tokens.length - 1];

		String parsedSegment = null;
		Integer parsedSegmentRep = null;
		String parsedTerminalGroup = null;
		Integer parsedTerminalGroupRep = null;

		boolean hasFieldPart = tail != null;

		TokenPart last = parseTokenStrict(lastToken, path);

		if (hasFieldPart) {
			if (!looksLikeSegmentToken(last.name())) {
				throw new HL7Exception("Invalid path: " + path);
			}
			parsedSegment = last.name();
			parsedSegmentRep = last.repetition();

			for (int i = 0; i < tokens.length - 1; i++) {
				TokenPart gp = parseTokenStrict(tokens[i], path);
				groups.add(gp.name());
				groupRepetitions.add(gp.repetition());
			}
		}
		else if (looksLikeSegmentToken(last.name())) {
			parsedSegment = last.name();
			parsedSegmentRep = last.repetition();

			for (int i = 0; i < tokens.length - 1; i++) {
				TokenPart gp = parseTokenStrict(tokens[i], path);
				groups.add(gp.name());
				groupRepetitions.add(gp.repetition());
			}
		} else {
			// Support group-only path
			if (tokens.length < 2) {
				throw new HL7Exception("Invalid path: " + path);
			}

			parsedTerminalGroup = last.name();
			parsedTerminalGroupRep = last.repetition();

			for (int i = 0; i < tokens.length - 1; i++) {
				TokenPart gp = parseTokenStrict(tokens[i], path);
				groups.add(gp.name());
				groupRepetitions.add(gp.repetition());
			}
		}

		this.segment = parsedSegment;
		this.segmentRepetition = parsedSegmentRep;
		this.terminalGroup = parsedTerminalGroup;
		this.terminalGroupRepetition = parsedTerminalGroupRep;

		Integer parsedField = null;
		Integer parsedFieldRep = null;
		Integer parsedComponent = null;
		Integer parsedSubComponent = null;

		if (tail != null) {
			String[] fieldParts = tail.split("-", -1);
			if (fieldParts.length < 1 || fieldParts.length > 3) {
				throw new HL7Exception("Invalid path: " + path);
			}

			for (String fieldPart : fieldParts) {
				if (fieldPart == null || fieldPart.isBlank()) {
					throw new HL7Exception("Invalid path: " + path);
				}
			}

			TokenPart fieldToken = parseTokenStrict(fieldParts[0], path);
			if (fieldToken.name() == null || !fieldToken.name().matches("[1-9][0-9]*")) {
				throw new HL7Exception("Invalid path: " + path);
			}
			parsedField = Integer.parseInt(fieldToken.name());
			parsedFieldRep = fieldToken.repetition();

			if (fieldParts.length >= 2) {
				if (!fieldParts[1].matches("[1-9][0-9]*")) {
					throw new HL7Exception("Invalid path: " + path);
				}
				parsedComponent = Integer.parseInt(fieldParts[1]) - 1;
			}

			if (fieldParts.length == 3) {
				if (!fieldParts[2].matches("[1-9][0-9]*")) {
					throw new HL7Exception("Invalid path: " + path);
				}
				parsedSubComponent = Integer.parseInt(fieldParts[2]) - 1;
			}
		}

		this.field = parsedField;
		this.fieldRepetition = parsedFieldRep;
		this.component = parsedComponent;
		this.subComponent = parsedSubComponent;
	}

	private boolean looksLikeSegmentToken(String token) {
		return token != null && token.matches("^[A-Z0-9]{2,4}$");
	}

	private TokenPart parseTokenStrict(String token, String originalPath) throws HL7Exception {
		if (token == null || token.isBlank()) {
			throw new HL7Exception("Invalid path: " + originalPath);
		}

		String t = token.trim();

		if (!t.contains("[") && !t.contains("]")) {
			if (!t.matches("^[A-Za-z0-9_]+$")) {
				throw new HL7Exception("Invalid path: " + originalPath);
			}
			return new TokenPart(t, null);
		}

		if (!t.matches("^[A-Za-z0-9_]+\\[[0-9]+\\]$")) {
			throw new HL7Exception("Invalid path: " + originalPath);
		}

		int start = t.indexOf('[');
		int end = t.indexOf(']');

		String name = t.substring(0, start);
		String repStr = t.substring(start + 1, end);

		if (name.isBlank() || !name.matches("^[A-Za-z0-9_]+$")) {
			throw new HL7Exception("Invalid path: " + originalPath);
		}
		if (!repStr.matches("[0-9]+")) {
			throw new HL7Exception("Invalid path: " + originalPath);
		}

		return new TokenPart(name, Integer.parseInt(repStr));
	}

	public boolean isGroupOnly() {
		return terminalGroup != null && segment == null;
	}

	public boolean hasSegment() {
		return segment != null;
	}

	public List<String> getGroups() {
		return groups;
	}

	public List<Integer> getGroupRepetitions() {
		return groupRepetitions;
	}

	public String getSegment() {
		return segment;
	}

	public Integer getSegmentRepetition() {
		return segmentRepetition;
	}

	public String getTerminalGroup() {
		return terminalGroup;
	}

	public Integer getTerminalGroupRepetition() {
		return terminalGroupRepetition;
	}

	public Integer getField() {
		return field;
	}

	public Integer getFieldRepetition() {
		return fieldRepetition;
	}

	public Integer getComponent() {
		return component;
	}

	public Integer getSubComponent() {
		return subComponent;
	}

	@Override
	public String toString() {
		return "Path{" +
			"groups=" + groups +
			", groupRepetitions=" + groupRepetitions +
			", segment='" + segment + '\'' +
			", segmentRepetition=" + segmentRepetition +
			", terminalGroup='" + terminalGroup + '\'' +
			", terminalGroupRepetition=" + terminalGroupRepetition +
			", field=" + field +
			", fieldRepetition=" + fieldRepetition +
			", component=" + component +
			", subComponent=" + subComponent +
			'}';
	}

	private record TokenPart(String name, Integer repetition) {
	}
}