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
///  AquaeNode is the high level object that represents a node in an Aquae
///  Federation.
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


import java.util.Arrays;
import java.net.URI;

//import uk.gov.Aquae.*;



///////////////////////////////////////////////////////////////////////////////
/// Aquae Node

// This represents a question that a user can ask of the network. They can't
// construct them themselves: they have to be obtained by finding them through
// the Metadata.
// We use this class to represent fresh queries constructed by a user as well
// as queries that are received from another node.
// TODO: Fix these comments
public class AquaeNode {
	// nodeName is just used to dereference the graph in the Metadata. Once
	// the Metadata has been loaded we don't need it any more and shouldn't
	// use it for anything functional. However, we keep it around for
	// debugging messages.
	private final String       nodeName;
	private final String       hostname;
	private final int          port;
	private final byte[]       tlsKey;


	// This constructor is responsible for ensuring that the structure it
	// produces is safe.  The arguments are allowed to contain unsanitised,
	// untrusted user input.
	AquaeNode(String nodeName, String hostname, int port, byte[] tlsKey) {
		// nodeName can be any valid UTF-8 string.
		this.nodeName = validateNodeName(nodeName);

		try {
			URI uri = new URI("https://" + hostname);
			if ((uri.getHost() == null) || (!uri.getHost().equals(hostname))) {
				throw new RuntimeException("Invalid Hostnmae: " + hostname); // TODO: fix exception type.
			}
		} catch (java.net.URISyntaxException e) {
				throw new RuntimeException("Invalid Hostnmae: " + hostname); // TODO: fix exception type and chain them together.
		}
		// TODO: populate a test system with a range of valid and invalid edge-case hostnames.
		this.hostname = hostname;

		// port 0 is not valid for source ports on outgoing connections
		if ((port <= 0) || (port > 65535)) {
			throw new RuntimeException("Invalid port number: " + port); // TODO: fix exception type.
		}
		this.port = port;

		// tlsKey can be any byte[].
		this.tlsKey = validateTlsKey(tlsKey);
	}

	// Validators for fields that we will store this object against in Maps.
	// These validators need to be static so that we can use them without
	// an AquaeNode object already in hand.  They also need to be able to
	// work with untrusted data rather than just fishing out the
	// pre-sanitized member from an instance of an object.
	// Returns the field if it's valid otherwise throws.

	static String validateNodeName(String nodeName) {
		// nodeName can be any valid UTF-8 string.
		return nodeName;
	}

	static byte[] validateTlsKey(byte[] tlsKey) {
		// tlsKey can be any byte[].
		return tlsKey;
	}


	// Object.equals() only gives us reference equality.
	public boolean equals(Object o) {
		AquaeNode p = null;

		if (this == o) return true;
		if (!(o instanceof AquaeNode)) return false; // null is never an instanceof this

		p = (AquaeNode)(o);

		return (((nodeName == null) ? (p.nodeName == null) : nodeName.equals(p.nodeName)) &&
		        ((hostname == null) ? (p.hostname == null) : hostname.equals(p.hostname)) &&
			(port == p.port) &&
			(Arrays.equals(tlsKey, p.tlsKey)));
	}

	// We overrode equals so we override hashCode() too in order to
	// maintain the invariant that objects compared equal with
	// equals() have the same hashCode()s.
	public int hashCode() {
		// MatchingRequirements are probably equal if their members have the hashCode()s.
		// 37 is a happy prime!

		int c = 1486746592;
		c = (37 * c) + ((nodeName == null) ? 0 : nodeName.hashCode());
		c = (37 * c) + ((hostname == null) ? 0 : hostname.hashCode());
		c = (37 * c) + port;
		c = (37 * c) + ((tlsKey   == null) ? 0 : Arrays.hashCode(tlsKey));

		return c;
	}
}

