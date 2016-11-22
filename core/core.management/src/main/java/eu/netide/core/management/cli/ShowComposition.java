/*
 * Copyright 2014-2016 NetIDE Consortium
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.ICompositionManager;
import eu.netide.core.management.ManagementHandler;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Command(scope = "netide", name = "showComposition", description = "Shows the current composition XML")
public class ShowComposition extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {

        ICompositionManager compositionManager = getService(ICompositionManager.class);


        System.out.printf("Current composition:\n");
        System.out.println(compositionManager.getCompositionSpecificationXml());

        return null;
    }

}
