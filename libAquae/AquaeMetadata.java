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
///  AquaeMetadata is a thin wrapper around the wire protocol that lifts the /
///  domain objects up to a useful level of abstraction for the higher level /
///  drivers and state machines. In doing so, it ensures that the higer level
///  users are always dealing with consistent and sanitised data.
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
/// Aquae Metadata
///
/// Metadata is the backbone of the configuration of the Aquae
/// Federation of nodes. It tells us who the other nodes are, where we
/// can find them, what queries can be run and which DSAs are in place.
/// In order to work with others, the nodes need to share compatible
/// metadata. "Compatible" does not always mean "matching".
///
/// The Metadata is not reloadable: in order to reload, higher layers
/// should create a new Metadata object from the new Metadata file and
/// then replace the new one with the old one in all the relevant
/// places.
/// Metadata objects that will be accessible to Threads must be
/// constructed before those Threads start so that there is a
/// happens-before relationship between the state inside the Metadata
/// object and their subsequent access to it. Provided this condidtion
/// is maintained, we do not need to do any locking or synchronisation
/// on the shared state of the Metadata as it is unmodified after object
/// construction.
/// https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility
/// https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.5
public class AquaeMetadata {
	private final String filename;
	private final Map<String, AquaeNode>                               nodes;
	private final Map<byte[], AquaeNode>                               certificates;
	private final Map<String, AquaeDataStructures.ConfidenceAttribute> confidence;
	private final Map<String, AquaeQuery>                              queries;
	private final Map<String, AquaeAgreement>                          agreements;

	// The ordinal values of this enum MUST match those of the MatchingSpec
	// field. That means that these names MUST appear in the same order
	// here as they do in the Protobuf declaration. Maybe we should use a
	// value-mapping rather than an ordinal-mapping?
	// This could be moved to a Matching component when we have such a
	// construct.
	static enum IdentityAttribute {
		SURNAME(),
		POSTCODE(),
		YEAR_OF_BIRTH(),
		INITIALS(),
		HOUSE_NUMBER(),
		DATE_OF_BIRTH(),
	}

	// Tools
	void debug(String s) {
		System.err.print(s);
	}

