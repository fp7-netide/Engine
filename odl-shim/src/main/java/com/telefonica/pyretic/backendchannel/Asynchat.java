/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     Telefonica I+D
 */


package com.telefonica.pyretic.backendchannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Selector;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * A class supporting chat-style (command/response) protocols.
 *
 * This is an abstract class. You must derive from this class, and implement
 * the two methods collectIncomingData and foundTerminator.
 *
 */
abstract class Asynchat extends Dispatcher {
    static final int BUFFER_SIZE = 4096*3;
    static Charset charset = Charset.forName("US-ASCII");
    static CharsetEncoder encoder = charset.newEncoder();
    static CharsetDecoder decoder = charset.newDecoder();
    String terminator = null;
    ByteBuffer outbuf;
    ByteBuffer inbuf;
    int termlen = 0;

    Asynchat() throws IOException {
        super();
        this.initAsynchat();
    }

    /**
     * Constructor.
     *
     * @param selector  Selector used for this channel
     */
    Asynchat(Selector selector) throws IOException {
        super(selector);
        this.initAsynchat();
    }

    void initAsynchat() {
        this.outbuf = ByteBuffer.allocate(BUFFER_SIZE);
        this.outbuf.flip();
        this.inbuf = ByteBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Set the terminator.
     *
     * @param terminator the terminator
     */
    void setTerminator(String terminator) {
        this.terminator = terminator;
        this.termlen = terminator.length();
    }

    /**
     * This method is invoked when data has been read into <code>str</code>
     * from the channel.
     *
     * @param str   received string
     */
    abstract void collectIncomingData(String str);

    /**
     * This method is invoked when the terminator has been received from the
     * channel.
     */
    abstract void foundTerminator();

    boolean readyToWrite() {
        return (this.state.isWritable() && this.outbuf.remaining() != 0);
    }

    void initiateSend() throws IOException {
        super.send(this.outbuf);
    }

    void send(String str) throws java.nio.BufferOverflowException {
        int len = str.length();
        if (len == 0) return;
        if (len > this.outbuf.remaining()) {
            this.outbuf.compact();
            this.outbuf.flip();
        }

        this.outbuf.mark();
        this.outbuf.position(this.outbuf.limit());
        this.outbuf.limit(this.outbuf.capacity());
        try {
            this.outbuf.put(this.encoder.encode(CharBuffer.wrap(str)));
        } catch (CharacterCodingException e) {
            //logger.severe(e.toString());
            System.exit(1);
        }
        this.outbuf.limit(this.outbuf.position());
        this.outbuf.reset();
    }

    String recv() {
        try {
            int count = super.recv(this.inbuf);
            this.inbuf.flip();
            String str =  this.decoder.decode(this.inbuf).toString();
            this.inbuf.clear();
            return str;
        } catch (IOException e) {
            //logger.severe(e.toString());
            this.handleClose();
            return "";
        }
    }

    void handleWrite() {
        try {
            this.initiateSend();
        } catch (IOException e) {
            //logger.severe(e.toString());
            this.handleClose();
        }
    }
    void handleRead() {

        String str = this.recv();
        if (str == null || str.length() == 0)
            return;
        if (this.terminator == null) {
            this.collectIncomingData(str);
            return;
        }

        System.out.println("----- handle read: str->" + str);
        int pos = 0;
        int len = str.length();
        while (pos < len) {
            int index = str.indexOf(this.terminator, pos);
            if (index == -1) {
                this.collectIncomingData(str.substring(pos));
                break;
            } else {
                // don't report an empty string
                if (index > 0)
                    this.collectIncomingData(str.substring(pos, index));
                pos = index + this.termlen;
                this.foundTerminator();
            }
        }
    }
}
