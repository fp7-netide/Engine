package eu.netide.lib.netip;

/**
 * Represents a ModuleAcknowledge message.
 * <p>
 * Created by timvi on 24.09.2015.
 */
public class ModuleAcknowledgeMessage extends Message {
    private String moduleName;

    /**
     * Creates a new instance of the ModuleAcknowledgeMessage class.
     */
    public ModuleAcknowledgeMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.MODULE_ACKNOWLEDGE);
    }

    /**
     * Gets module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Sets module name.
     *
     * @param moduleName the module name
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public byte[] getPayload() {
        return this.moduleName.getBytes();
    }
}
