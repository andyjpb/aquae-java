///////////////////////////////////////////////////////////////////////////////
///
/// libAquae - An implementation of the Aquae Protocol.
///
///  gov.uk Personal Data Exchange is a way to query existing personal data
///  held by government.
///
///  libAquae is a library that implements the primitive operations that
///  underlie the Aquae Protocol. It can be used to implement nodes, clients,
///  servers, utilities and tools that need to speak to other Aquae
///  implementations.
///  Aquae is the underlying protocol used to describe and transport question
///  and answer style eligibility queries within a federation of cooperating
///  nodes.
///  AquaeDataStructures are the Java, Transport agnostic, representation of
///  all of the Aquae Domain /  Objects.
///
///
///  Copyright (C) 2017, Andy Bennett, Crown Copyright (Government Digital Service).
///
///  Permission is hereby granted, free of charge, to any person obtaining a
///  copy of this software and associated documentation files (the "Software"),
///  to deal in the Software without restriction, including without limitation
///  the rights to use, copy, modify, merge, publish, distribute, sublicense,
///  and/or sell copies of the Software, and to permit persons to whom the
///  Software is furnished to do so, subject to the following conditions:
///
///  The above copyright notice and this permission notice shall be included in
///  all copies or substantial portions of the Software.
///
///  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
///  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
///  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
///  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
///  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
///  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
///  DEALINGS IN THE SOFTWARE.
///
/// Andy Bennett <andyjpb@digital.cabinet-office.gov.uk>, 2017/08
///
///////////////////////////////////////////////////////////////////////////////

// libAquae cannot be invoked directly. You should import it into another
// program thus:
// import uk.gov.Aquae.*;


package uk.gov.Aquae;


import java.io.*;
import java.util.*;



///////////////////////////////////////////////////////////////////////////////
/// Aquae Data Structures

public class AquaeDataStructures {

	static class ConfidenceAttribute {
		private final String name;
		private final String description;

		ConfidenceAttribute(String name, String description) {
			this.name        = name;
			this.description = description;
		}

		// Object.equals() only gives us reference equality.
		public boolean equals(Object o) {
			ConfidenceAttribute p = null;

			if (this == o) return true;
			if (!(o instanceof ConfidenceAttribute)) return false; // null is never an instance of this

			p = (ConfidenceAttribute)(o);

			return (((name        == null) ? (p.name        == null) : name.equals(p.name)) &&
			        ((description == null) ? (p.description == null) : description.equals(p.description)));
		}

		// We overrode equals so we override hashCode() too in order to
		// maintain the invariant that objects compared equal with
		// equals() have the same hashCode()s.
		public int hashCode() {
			// MatchingRequirements are probably equal if their members have the hashCode()s.
			// 37 is a happy prime!

			int c = 1486746592;
			c = (37 * c) + ((name        == null) ? 0 : name.hashCode());
			c = (37 * c) + ((description == null) ? 0 : description.hashCode());

			return c;
		}

		// Validators for fields that we will store this object against
		// in Maps.  These validators need to be static so that we can
		// use them without a ConfidenceAttribute object already in
		// hand.  They also need to be able to work with untrusted data
		// rather than just fishing out the pre-sanitized member from
		// an instance of an object.  Returns the field if it's valid
		// otherwise throws.
		static String validateName(String name) {
			// name can be any valid UTF-8 string.
			return name;
		}
	}

	static class MatchingRequirements {
		private final Set<AquaeMetadata.IdentityAttribute> requiredAttributes;
		private final Set<AquaeMetadata.IdentityAttribute> disambiguatorAttributes;
		private final Set                                 confidenceAttributes;

		MatchingRequirements(Set<AquaeMetadata.IdentityAttribute> requiredAttributes, Set<AquaeMetadata.IdentityAttribute> disambiguatorAttributes, Set confidenceAttributes) {
			this.requiredAttributes      = requiredAttributes;
			this.disambiguatorAttributes = disambiguatorAttributes;
			this.confidenceAttributes    = confidenceAttributes;
		}


		// Object.equals() only gives us reference equality.
		public boolean equals(Object o) {
			MatchingRequirements p = null;

			if (this == o) return true;
			if (!(o instanceof MatchingRequirements)) return false; // null is never an instance of this

			p = (MatchingRequirements)(o);

			return (((requiredAttributes      == null) ? (p.requiredAttributes      == null) : requiredAttributes.equals(p.requiredAttributes)) &&
			        ((disambiguatorAttributes == null) ? (p.disambiguatorAttributes == null) : disambiguatorAttributes.equals(p.disambiguatorAttributes)) &&
				((confidenceAttributes    == null) ? (p.confidenceAttributes    == null) : confidenceAttributes.equals(p.confidenceAttributes)));
		}

		// We overrode equals so we override hashCode() too in order to
		// maintain the invariant that objects compared equal with
		// equals() have the same hashCode()s.
		public int hashCode() {
			// MatchingRequirements are probably equal if their members have the hashCode()s.
			// 37 is a happy prime!

			int c = 1486746592;
			c = (37 * c) + ((requiredAttributes      == null) ? 0 : requiredAttributes.hashCode());
			c = (37 * c) + ((disambiguatorAttributes == null) ? 0 : disambiguatorAttributes.hashCode());
			c = (37 * c) + ((confidenceAttributes    == null) ? 0 : confidenceAttributes.hashCode());

			return c;
		}

	}

}

