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

// ./run-viaduct <config-file>


import java.io.*;
import java.net.*;
import java.util.*;

// import libAqaue;



////////////////////////////////////////////////////////////////////////////////
/// Framework
/// Mostly borrowed from https://github.com/alecmuffett/jchargen/blob/master/JChargen.java

class Tools {

	static void debug(long id, String s) {
		System.out.print("[" + id + "] " + s + "\n");
	}

	static void debug(long id, String s, String t) {
		System.out.print("[" + id + "] " + s + ": " + t + "\n");
	}

	static void debug(long id, Exception e) {
		System.out.print("[" + id + "] exception: " + e.getMessage() + "\n");
	}
}


class ViaductListener implements Runnable {
	private final int    port;
	private final String nodeName;

	ViaductListener(int port, String nodeName) {
		this.port     = port;
		this.nodeName = nodeName;
	}

	public void run() {
		ServerSocket server = null;
		long         id     = Thread.currentThread().getId();

		System.err.print("Coming up for Aquae requests as " + nodeName + " on port " + port + "...\n\n");
		try {
			server = new ServerSocket(port);
			System.err.print("Viaduct is ready for action!\n - aquae://localhost:" + port + "/\n\n");
			while (true) {
				new Thread(new ViaductWorker(server.accept())).start();
			}
		} catch (Exception e) {
			Tools.debug(id, e);
		} finally {
			try {
				server.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
}

////////////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////////////
/// Worker Thread
/// This is the main meat of the Viaduct Daemon Implementation.
/// We have a Worker for each TCP connection. Currently there is one TCP
/// connection for each Query that is in flight at this node.

class ViaductWorker implements Runnable {
	Socket sock;

	ViaductWorker(Socket sock) {
		this.sock = sock;
	}

	void chat() throws IOException {
		System.err.print("Hello from the thread!\n");
		sock.getOutputStream().write("hi\n".getBytes());
	}

	public void run() {
		long start = System.currentTimeMillis();
		long id = Thread.currentThread().getId();
		Tools.debug(id, "new-worker");

		try {
			chat();
		} catch (Exception e) {
			Tools.debug(id, e);
		} finally {
			try {
				long stop = System.currentTimeMillis();
				long delta = stop - start;
				long divisor = delta / 1000;
				long bps = 0;
				Tools.debug(id,
						"wrote",
						" millis=" + delta +
						" bps=" + bps
					   );
				sock.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
}

////////////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////////////
/// Main Program Logic

// The Viaduct Daemon
public class ViaductD {

	static {
		// Require assertions to be enabled.
		// We need to make sure that we never use assert in a way that
		// can enable a denial of service attack.
		boolean assertsEnabled = false;
		assert assertsEnabled = true; // Intentional side effect!!!
		if (!assertsEnabled)
			throw new RuntimeException("Asserts must be enabled!!!");
	}

	private static void usage(int exit_code) {
		System.err.print("Usage: ViaductD <configuration-file>\n");
		if (exit_code > 0) {
			System.exit(exit_code);
		}
	}

	public static void main(String[] args) throws Exception {
		String                           config_file = null;
		ViaductConfig                    config      = null;
		Iterator<ViaductConfig.Listener> ivl         = null;
		List<Thread>                     threads     = new ArrayList<Thread>();
		Iterator<Thread>                 it          = null;

		if (args.length != 1) {
			usage(1);
		}
		config_file = args[0];

		System.err.print("Viaduct on Java v0.1\n");
		System.err.print("Copyright (C) 2017, The Personal Data Exchange Team, Crown Copyright (Government Digital Service)\n");
		System.err.print("\n");
		System.err.print("Written by Andy Bennett <andyjpb@digital.cabinet-office.gov.uk, 2017/08/09\n");
		System.err.print("\n");

		config = new ViaductConfig(config_file);
		System.err.print("\n");

		if (config == null) {
			throw new RuntimeException("Failed to read config file!");
		}

		ivl = config.listeners.iterator();
		while(ivl.hasNext()) {
			ViaductListener        listener = null;
			Thread                 thread   = null;
			ViaductConfig.Listener l        = ivl.next();

			listener = new ViaductListener(l.port, l.node_name); // TODO: change argument to a pointer to the config?
			thread   = new Thread(listener);
			thread.start();

			threads.add(thread);
		}

		it = threads.iterator();
		while (it.hasNext()) {
			Thread t = it.next();

			t.join();
		}
	}
}

////////////////////////////////////////////////////////////////////////////////