	// Load, validate and internalise a Metadata file.
	public AquaeMetadata(String filename) throws FileNotFoundException, IOException {
		uk.gov.Aquae.ProtocolBuffers.Metadata.Federation                    md            = null;
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.Node>                in            = null;
		Map<String, AquaeNode>                                              nodes         = null;
		Map<byte[], AquaeNode>                                              certificates  = null;
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.ConfidenceAttribute> ica           = null;
		Map<String, AquaeDataStructures.ConfidenceAttribute>                confidence    = null;
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.QuerySpec>           iq            = null;
		Map<String, AquaeQuery>                                             queries       = null;
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.DSA>                 ia            = null;
		Map<String, AquaeAgreement>                                         agreements    = null;

		md = uk.gov.Aquae.ProtocolBuffers.Metadata.Federation.parseFrom(new FileInputStream(filename));
		this.filename = filename;

		//  + Validate the Metadata file
		// TODO: check we have the Validity section
		//   check that it is valid based on the current time

		// Import each of the Metadata sections in turn,
		// converting things into our internal representation
		// as we go.

		// Nodes
		nodes        = new HashMap<String, AquaeNode>();
		certificates = new HashMap<byte[], AquaeNode>();
		in           = md.getNodeList().iterator();
		while(in.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.Node n  = in.next();
			AquaeNode                                  an = Node2AquaeNode(n); // PB structural properties validated as correct after this point. Only returned structure is guranteed safe & sanitized tho'.
			AquaeNode                                  r  = null;

			if (an == null) {
				// TODO: fix exception type. Can this
				// even happen? => does it need to be
				// fixed?
				throw new RuntimeException("Node2AquaeNode cannot return null!");
			}

			r = nodes.put(AquaeNode.validateNodeName(n.getName()), an);

			if (r != null) {
				// TODO: fix exception type. Is this
				// something that can be recovered
				// from? if not, a runtime exepction
				// (or subclass thereof) is acceptable;
				// reuse the Transport exception
				// pattern!
				throw new RuntimeException("A declaration for a Node named " + n.getName() + " appears more than once in " + filename);
			}

			r = certificates.put(AquaeNode.validateTlsKey(n.getCertificate().toByteArray()), an);

			if (r != null) {
				// TODO: fix exception type as per
				// nodes.put check.
				throw new RuntimeException("Node named " + n.getName() + " shares a publicKey with another node in " + filename);
			}
		}

		debug("Nodes: " + nodes.toString() + "\n");
		debug("Certificates: " + certificates.toString() + "\n");
		this.nodes = nodes;
		this.certificates = certificates;


		// Agreements
		// TODO
		agreements = new HashMap<String, AquaeAgreement>();
		ia         = md.getAgreementList().iterator();
		while(ia.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.DSA a = ia.next();
			Agreement2AquaeAgreement(a);
		}

		debug("Agreements: " + agreements.toString() + "\n");
		this.agreements = null;


		// Confidence Attributes
		confidence = new HashMap<String, AquaeDataStructures.ConfidenceAttribute>();
		ica        = md.getConfidenceAttributeList().iterator();
		while(ica.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.ConfidenceAttribute ca  = ica.next();
			AquaeDataStructures.ConfidenceAttribute                   aca = ConfidenceAttribute2ConfidenceAttribute(ca);
			AquaeDataStructures.ConfidenceAttribute                   r   = null;

			if (aca == null) {
				// TODO: fix exception type. Can this
				// even happen? => does it need to be
				// fixed?
				throw new RuntimeException("ConfidenceAttribute2ConfidenceAttribute cannot return null!");
			}

			r = confidence.put(AquaeDataStructures.ConfidenceAttribute.validateName(ca.getName()), aca);

			if (r != null) {
				// TODO: fix exception type. Is this
				// something that can be recovered
				// from? if not, a runtime exepction
				// (or subclass thereof) is acceptable;
				// reuse the Transport exception
				// pattern!
				throw new RuntimeException("A declaration for a Confidence Attribute named " + ca.getName() + " appears more than once in " + filename);
			}
		}
		debug("Confidence Attributes: " + confidence.toString() + "\n");
		this.confidence = confidence;


		// Queries
		queries = new HashMap<String, AquaeQuery>();
		iq      = md.getQueryList().iterator();
		while (iq.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.QuerySpec q  = iq.next();
			AquaeQuery                                      aq = QuerySpec2AquaeQuery(queries, q); // PB structural properties validated as correct after this point. Only returned structure is guranteed safe & sanitized tho'.
			AquaeQuery                                      r  = null;

			if (aq == null) {
				// TODO: fix exception type. Can this
				// even happen? => does it need to be
				// fixed?
				throw new RuntimeException("QuerySpec2AquaeQuery cannot return null!");
			}

			r = queries.put(AquaeQuery.validateQueryName(q.getName()), aq);

			if (r != null) {
				// TODO: fix exception type. Is this
				// something that can be recovered
				// from? if not, a runtime exepction
				// (or subclass thereof) is acceptable;
				// reuse the Transport exception
				// pattern!
				throw new RuntimeException("A declaration for a Query named " + q.getName() + " appears more than once in " + filename);
			}
		}

		debug("Queries: " + queries.toString() + "\n");
		this.queries = queries;


		//TODO:
		//  + Catch the IOException as it's not really something the caller can deal with?
		//  + Work out the "root hash" of the Metadata
	}


	/// AquaeNode helpers

