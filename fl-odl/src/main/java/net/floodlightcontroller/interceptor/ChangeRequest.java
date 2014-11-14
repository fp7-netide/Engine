package net.floodlightcontroller.interceptor;

import java.nio.channels.SocketChannel;

public class ChangeRequest {
	//SelectionKey.OP_ACCEPT  = 16
	//SelectionKey.OP_CONNECT =  8
	//SelectionKey.OP_WRITE   =  4
	//SelectionKey.OP_READ    =  1
	
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	
	public SocketChannel socket;
	public int type;
	public int ops;
	
	public ChangeRequest(SocketChannel socket, int type, int ops) {
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}
