/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.netide.core.caos;

import eu.netide.core.api.IShimManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker _shimManagerTracker;

    public void start(BundleContext context) {
        System.out.println("NetIDE CaOs module started!");

        // Test
        _shimManagerTracker = new ServiceTracker(context, IShimManager.class, null);
        _shimManagerTracker.open();
        System.out.println("Watching " + _shimManagerTracker.size() + " services at start.");

        ((IShimManager) context.getService(context.getServiceReference(IShimManager.class))).GetConnector().SendMessage("Test from CaOs");
    }

    public void stop(BundleContext context) {
        System.out.println("Watching " + _shimManagerTracker.size() + " services at stop.");
        System.out.println("Sending close to ShimConnector from CaOs bundle...");
        if (_shimManagerTracker.size() > 0)
            ((IShimManager) _shimManagerTracker.getService()).GetConnector().Close();
        _shimManagerTracker.close();
        System.out.println("NetIDE CaOs module stopped!");
    }

}