	// Validate and internalise a Node Message.
	private AquaeNode Node2AquaeNode(uk.gov.Aquae.ProtocolBuffers.Metadata.Node n) {
		// Nodes are straight forward because they do not
		// reference any other objects in the Metadata file.

		if (n == null) {
			throw new RuntimeException("Null reference for Node in " + filename);
		}

		if (!n.hasName()) {
			throw new RuntimeException("Missing nodeName for Node declaration " + n + " in " + filename);
		}

		if (!n.hasLocation()) {
			throw new RuntimeException("Missing location for Node declaration " + n.getName() + " in " + filename);
		}

		if (!n.getLocation().hasHostname()) {
			throw new RuntimeException("Missing hostname for Node declaration " + n.getName() + " in " + filename);
		}

		if (!n.getLocation().hasPortNumber()) {
			throw new RuntimeException("Missing port number for Node declaration " + n.getName() + " in " + filename);
		}

		if (!n.hasCertificate()) {
			throw new RuntimeException("Missing public key certificate for Node declaration " + n.getName() + " in " + filename);
		}

		return new AquaeNode(
				n.getName(),
				n.getLocation().getHostname(),
				n.getLocation().getPortNumber(),
				n.getCertificate().toByteArray());
	}

	private AquaeNode findNode(String name) {
		return nodes.get(name);
	}

	/// AquaeAgreement helpers
	private AquaeAgreement Agreement2AquaeAgreement(uk.gov.Aquae.ProtocolBuffers.Metadata.DSA a) {
		debug("Agreement2AquaeAgreement: Ignoring " + a.getJustification() + "\n");

		return new AquaeAgreement();
	}

	private AquaeAgreement findAgreement(String name) {
		return agreements.get(name);
	}


	/// AquaeDataStructures.ConfidenceAttribute helpers
	private AquaeDataStructures.ConfidenceAttribute ConfidenceAttribute2ConfidenceAttribute(uk.gov.Aquae.ProtocolBuffers.Metadata.ConfidenceAttribute a) {
		// ConfidenceAttributes are straighforward because they
		// do not reference any other objects in the Metadata
		// file.

		if (a == null) {
			throw new RuntimeException("Null reference for ConfidenceAttribute in " + filename);
		}

		if (!a.hasName()) {
			throw new RuntimeException("Missing Name for ConfidenceAttribute declaration " + a + " in " + filename);
		}

		if (!a.hasDescription()) {
			throw new RuntimeException("Missing Description for ConfidenceAttribute declaration " + a.getName() + " in " + filename);
		}

		return new AquaeDataStructures.ConfidenceAttribute(
				a.getName(),
				a.getDescription());
	}

	private AquaeDataStructures.ConfidenceAttribute findConfidenceAttribute(String name) {
		return confidence.get(name);
	}


	/// AquaeQuery helpers

