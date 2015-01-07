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

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

//TODO clean code to be more readable and refactor

/**
 * Implementation of the python asyncore design pattern.
 *
 * The basic idea behind the asyncore design is that you must instantiate the
 * initial channels from subclasses of Dispatcher, then register them with a
 * Selector, and start the Dispatcher loop.
 *
 * Registering a channel to a Selector is done by invoking  the createSocket
 * method, or by using a constructor that takes a SocketChannel as parameter.
 * The class default_selector is used as the Selector by default.
 *
 * Each channel gets IO events through the handleRead, handleWrite,
 * handleAccept and handleConnect methods. Note that all events are not always
 * meaningful depending on the socket type, for example outgoing connections
 * never receive handleAccept events.
 *
 * All channels receive timer events through the handleTick method, and it is
 * possible to instantiate a channel that receives only timer events by
 * subclassing the Timer class.
 *
 * As a workaround against a bug that occurs with java.nio on linux, a
 * Dispatcher instance should always have at least one of its 'readyTo' methods
 * return True. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
 *
 */
abstract class Dispatcher{
    static final long SELECT_TIMEOUT = 20L;
    static final long USER_TIMEOUT = 200L;
    static final String CONCURRENT_MODIFICATION =
            "possibly skipping events after a close, we will get them on next select";
    //static Logger logger = Logger.getLogger("com.telefonica.pyretic");
    static Selector default_selector = null;

    Selector selector;
    ConnectionState state;
    SelectableChannel channel;
    InetSocketAddress address;

    Dispatcher() throws java.io.IOException {
        this.initDispatcher(null, null);
    }

    Dispatcher(Selector selector) throws java.io.IOException {
        this.initDispatcher(selector, null);
    }

    Dispatcher(SocketChannel channel) throws java.io.IOException {
        assert channel != null :  "null channel";
        this.initDispatcher(null, channel);
        this.setChannel(channel);
        this.state.connected();
    }

    /**
     * Use this constructor when the SocketChannel parameter has been obtained
     * from an invocation of handleAccept by another listening channel.
     *
     * @param selector  Selector used for this channel
     * @param channel   associated SocketChannel instance
     */
    Dispatcher(Selector selector, SocketChannel channel)
            throws java.io.IOException {
        assert channel != null :  "null channel";
        this.initDispatcher(selector, channel);
        this.setChannel(channel);
        this.state.connected();
    }

    void initDispatcher(Selector selector, SelectableChannel channel)
            throws java.io.IOException {
        if (selector == null) {
            if (default_selector == null)
                default_selector = Selector.open();
            selector = default_selector;
        }
        this.selector = selector;
        this.state = new ConnectionState();
        this.channel = channel;
        this.address = null;
    }

    /**
     * This method is invoked whenever readyToRead() is true, and there is data
     * ready to be read from the channel.
     */
    abstract void handleRead();

    /**
     * This method is invoked whenever readyToWrite() is true, and the channel
     * can be written to (a socket can be written to when it is connected and
     * when the internal kernel buffer is not full).
     */
    abstract void handleWrite();

    /**
     * This method is invoked whenever readyToAccept() is true, and there is an
     * incoming connection request.
     *
     * @param channel of the incoming connection request
     */
    abstract void handleAccept(SocketChannel channel);

    /**
     * This method is invoked when readyToConnect() is true, the channel is
     * associated with an outgoing connection and the socket three-way
     * connection handshake has been successfully completed.
     */
    abstract void handleConnect();

    /**
     * This method is invoked at each timer event.
     */
    abstract void handleTick();

    /**
     * This method is invoked when the channel is about to close. It is invoked
     * after a remote disconnection or on exceptionnal events such as
     * IOException exceptions. A subclass of Dispatcher that overrides this
     * method must call super.handleClose();
     */
    void handleClose() {
        this.close();
    }

    /* One must override the readyToWrite() method to restrict the condition to
     * having data ready to be written. Failing to do so will cause the loop to
     * spin continuously reporting that the channel is ready to be writtent to,
     * and consume a lot of cpu. See how this is done in class Asynchat.
     */
    boolean readyToWrite() { return this.state.isWritable(); }
    boolean readyToRead() { return this.state.isReadable(); }
    boolean readyToAccept() { return this.state.isAcceptable(); }
    boolean readyToConnect() { return this.state.isConnectable(); }

    /* Return the associated SocketChannel instance. */
    SocketChannel getSocketChannel() {
        assert this.channel != null :  "null channel";
        assert this.channel instanceof SocketChannel : "not a socket";
        return (SocketChannel) this.channel;
    }

