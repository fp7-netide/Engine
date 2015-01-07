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
package org.opendaylight.openflowplugin.pyretic;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.json.simple.JSONObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Álvaro Felipe Melchor on 08/09/14.
 */
public interface ODLHandler {
    /**
     * @param tablePath
     */
    void onSwitchAppeared(InstanceIdentifier<Table> tablePath);

    /**
     * @param packetProcessingService the packetProcessingService to set
     */
    void setPacketProcessingService(PacketProcessingService packetProcessingService);

    /**
     * @param dataStoreAccessor the dataStoreAccessor to set
     */
    void setDataStoreAccessor(FlowCommitWrapper dataStoreAccessor);

    /**
     * @param registrationPublisher the registrationPublisher to set
     */
    void setRegistrationPublisher(DataChangeListenerRegistrationHolder registrationPublisher);

    public void setBackendChannel(BackendChannel channel);

    void sendToSwitch(JSONObject json, String type);

}