	// Validate and internalise a QuerySpec Message.
	// For each query we find the implementing node in the
	// pre-existing node list.
	// For each choice we find any required queries in the supplied
	// list of queries as we're still in the process of building
	// it.
	private AquaeQuery QuerySpec2AquaeQuery(Map<String, AquaeQuery> queries, uk.gov.Aquae.ProtocolBuffers.Metadata.QuerySpec q) {
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.ImplementingNode> iin    = null;
		Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.Choice>           ic     = null;
		AquaeQuery                                                       result = null;

		if (q == null) {
			throw new RuntimeException("Null reference for Query in " + filename);
		}

		if (!q.hasName()) {
			throw new RuntimeException("Missing name for Query declaration " + q + " in " + filename);
		}

		if (q.getNodeCount() < 1) {
			throw new RuntimeException("Query " + q.getName() + " is not implemented by any nodes!");
		}

		result = new AquaeQuery(
				q.getName()
				);


		// Implementing Nodes
		iin = q.getNodeList().iterator();
		while(iin.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.ImplementingNode in  = iin.next();
			AquaeNode                                              ain = null;
			AquaeDataStructures.MatchingRequirements               mr  = null;

			if (!in.hasNodeId()) {
				throw new RuntimeException("Missing nodename of implementing node clause for Query declaration " + q.getName() + " in " + filename);
			}

			ain = findNode(in.getNodeId());

			// TODO: fix exception type as per nodes.put
			// check.
			if (ain == null) {
				throw new RuntimeException("Query " + q.getName() + " references an undeclared node " + in.getNodeId() + " in " + filename);
			}

			if (in.hasMatchingRequirements()) {
				uk.gov.Aquae.ProtocolBuffers.Metadata.MatchingSpec pmr                  = in.getMatchingRequirements();
				Set<IdentityAttribute>                             required             = null;
				Set<IdentityAttribute>                             disambiguators       = null;
				Set<AquaeDataStructures.ConfidenceAttribute>       confidenceAttributes = null;

				if (pmr.getRequiredCount() > 0) {
					Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.MatchingSpec.IdFields> iidf = null;

					required = new HashSet<IdentityAttribute>();

					iidf = pmr.getRequiredList().iterator();
					while (iidf.hasNext()) {
						uk.gov.Aquae.ProtocolBuffers.Metadata.MatchingSpec.IdFields idf = iidf.next();
						IdentityAttribute                                           ms  = null;

						ms = IdentityAttribute.values()[idf.ordinal()]; // This assumes that the IdentityAttribute enum matches the IdFields enum. If it doesn't then we're in trouble! Maybe we should use a value mapping rather than an ordinal mapping?
						if (!required.add(ms)) {
							throw new RuntimeException("Query " + q.getName() + " has a matching requirement with a duplicated required field " + ms + " in " + filename);
						}
					}


				}

				if (pmr.getDisambiguatorsCount() > 0) {
					Iterator<uk.gov.Aquae.ProtocolBuffers.Metadata.MatchingSpec.IdFields> iidf = null;

					disambiguators = new HashSet<IdentityAttribute>();

					iidf = pmr.getDisambiguatorsList().iterator();
					while (iidf.hasNext()) {
						uk.gov.Aquae.ProtocolBuffers.Metadata.MatchingSpec.IdFields idf = iidf.next();
						IdentityAttribute                                           ms  = null;

						ms = IdentityAttribute.values()[idf.ordinal()]; // This assumes that the IdentityAttribute enum matches the IdFields enum. If it doesn't then we're in trouble! Maybe we should use a value mapping rather than an ordinal mapping?
						if (!disambiguators.add(ms)) {
							throw new RuntimeException("Query " + q.getName() + " has a matching requirement with a duplicated disambiguator field " + ms + " in " + filename);
						}
					}


				}

				if (pmr.getConfidenceBuildersCount() > 0) {
					Iterator<String> istr = null;

					confidenceAttributes = new HashSet<AquaeDataStructures.ConfidenceAttribute>();

					istr = pmr.getConfidenceBuildersList().iterator();
					while(istr.hasNext()) {
						String                                  str = istr.next();
						AquaeDataStructures.ConfidenceAttribute ca  = null;

						ca = findConfidenceAttribute(str);

						if (ca == null) {
							throw new RuntimeException("Query " + q.getName() + " references an undeclared Confidence Attribute " + str + " in " + filename);
						}

						if (!confidenceAttributes.add(ca)) {
							throw new RuntimeException("Query " + q.getName() + " has a duplicated Confidence Attribute field " + str + " in " + filename);
						}
					}
				}

				mr = new AquaeDataStructures.MatchingRequirements(required, disambiguators, confidenceAttributes);
			}

			result.addImplementor(ain, mr);
		}


		// Dependency Choices
		ic = q.getChoiceList().iterator();
		while(ic.hasNext()) {
			uk.gov.Aquae.ProtocolBuffers.Metadata.Choice c   = ic.next();
			Iterator<String>                             irq = null;
			List<AquaeQuery>                             rql = null;

			if (c.getRequiredQueryCount() < 1) {
				throw new RuntimeException("Missing Required Query name(s) for Choice in Query " + q.getName() + " in " + filename);
			}

			rql = new ArrayList<AquaeQuery>();
			irq = c.getRequiredQueryList().iterator();
			while(irq.hasNext()) {
				String     rq  = irq.next();
				AquaeQuery rqq = null;

				rqq = queries.get(rq);
				if (rqq == null) {
					throw new RuntimeException("Query Choice for " + q.getName() + " references undeclared Query " + rq + " in " + filename);
				}
				rql.add(rqq);
			}
			result.addChoice(rql);
		}

		return result;
	}

	public AquaeQuery findQuery(String name) {
		return queries.get(name);
	}
}

///////////////////////////////////////////////////////////////////////////////

