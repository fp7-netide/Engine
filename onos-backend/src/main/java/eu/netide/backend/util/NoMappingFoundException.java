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
package eu.netide.backend.util;

/**
 * Thrown to indicate that no mapping for the input value is found.
 */
public class NoMappingFoundException extends RuntimeException {
    /**
     * Creates an instance with the specified values.
     *
     * @param input input value of mapping causing this exception
     * @param output the desired class which the input value is mapped to
     */
    public NoMappingFoundException(Object input, Class<?> output) {
        super(String.format("No mapping found for %s when converting to %s", input, output.getName()));
    }
}
