package org.opendaylight.openflowplugin.pyretic;

import com.telefonica.pyretic.backendchannel.BackendChannel;
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
}
