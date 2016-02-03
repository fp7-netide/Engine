/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.openflowjava.protocol.impl.deserialization;

import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.impl.deserialization.InstructionDeserializerInitializer;

public class NetIdeDeserializerRegistryImpl extends DeserializerRegistryImpl {

    @Override
    public void init() {
        super.init();
        NetIdeMessageDeserializerInitializer.registerMessageDeserializers(this);
        InstructionDeserializerInitializer.registerDeserializers(this);
    }
}
