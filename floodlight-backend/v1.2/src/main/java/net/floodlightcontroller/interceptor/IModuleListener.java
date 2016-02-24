/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public interface IModuleListener {
    void onModuleAckMessage(String moduleName, int moduleId);
}
