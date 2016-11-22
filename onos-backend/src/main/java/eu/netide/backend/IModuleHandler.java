/*
 *  Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors: Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.backend;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public interface IModuleHandler {
    int getModuleId(String requestId);

    String getModuleNameFromID(int moduleId);

    void obtainModuleId(int xId, String moduleName, int backendId);

    void obtainBackendModuleId(int xId, String moduleName);

    void onModuleAckMessage(String moduleName, int moduleId);
}
