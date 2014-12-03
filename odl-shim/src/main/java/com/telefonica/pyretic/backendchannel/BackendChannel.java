package com.telefonica.pyretic.backendchannel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;

import org.opendaylight.openflowplugin.pyretic.ODLHandlerSimpleImpl;
import org.opendaylight.openflowplugin.pyretic.ODLManagerSimpleImpl;
import org.opendaylight.openflowplugin.pyretic.multi.ODLManagerMultiImpl;
import org.opendaylight.openflowplugin.pyretic.ODLManager;

/**
 * Created by Álvaro Felipe Melchor on 01/08/14.
 */
public class BackendChannel extends Connection implements Runnable{

    //private ODLClient client;
    //private ODLHandlerSimpleImpl handler;
    //private ODLManagerSimpleImpl manager;

    // private ODLManagerMultiImpl multiManager; // 1
    private ODLManager multiManager;

    String TERM_CHAR = "\n";
    public BackendChannel(String host, int port) throws IOException {
        super(host, port);
        this.setTerminator(TERM_CHAR);
        new Thread(this).start();

    }


    //Put here all the code needed to manage the different messages of Pyretic

    //Here put the logic to manage the differents messages
    @Override
    void foundTerminator() {
        /*The end of a command or message has been seen.
        * */
        //System.out.println("Packet from pyretic");
        Object obj= JSONValue.parse(this.getBuff());
        JSONArray array=(JSONArray)obj;
        String type = array.get(0).toString();

        System.out.println("Message type: " + type);
        if(type.equals("packet")){
           // multiManager.sendToSwitch((JSONObject)array.get(1));
        }
        else if (type.equals("install")){
            System.out.println("install");
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

    /*public void setODLClient(ODLClient client){
        this.client = client;
    }
    public void setHandler(ODLHandlerSimpleImpl handler) {
        this.handler = handler;
    }

    public void setManager(ODLManagerSimpleImpl manager) {
        this.manager = manager;
    }*/

    //public void setManager(ODLManagerMultiImpl multiManager) { // 1
      //  this.multiManager = multiManager;
    //}
    public void setManager(ODLManager multiManager) {
        this.multiManager = multiManager;
    }

    //have other definition of push with different arguments types
    public void push(String msg){
        System.out.println("Gonna push in the backend channel ");
        super.send(msg);
        System.out.println("Pushed");
    }

    @Override
    public void run() {
        Dispatcher.loop();
    }
}

