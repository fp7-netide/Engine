package com.telefonica.pyretic.backendchannel;

import java.io.IOException;

/**
 * Created by Álvaro Felipe Melchor on 04/08/14.
 */
public class Main {

    public static void main(String[] args){
        try {
            BackendChannel client = new BackendChannel("localhost", 41414);
            client.push("Hello\n");
            client.push("World\n");
            client.push("Yeeeeeeaaaah!!!!!!\n");
            System.out.println("-<-<-<-<- Starting backend channel");
            for (int i = 0; i < 10; i++){
                client.push(String.valueOf(i));
            }
            client.push("\n");

            //backendchannel.push("['["switch", "join"", 1, "BEGIN"]']\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
