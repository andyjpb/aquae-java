///////////////////////////////////////////////////////////////////////////////
///
/// viaduct - An Aquae server in Java.
///
///  gov.uk Personal Data Exchange is a way to query existing personal data held
///  by government.
///
///  Viaduct is an implementation of an Aquae server in Java.
///  Aquae is the underlying protocol used to describe and transport question
///  and answer style eligibility queries within a federation of cooperating
///  nodes.
///  ViaductConfig is the high level object that represents a configuration for
///  the ViaductD server.
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
/// Andy Bennett <andyjpb@digital.cabinet-office.gov.uk>, 2017/09
///
///////////////////////////////////////////////////////////////////////////////

// ./run-viaduct <config-file>


import java.io.*;
import java.util.*;

import uk.gov.Aquae.AquaeMetadata;



////////////////////////////////////////////////////////////////////////////////
/// Viaduct Runtime Configuration

class ViaductConfig {
	private final String                     filename;
	private final Map<String, AquaeMetadata> metadata;
	public  final List<Listener>             listeners;

	ViaductConfig(String filename) throws FileNotFoundException, IOException {
		ProtocolBuffers.DaemonConfig       c = null;
		String                             s = null;
		Iterator<ProtocolBuffers.Listener> i = null;

		this.metadata  = new HashMap<String, AquaeMetadata>();
		this.listeners = new ArrayList<Listener>();

		s = new File(filename).getCanonicalPath();
		System.err.print("Reading " + s + ".\n");
		c = ProtocolBuffers.DaemonConfig.parseFrom(new FileInputStream(s));
		this.filename = s;

		// Find all the Metadata references
		i = c.getListenerList().iterator();
		while(i.hasNext()) {
			ProtocolBuffers.Listener l = i.next();
			String                   f = null;

			if (!l.hasMetadataFile()) {
				throw new RuntimeException("Listener " + l + " must have a metadata_file!");
			}
			f = new File(l.getMetadataFile()).getCanonicalPath();

			if (metadata.get(f) == null) {
				metadata.put(f, new AquaeMetadata(f));
			}
		}

		// Process all the Listeners
		i = c.getListenerList().iterator();
		while(i.hasNext()) {
			ProtocolBuffers.Listener l = i.next();

			listeners.add(new Listener(l));
		}
	}

	class Listener {
		public  final String        node_name;
		public  final int           port;
		private final String        metadata_file;
		private final AquaeMetadata metadata;
		private final List<String>  queries;

		private Listener(ProtocolBuffers.Listener l) throws IOException {
			int           p = 0;
			String        s = null;
			AquaeMetadata m = null;
			//AquaeNode     n = null;

			// node_name
			if (l == null) {
				throw new RuntimeException("Listener cannot be null!");
			}

			if (!l.hasNodeName()) {
				throw new RuntimeException("Listener " + l + " must have a node_name!");
			}
			this.node_name = l.getNodeName();

			// port
			if (!l.hasPort()) {
				throw new RuntimeException("Listener " + l + " must have a port!");
			}

			p = l.getPort();
			if ((p <=0) || (p > 65535)) {
				throw new RuntimeException("Listener " + l + " has invalid port number " + p + "!");
			}
			this.port = p;

			// metadata
			if (!l.hasMetadataFile()) {
				throw new RuntimeException("Listener " + l + " must have a metadata_file!");
			}
			s = new File(l.getMetadataFile()).getCanonicalPath();
			m = ViaductConfig.this.metadata.get(s);
			if (m == null) {
				throw new RuntimeException("Something went wrong when finding metadata for listener " + l + "!");
			}
			this.metadata_file = s;
			this.metadata      = m;

			//TODO
			//n = m.findNode(node_name);
			//if n is null, emit a warning
			//if not null, check the port
			//if the port does not match, emit a warning

			// query list
			this.queries = new ArrayList<String>();
			// TODO: Parse this list and do something with it.
			//this.queries = new ArrayList(l.getQueryList());
			//q = m.findQuery(query_name);
			//if q is null, emit a warning
			//if not null, check the implementorByNode using n from above.
		}
	}
}

