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
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker _shimManagerTracker;

    public void start(final BundleContext context) {
        System.out.println("NetIDE CaOs module started!");

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    int count = 0;
                    final ServiceReference<?> ref = context.getServiceReference("eu.netide.core.api.IShimManager");
                    if (ref == null && count < 4) {
                        System.err.println("ServiceReference to IShimManager is null...");
                        count++;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        IShimManager manager = (IShimManager) context.getService(ref);
                        manager.GetConnector().SendMessage("Test from CaOs!");
                        break;
                    }

                }
            }

        }).start();

        // Test
//        try {
//            _shimManagerTracker = new ServiceTracker(context, context.createFilter("(" + Constants.OBJECTCLASS + "=" + IShimManager.class.getName() + ")"), null);
//        } catch (InvalidSyntaxException e) {
//            e.printStackTrace();
//        }
//        _shimManagerTracker.open();
//        System.out.println("Watching " + _shimManagerTracker.size() + " services at start.");
    }

    public void stop(BundleContext context) {
//        System.out.println("Watching " + _shimManagerTracker.size() + " services at stop.");
//        if (_shimManagerTracker.size() > 0) {
//            System.out.println("Sending message to ShimConnector from CaOs bundle...");
//            ((IShimManager) _shimManagerTracker.getService()).GetConnector().SendMessage("Test from CaOs");
//        }
//        _shimManagerTracker.close();
        System.out.println("NetIDE CaOs module stopped!");
    }

}