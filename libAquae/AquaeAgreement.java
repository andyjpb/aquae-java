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
///  AquaeAgreement is the high level object that represents a Data Sharing
///  Agreement between a set of  node in an Aquae /  Federation.
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



///////////////////////////////////////////////////////////////////////////////
/// Aquae Agreement

// This represents a question that a user can ask of the network. They can't
// construct them themselves: they have to be obtained by finding them through
// the Metadata.
// We use this class to represent fresh queries constructed by a user as well
// as queries that are received from another node.
// TODO: Fix these comments
public class AquaeAgreement {
	// state : pointers to the metadata,

	AquaeAgreement() {

	}

}

