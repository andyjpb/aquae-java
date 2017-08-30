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
///  AquaeQuery is the high level query interface that allows a user to ask a
///  question and recieve an answer.
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


import java.util.*;

//import uk.gov.Aquae.*;



///////////////////////////////////////////////////////////////////////////////
/// Aquae Query

// This represents a question that a user can ask of the network. They can't
// construct them themselves: they have to be obtained by finding them through
// the Metadata.
// We use this class to represent fresh queries constructed by a user as well
// as queries that are received from another node.
public class AquaeQuery {
	// queryName is just used to dereference the graph in the Metadata.
	// Once the Metadata has been loaded we don't need it any more and
	// shouldn't use it for anything functional. However, we keep it around
	// for debugging messages.
	private final String                                                          queryName;
	private final Set<Implementor>                                                implementors;
	private final Map<AquaeNode,                                Set<Implementor>> implementorByNode;
	private final Map<AquaeDataStructures.MatchingRequirements, Set<Implementor>> implementorByMatchingRequirements;
	private final Set<Choice>                                                     choices;
	// TODO: keep track of the agreements that govern this query so that we can find them quickly when we are planning.

	// This constructor is responsible for ensuring that the structure it
	// produces is safe.  The arguments are allowed to contain unsanitised,
	// untrusted user input.
	AquaeQuery(String queryName) {
		// queryName can be any valid UTF-8 string.
		this.queryName = validateQueryName(queryName);

		this.implementors                      = new HashSet<Implementor>();
		this.implementorByMatchingRequirements = new HashMap<AquaeDataStructures.MatchingRequirements, Set<Implementor>>();
		this.implementorByNode                 = new HashMap<AquaeNode,                                Set<Implementor>>();
		this.choices                           = new HashSet<Choice>();
	}

	// Validators for fields that we will store this object against in
	// Maps.  These validators need to be static so that we can use them
	// without an AquaeQuery object already in hand.  They also need to be
	// able to work with untrusted data rather than just fishing out the
	// pre-sanitized member from an instance of an object.  Returns the
	// field if it's valid otherwise throws.

	static String validateQueryName(String queryName) {
		// queryName can be any valid UTF-8 string.
		return queryName;
	}


	// Lexicographical AquaeQuery Comparator by the name of the query.
	private static class AquaeQueryNameComparator implements Comparator<AquaeQuery> {
		public int compare(AquaeQuery x, AquaeQuery y) {
			if (x == y)    return  0; // Both null or same object.
			if (x == null) return -1; // null < anything => x < y
			if (y == null) return  1; //                 => x > y

			if (x.queryName == y.queryName) return  0; // Both fields null or same object.
			if (x.queryName == null)        return -1; // null < anything => x < y
			if (y.queryName == null)        return  1; //                 => x > y

			return x.queryName.compareTo(y.queryName);
		}
	}


	public class Implementation {

	}

	// A node that implements a query consists of the AquaeNode object and
	// optionally some other data such as that node's matching
	// requirements.
	private class Implementor {
		private final AquaeNode                                node;
		private final AquaeDataStructures.MatchingRequirements matchingRequirements;

		private Implementor(AquaeNode node, AquaeDataStructures.MatchingRequirements matchingRequirements) {
			if (node == null) {
				throw new RuntimeException("AquaeNode cannot be null when constructing an AquaeQuery.Implementor!");
			}
			this.node                 = node;
			this.matchingRequirements = matchingRequirements;
		}


		// Object.equals() only gives us reference equality.
		public boolean equals(Object o) {
			Implementor p = null;

			if (this == o) return true;
			if (!(o instanceof Implementor)) return false; // null is never an instanceof this

			p = (Implementor)(o);

			return (((node                 == null) ? (p.node                 == null) : node.equals(p.node)) &&
			        ((matchingRequirements == null) ? (p.matchingRequirements == null) : matchingRequirements.equals(p.matchingRequirements)));
		}

