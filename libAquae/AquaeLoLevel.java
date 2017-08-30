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
///  AquaeLoLevel is a thin wrapper around the wire protocol that lifts the
///  domain objects up to a useful level of abstraction for the higher level
///  drivers and state machines.
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
/// Aquae LoLevel

// This is the main Aquae runtime context. - no it's not! but it has inner classes rather than nested classes so that it can reference one.
// These are the core Aquae data structures.
public class AquaeLoLevel {
	final PrintStream err;
	final String      nodeName;

	public AquaeLoLevel(String nodeName) {
		this.err      = System.err;
		this.nodeName = nodeName;
	}

	// Tools
	void debug(String s) {
		err.print(s);
	}


	// Inner Classes give us a bit of closure over the global variables.


	////////////////////////////////////////////////////////////////////////
	// Aquae consists of exchanging messages over a pair of ports; usually
	// a TLS socket. Here we marshall those messags on and off the wire
	// using the Aquae Framing and Encapsulation Protocols.
	public class Transport {
		private final int          maximum_payload_size = 1024 * 1024; // 1MiB
		private final InputStream  in;
		private final OutputStream out;

		// The Transport State Machine.
		// When the state is WAITING_FOR_PAYLOAD, expected_payload
		// tells us which substate we are in.
		// reset() puts the state machine in the inital state.
		// state must change every time an in.read() call returns one
		// or more bytes.
		TransportState state             = null;
		MsgType        expected_payload  = null;
		int            payload_remaining = 0;
		byte[]         payload_buffer    = null;


		public Transport(InputStream in, OutputStream out) {
			this.in  = in;
			this.out = out;
			//TODO: If in is ready, put the state machine into the WAITING_FOR_FRAME state.
			this.state = TransportState.WAITING_FOR_FRAME;
		}


		public AquaeLoLevel.MsgType read() throws TransportException, IOException {
			int    first_byte       = 0;
			int    version_number   = 0;
			int    reserved_bits    = 0;
			int    header_length    = 0;
			int    offset           = 0;
			int    header_remaining = 0;
			byte[] header_buffer    = null;
			uk.gov.Aquae.ProtocolBuffers.Transport.Header header = null;


			if (state != TransportState.WAITING_FOR_FRAME) {
				throw new TransportRuntimeException("Transport not in WAITING_FOR_FRAME state!");
			}
			if (expected_payload != null) {
				throw new TransportRuntimeException("Transport in WAITING_FOR_FRAME state but still expecting a payload!");
			}
			if (payload_remaining != 0) {
				throw new TransportRuntimeException("Transport in WAITING_FOR_FRAME state but still has payload left to read!");
			}
			if (payload_buffer != null) {
				throw new TransportRuntimeException("Transport in WAITING_FOR_FRAME state but still has buffer space allocated!");

			}


			// Read the version number off the wire.
			first_byte = in.read();
			if (first_byte == -1) {
				throw new TransportException("Transport input stream reached unexpected end of file whilst reading first byte of frame!");
			}
			state = TransportState.READ_FIRST_BYTE;

			version_number = (0xF0 & first_byte) >>> 4;
			reserved_bits  = (0x0F & first_byte);

			if (version_number != 0) {
				throw new TransportException("Transport input stream encountered unexpected version number: " + version_number);
			}
			if (reserved_bits != 0) {
				throw new TransportException("Transport input stream encountered non-zero reserved bits in Encapsulation Protocol!");
			}

			// Read the header length off the wire.
			header_length = in.read();
			if (header_length == -1) {
				throw new TransportException("Transport input stream reached unexpected end of file whilst reading header length!");
			}
			state = TransportState.READ_HEADER_LENGTH;

			header_buffer = new byte[header_length]; // All zero by JVM.
			state = TransportState.WAITING_FOR_HEADER;

			// Read & parse the header itself.
			while (header_remaining > 0) {
				int read = 0;

				read = in.read(header_buffer, offset, header_remaining);

				if (read == -1) {
					throw new TransportException("Transport input stream reached unexpected end of file!");
				}
				state = TransportState.READING_HEADER;

				offset            = offset            + read;
				header_remaining  = header_remaining  - read;
			}
			state = TransportState.PARSING_HEADER;

			header = uk.gov.Aquae.ProtocolBuffers.Transport.Header.parseFrom(header_buffer);

			if (header == null) {
				throw new TransportException("Transport input stream recieved a null header!");
			}

			if (!header.hasLength()) {
				throw new TransportException("Transport input stream received a header without a length field!");
			}

			if (header.getLength() > maximum_payload_size) {
				throw new TransportException("Transport input stream received a header with an unreasonably large length field! Peer wants to send " + header.getLength() + " bytes and our limit is " + maximum_payload_size);
			}

			if (!header.hasType()) {
				throw new TransportException("Transport input stream received a header without a type field!");
			}

			payload_remaining = header.getLength();
			payload_buffer    = new byte[payload_remaining]; // All zero by JVM.
			expected_payload  = MsgType.values()[header.getType().ordinal()]; // This assumes that the MsgType enum matches the Header.Type enum. If it doesn't then we're in trouble! Maybe we should use a value mapping rather than an ordinal mapping?
			state = TransportState.WAITING_FOR_PAYLOAD;

			return expected_payload;
		}