    /* Return the associated ServerSocketChannel instance. */
    ServerSocketChannel getServerSocketChannel() {
        assert this.channel != null :  "null channel";
        assert this.channel instanceof ServerSocketChannel : "not a server";
        return (ServerSocketChannel) this.channel;
    }

    /**
     * @return the InetSocketAddress of the socket
     */
    InetSocketAddress getInetSocketAddress() {
        return this.address;
    }

    /**
     * @return true when the socket is connected
     */
    boolean connected() {
        return this.state.isWritable();
    }

    void addChannel() throws ClosedChannelException {
        assert this.channel != null :  "null channel";
        //logger.info("addChannel" + this.toString());
        this.channel.register(this.selector, 0, this);
    }

    void delChannel() {
        if (this.channel != null) {
            SelectionKey key = this.channel.keyFor(this.selector);
            if (key != null)
                key.cancel();
            this.channel = null;
        }
    }

    /**
     * Register the channel with its associated Selector.
     *
     * @param server true when the channel must be associated with a listening
     *               ServerSocketChannel, false when it must be associated
     *               with an outgoing SocketChannel
     */
    void createSocket(boolean server) throws java.io.IOException {
        if (server) {
            this.channel = (SelectableChannel) ServerSocketChannel.open();
        } else {
            this.channel = (SelectableChannel) SocketChannel.open();
        }
        this.setChannel(channel);
    }

    /**
     * Call this method with the SocketChannel obtained through a handleAccept
     * event as its parameter, in order to register the channel with
     * its selector.
     *
     * @param channel the SelectableChannel to register with the selector
     */
    void setChannel(SelectableChannel channel)
            throws java.io.IOException,
            ClosedChannelException {
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.addChannel();
    }

    /**
     * Set the SO_REUSEADDR socket option that controls whether `bind' should
     * permit reuse of local addresses for this socket.
     */
    void setReuseAddr(){
        assert this.channel != null :  "null channel";
        if (this.channel instanceof ServerSocketChannel) {
            ServerSocketChannel server = (ServerSocketChannel) this.channel;
            try {
                server.socket().setReuseAddress(true);
            } catch (java.net.SocketException e){ /* ignore */ }
        }
    }

    /**
     *  Listen for connections made to the socket.
     */
    void listen() { this.state.accepting(); }

    /**
     * Bind the socket to host, port.
     *
     * @param host  host name
     * @param port  port number
     */
    void bind(String host, int port) throws java.io.IOException {
        ServerSocket socket = this.getServerSocketChannel().socket();
        if (host != null)
            this.address = new InetSocketAddress(host, port);
        else
            this.address = new InetSocketAddress(port);
        socket.bind(this.address);
    }

    /**
     * Connect to host, port.
     *
     * @param host  host name
     * @param port  port number
     */
    void connect(String host, int port) throws java.io.IOException {
        this.state.connecting();
        this.address = new InetSocketAddress(host, port);
        this.getSocketChannel().connect(this.address);
    }

    /**
     * Write the content of <code>data</code> to the socket.
     *
     * @param data  buffer holding the bytes to be written
     */
    void send(ByteBuffer data) throws java.io.IOException {
        this.getSocketChannel().write(data);
    }

    /**
     * Read from the socket into <code>data</code>.
     *
     * @param data  buffer holding the bytes that have been read
     */
    int recv(ByteBuffer data) throws java.io.IOException {
        int count = this.getSocketChannel().read(data);
        /* a closed connection is indicated by signaling
         * a read condition, and having read() return -1. */
        if (count == -1) {
            this.handleClose();
            count = 0;
        }
        return count;
    }

    /**
     * Close the socket and remove the channnel from its selector.
     */
    void close() {
        //logger.info("close: " + this.toString());
        this.state.closing();
        if (this.channel != null) {
            try {
                if (this.channel instanceof ServerSocketChannel)
                    ((ServerSocketChannel) this.channel).socket().close();
                else if (this.channel instanceof SocketChannel)
                    ((SocketChannel) this.channel).socket().close();
            } catch (java.io.IOException e) { /* ignore */ }
        }
        this.delChannel();
    }

    void handle_read_event() {
        //logger.info("handle_read_event" + this.toString());
        this.handleRead();
    }

    void handle_write_event() {
        //logger.finest("handle_write_event" + this.toString());
        this.handleWrite();
    }

    void handle_accept_event() {
        SocketChannel channel = null;
        try {
            channel = this.getServerSocketChannel().accept();
        } catch (java.io.IOException e) {
            //logger.severe(e.toString());
            this.handleClose();
            return;
        }

        if (channel != null) {
            //logger.info("handle_accept_event: " + this.toString());
            this.handleAccept(channel);
        }
        else{}
            //logger.severe("handle_accept_event null event: " + this.toString());
    }

