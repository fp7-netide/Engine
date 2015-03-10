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

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.IOException;

import org.opendaylight.openflowplugin.pyretic.ODLHandler;
import org.opendaylight.openflowplugin.pyretic.ODLManager;


public class BackendChannel extends Connection implements Runnable{

    private ODLHandler multiHandler;
    private ODLManager multiManager;

    String TERM_CHAR = "\n";
    public BackendChannel(String host, int port) throws IOException {
        super(host, port);
        this.setTerminator(TERM_CHAR);
        new Thread(this).start();

    }
    //Put here all the code needed to manage the different messages of Pyretic

    //Here put the logic to manage the different messages
    @Override
    void foundTerminator() {
        /*The end of a command or message has been seen.
        * */
        Object obj= JSONValue.parse(this.getBuff());
        JSONArray array=(JSONArray)obj;
        String type = array.get(0).toString();

        if(type.equals("packet")){
            multiHandler.sendToSwitch(array);
        }
        else if (type.equals("inject_discovery_packet")) {
            System.out.println("sending inject discovery packet starts");
            multiHandler.sendToSwitch(array);
            System.out.println("inject discovery packet sent");
        }
        else if (type.equals("install")){
            System.out.println("install starts");
            multiHandler.sendToSwitch(array);
            System.out.println("install ends");
        }
        else if(type.equals("delete")){
            System.out.println("delete");
        }
        else if(type.equals("clear")){
            System.out.println("clear");
        }
        else if(type.equals("barrier")){
            System.out.println("clear");
        }
        else if(type.equals("flow_stats_request")){
            System.out.println("clear");
        }
        else{
            System.out.println("ERROR: Unknown msg from frontend " + array.get(1));
        }

    }
    public void setMultiManager(ODLManager multiManager) {
        this.multiManager = multiManager;
    }
    public void setHandler(ODLHandler multiHandler) {
        this.multiHandler = multiHandler;
    }

    public synchronized void push(String msg){
        if (msg != "") {
            super.send(msg);
            sleep(300);
        }
    }

    @Override
    public void run() {
        Dispatcher.loop();
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