		// Thrown when the API is used incorrectly.
		private class TransportRuntimeException extends RuntimeException {
			TransportRuntimeException() {
				super();
			}
			TransportRuntimeException(String s) {
				super(s);
			}
		}

		// Thrown when we encounter a recoverable error.
		private class TransportException extends Exception {
			TransportException() {
				super();
			}
			TransportException(String s) {
				super(s);
			}
		}

		private void readPayload() throws TransportException, IOException {
			int offset = 0;


			if (state != TransportState.WAITING_FOR_PAYLOAD) {
				throw new TransportRuntimeException("Transport not in WAITING_FOR_PAYLOAD state!");
			}
			if (expected_payload == null) {
				throw new TransportRuntimeException("Transport is not expecting a payload!");
			}
			if (payload_remaining <= 0) {
				throw new TransportRuntimeException("Transport has no more payload left to read!");
			}
			if (payload_buffer.length <= payload_remaining) {
				throw new TransportRuntimeException("Trnasport has more payload left to read than total buffer space!");
			}


			while (payload_remaining > 0) {
				int read = 0;

				read = in.read(payload_buffer, offset, payload_remaining);

				if (read == -1) {
					throw new TransportException("Transport input stream reached unexpected end of file!");
				}
				state = TransportState.READING_PAYLOAD;

				offset            = offset            + read;
				payload_remaining = payload_remaining - read;
			}
			state = TransportState.READ_PAYLOAD;

			expected_payload = null;
			state            = TransportState.WAITING_FOR_FRAME;
		}

		// TODO: drainPayload()?
		// TODO: Messsage readFrame()?

		public IdentitySignRequest readIdentitySignRequest() throws TransportException, IOException {

			if (state != TransportState.WAITING_FOR_PAYLOAD) {
				throw new TransportRuntimeException("Transport not in WAITING_FOR_PAYLOAD state!");
			}
			if (expected_payload != MsgType.IDENTITY_SIGN_REQUEST) {
				throw new TransportRuntimeException("Transport is not expecting to read an IDENTITY_SIGN_REQUEST message at this time!");
			}

			readPayload();

			return new IdentitySignRequest(payload_buffer);
		}

		public SignedQuery readSignedQuery() throws TransportException, IOException {

			if (state != TransportState.WAITING_FOR_PAYLOAD) {
				throw new TransportRuntimeException("Transport not in WAITING_FOR_PAYLOAD state!");
			}
			if (expected_payload != MsgType.SIGNED_QUERY) {
				throw new TransportRuntimeException("Transport is not expecting to read a SIGNED_QUERY message at this time!");
			}

			readPayload();

			return new SignedQuery(payload_buffer);
		}

		// TODO: readers for the other messages...

	}

	private static enum TransportState {
		WAITING_FOR_FRAME,
		READ_FIRST_BYTE,
		READ_HEADER_LENGTH,
		WAITING_FOR_HEADER,
		READING_HEADER,
		PARSING_HEADER,
		WAITING_FOR_PAYLOAD,
		READING_PAYLOAD,
		READ_PAYLOAD,
	}

