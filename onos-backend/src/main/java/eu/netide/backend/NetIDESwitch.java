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

import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonio on 03/10/16.
 */
public class NetIDESwitch {

    protected OFFeaturesReply features;
    protected OFDescStatsReply desc;
    private Dpid dpid;
    private OFVersion ofVersion;
    protected List<OFPortDescStatsReply> ports = new ArrayList<>();

    public NetIDESwitch(Dpid dpid, OFFeaturesReply features, OFVersion version) {
        this.dpid = dpid;
        this.features = features;
        this.ofVersion = version;
    }

    public OFVersion getVersion() {
        return this.ofVersion;
    }

    public Dpid getDpid() {
        return this.dpid;
    }

    public OFFeaturesReply getFeatures() {
        return this.features;
    }

    public void setDescription(OFDescStatsReply description) {
        this.desc = description;
    }

    public void setPort(OFPortDescStatsReply desc) {
        //For OF_13 and beyond
        this.ports.add(desc);
    }

    public List<OFPortDescStatsReply> getPorts() {
        return this.ports;
    }

    public OFFactory factory() {
        return OFFactories.getFactory(ofVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NetIDESwitch that = (NetIDESwitch) o;

        if (!features.equals(that.features)) {
            return false;
        }
        if (desc != null ? !desc.equals(that.desc) : that.desc != null) {
            return false;
        }
        if (!dpid.equals(that.dpid)) {
            return false;
        }
        if (ofVersion != that.ofVersion) {
            return false;
        }
        return ports != null ? ports.equals(that.ports) : that.ports == null;

    }

    @Override
    public int hashCode() {
        int result = features.hashCode();
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + dpid.hashCode();
        result = 31 * result + ofVersion.hashCode();
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        return result;
    }
}