		// We overrode equals so we override hashCode() too in order to
		// maintain the invariant that objects compared equal with
		// equals() have the same hashCode()s.
		public int hashCode() {
			// MatchingRequirements are probably equal if their members have the hashCode()s.
			// 37 is a happy prime!

			int c = 1486746592;
			c = (37 * c) + ((node                 == null) ? 0 : node.hashCode());
			c = (37 * c) + ((matchingRequirements == null) ? 0 : matchingRequirements.hashCode());

			return c;
		}
	}

	public void addImplementor(AquaeNode node, AquaeDataStructures.MatchingRequirements mr) {
		Implementor      i        = new Implementor(node, mr);
		Set<Implementor> existing = null;

		// Add to implementors.
		if (!implementors.add(i)) {
			throw new RuntimeException("Cannot add implementor " + i + " to query " + this + ": we already know that node " + node + " implements this query with matching requirements " + mr + "!");
		}

		// If any of the rest of these fail then we have a bug as
		// successfully adding to implementors gurantees that this
		// Implementor is unique for this query.

		// Add to implementorByNode index.
		existing = implementorByNode.get(node);
		if (existing == null) {
			existing = new HashSet<Implementor>();
		}
		if (!existing.add(i)) {
			throw new RuntimeException("Bug in AquaeQuery.addImplementor() as we got a Set conflict whilst adding " + i + " to implementorByNode for " + this + "!");
		}
		implementorByNode.put(node, existing);

		// Add to implementorByMatchingRequirements index.
		existing = implementorByMatchingRequirements.get(mr);
		if (existing == null) {
			existing = new HashSet<Implementor>();
		}
		if (!existing.add(i)) {
			throw new RuntimeException("Bug in AquaeQuery.addImplementor() as we got a Set conflict whilst adding " + i + " to implementorByMatchingRequirements for " + this + "!");
		}
		implementorByMatchingRequirements.put(mr, existing);
	}


	private class Choice {
		private final List<AquaeQuery> queries; // This is not a Set because a query can legitimately be required more than once.

		Choice(List<AquaeQuery> queries) {
			if ((queries == null) || (queries.size() == 0)) {
				throw new RuntimeException("Required Query List cannot be null or zero length when constructing an AquaeQuery.Choice!");
			}
			this.queries = queries;
		}


		// Object.equals() only gives us reference equality.
		public boolean equals(Object o) {
			Choice p = null;

			if (this == o) return true;
			if (!(o instanceof Choice)) return false; // null is never an instanceof this

			p = (Choice)(o);

			return (((queries == null) ? (p.queries == null) : queries.equals(p.queries)));
		}

		// We overrode equals so we override hashCode() too in order to
		// maintain the invariant that objects compared equal with
		// equals() have the same hashCode()s.
		public int hashCode() {
			// MatchingRequirements are probably equal if their members have the hashCode()s.
			// 37 is a happy prime!

			int c = 1486746592;
			c = (37 * c) + ((queries == null) ? 0 : queries.hashCode());

			return c;
		}
	}

	public void addChoice(List<AquaeQuery> queries) {
		List<AquaeQuery> the_queries = new ArrayList<AquaeQuery>(queries); // Take a private copy because we need to manipulate its properties.
		Choice                     c = null;

		Collections.sort(the_queries, new AquaeQueryNameComparator()); // Canonicalise the list of queries.
		c = new Choice(the_queries);

		if (!choices.add(c)) {
			throw new RuntimeException("Cannot add choice " + c + " to query " + this + ": this query already knows about that choice!");
		}
	}


	public class Plan {
	// TODO: state : pointers to the metadata, the name of the asking node and the name of the asked node.
		String[] IdentityRequirements; // nodes and their id field reqs

		void set_identity(AquaeDataStructures.Person p) {
			// check that all the right fields in p are set based on the IdentityRequirements.
			// stash p somewhere
			// send the identity off to the identity bridge (from the plan) for signing
			// stash the signed identity somewhere. it'll be opaque

		}

		// ...

		public Result execute() {

			return null;
		}

	}

	public class Result {
		// There's some kind of structure in here that maps the answers to their types and values

	}


	public Choice[] getChoices() {

		return null;
	}

	public Plan plan(Choice c) {

		return null;
	}
}