    void handle_connect_event() {
        if (! this.connected()) {
            try {
                this.getSocketChannel().finishConnect();
            } catch (java.io.IOException e) {
                //logger.severe(e.toString());
                this.handleClose();
                return;
            }
            this.state.connected();
            //logger.info("handle_connect_event" + this.toString());
            this.handleConnect();
        }
    }

    void handle_tick_event() {
        this.handleTick();
    }

    public String toString() {
        if (this.channel == null)
            return super.toString();
        else
            return this.channel.toString();
    }

    static void loop() {
        loop(USER_TIMEOUT);
    }

    static void loop(long timeout) {
        try {
            if (default_selector == null)
                default_selector = Selector.open();
        } catch (java.io.IOException e) {
            //logger.severe(e.toString());
            return;
        }
        loop(default_selector, timeout);
    }

    /**
     *  Enter a polling loop that terminates when all open channels have been
     *  closed.
     *
     *  The selector is a map whose items are the channels to watch.  As
     *  channels are closed they are deleted from their map.
     *
     * @param selector  Selector used for this loop
     * @param timeout   the timer events period in milliseconds
     */
    static void loop(Selector selector, long timeout) {
        //All the logic comes here
        assert selector != null :  "null selector";

        while (!selector.keys().isEmpty()) {

            setSelectionKeys(selector);
            int eventCount = 0;
            try {
                eventCount = selector.select(SELECT_TIMEOUT);
            } catch (java.io.IOException e) {
                //logger.severe(e.toString());
                return;
            }

            /* Iterate over the resulting selected SelectionKeys and send the
             * corresponding IO events. */
            Iterator it = selector.selectedKeys().iterator();
            SelectionKey key = null;
            while (it.hasNext()) {
                try {
                    key = (SelectionKey) it.next();
                } catch (java.util.ConcurrentModificationException e) {
                    //logger.severe(CONCURRENT_MODIFICATION);
                    break;
                }
                it.remove();

                Dispatcher dispatcher = (Dispatcher) key.attachment();
                if (key.isValid() && key.isReadable()) {
                    //System.out.println("isReadable");
                    dispatcher.handle_read_event();
                }
                if (key.isValid() && key.isWritable()) {
                    //System.out.println("isWritable");
                    dispatcher.handle_write_event();
                }
                if (key.isValid() && key.isAcceptable()) {
                    //System.out.println("isAcceptable");
                    dispatcher.handle_accept_event();
                }
                if (key.isValid() && key.isConnectable()) {
                    //System.out.println("isConnectable");
                    dispatcher.handle_connect_event();
                }
            }

        }
    }

    /* Set the SelectionKeys before the call to select. */
    static int setSelectionKeys(Selector selector) {
        int count = 0;
        Iterator it = selector.keys().iterator();
        while (it.hasNext()) {
            int ops = 0;
            SelectionKey key = (SelectionKey) it.next();
            if (! key.isValid()) continue;
            Dispatcher dispatcher = (Dispatcher) key.attachment();
            if (dispatcher.readyToRead()) {
                count++;
                ops |= SelectionKey.OP_READ;
            }
            if (dispatcher.readyToWrite()) {
                count++;
                ops |= SelectionKey.OP_WRITE;
            }
            if (dispatcher.readyToAccept()) {
                count++;
                ops |= SelectionKey.OP_ACCEPT;
            }
            if (dispatcher.readyToConnect()) {
                count++;
                ops |= SelectionKey.OP_CONNECT;
            }
            key.interestOps(ops);
        }
        return count;
    }

}

class ConnectionState {
    static final int NONE = 0;
    static final int ACCEPTING = 1;
    static final int CONNECTING = 2;
    static final int CONNECTED = 3;
    static final int CLOSING = 4;
    int state = NONE;

    boolean isAcceptable() { return (this.state == ACCEPTING); }
    boolean isConnectable() { return (this.state == CONNECTING); }
    boolean isReadable() { return (this.state == CONNECTED); }
    boolean isWritable() { return (this.state == CONNECTED); }
    boolean isClosed() { return (this.state == CLOSING); }

    void accepting() { this.state = ACCEPTING; }
    void connecting() { this.state = CONNECTING; }
    void connected() { this.state = CONNECTED; }
    void closing() { this.state = CLOSING; }

    public String toString() {
        String str = "state: ";
        switch(this.state) {
            case ACCEPTING:
                str += "accepting";
            case CONNECTING:
                str += "connecting";
            case CONNECTED:
                str += "connected";
            case CLOSING:
                str += "closed";
            default:
                str += "none";
        }
        return str;
    }
}