	// The ordinal values of this enum MUST match those of the Header Type
	// field. That means that these names MUST appear in the same order
	// here as they do in the Protobuf declaration. Maybe we should use a
	// value-mapping rather than an ordinal-mapping?
	public static enum MsgType {
		IDENTITY_SIGN_REQUEST(),
		SIGNED_QUERY(),
		BAD_QUERY_RESPONSE(),
		QUERY_RESPONSE(),
		SECOND_WHISTLE(),
		QUERY_ANSWER(),
		FINISH(),
	}
	/*
	public static enum MsgType {
		IDENTITY_SIGN_REQUEST (1, IdentitySignRequest.class),
		SIGNED_QUERY          ,
		BAD_QUERY_RESPONSE    ,
		QUERY_RESPONSE        ,
		SECOND_WHISTLE        ,
		QUERY_ANSWER          ,
		FINISH                ,
		;

		private final int                      ordinal;
		private final Class<? extends Message> the_class;

		MsgType(int ordinal, Class<? extends Message> the_class) {
			this.ordinal   = ordinal;
			this.the_class = the_class;

			System.out.println("MsgType Constructor for " + ordinal);
		}

		public Message newMessage(byte[] buffer) {
			Message  m    = null;

			try {
				// Reference to the outer class is null because enums are implicitly static.
				// http://docs.oracle.com/javase/specs/jls/se7/html/jls-8.html#jls-8.9
				m = the_class.getConstructor(this.getClass().getEnclosingClass(), byte[].class).newInstance(null, buffer);
			} catch (NoSuchMethodException e) {
				System.out.println(e);
				// Thrown from getConstructor
			} catch (InstantiationException e) {
				System.out.println(e);
				// Thrown from newInstance
			} catch (IllegalAccessException e) {
				System.out.println(e);
				// Thrown from newInstance
			} catch (java.lang.reflect.InvocationTargetException e) {
				System.out.println(e);
				//InvocationTargetException
				// Thrown from newInstance
			}

			return m;

		}
	}
	*/


	////////////////////////////////////////////////////////////////////////
	// Aquae consists of exchanging messages Here we provide the domain
	// objects that implement the Aquae Messaging Protocol.
	abstract public class Message {
		// TODO: should this be an interface? It doesn't really define anything.

	}

	public class IdentitySignRequest extends Message {
		/*
		 * message IdentitySignRequest {
		 *   optional PersonIdentity subjectIdentity = 1;
		 *   // TODO: also need to send the query that we want to run. Then identity bridge verifies.
		 *   repeated string identitySetNodes = 2;
		 * }
		 */


		// TODO: Construct by hand

		// Construct from a Protocol Buffer from the Transport Layer
		private IdentitySignRequest(uk.gov.Aquae.ProtocolBuffers.Messaging.IdentitySignRequest pb) {
			ConstructFromPB(pb);
		}

		// Construct from a buffer from the Transport Layer
		private IdentitySignRequest(byte[] buffer) {
			try {
				ConstructFromPB(uk.gov.Aquae.ProtocolBuffers.Messaging.IdentitySignRequest.parseFrom(buffer));
			} catch (com.google.protobuf.InvalidProtocolBufferException e) {

			}
		}

		private void ConstructFromPB(uk.gov.Aquae.ProtocolBuffers.Messaging.IdentitySignRequest pb) {

		}

		public void hi() {
			System.out.println("hello from IdentitySignRequest");
		}

		// TODO: Procedure to check consistency (and set a flag?)

	}

	public class SignedQuery extends Message {
		// TODO: Construct by hand

		// Construct from a Protocol Buffer from the Transport Layer
		private SignedQuery(uk.gov.Aquae.ProtocolBuffers.Messaging.IdentitySignRequest pb) {

		}

		// Construct from a buffer from the Transport Layer
		private SignedQuery(byte[] buffer) {
			//this(uk.gov.Aquae.ProtocolBuffers.Messaging.IdentitySignRequest.parseFrom(buffer));
		}

		public void hi() {
			System.out.println("hello from SignedQuery");
		}

		// TODO: Procedure to check consistency (and set a flag?)

	}


	////////////////////////////////////////////////////////////////////////

}

///////////////////////////////////////////////////////////////////////////////

