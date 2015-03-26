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

/**
 * Created by Álvaro Felipe Melchor on 04/08/14.
 */
public class Main {

    public static void main(String[] args){
        try {
            BackendChannel client = new BackendChannel("localhost", 41414);
            for (int i = 0; i < 10; i++){
                client.push(String.valueOf(i));
            }
            client.push("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
