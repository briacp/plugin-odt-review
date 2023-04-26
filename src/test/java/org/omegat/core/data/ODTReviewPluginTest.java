/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2008 Alex Buloichik
               2010 Volker Berlin
               Home page: http://www.omegat.org/
               Support center: https://omegat.org/support

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.core.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.omegat.core.Core;
import org.omegat.tokenizer.LuceneFrenchTokenizer;
import org.omegat.util.Preferences;

import net.briac.omegat.ODTReviewPlugin;

public class ODTReviewPluginTest {

    public static void main(String[] args) throws Exception {
        IProject project = loadProject(new File("src/test/resources/omegat.project"));
        System.out.println(project.getProjectFiles());
       //new ODTReviewPlugin(project).exportODT(new File("review.odt"));
       new ODTReviewPlugin(project).importODT(new File("review.odt"));
    }

    @Test
    @Ignore
    public void testTextFilterParsing() throws Exception {
        assertEquals("Header", "xxx");
    }
    
    private static IProject loadProject(File projectFile) throws Exception {
        ProjectProperties props = new ProjectProperties(new File("Sample Project"));
        props.setSourceLanguage("en-US");
        props.setTargetLanguage("fr-CA");
        props.setTargetTokenizer(LuceneFrenchTokenizer.class);

        Core.initializeConsole(new HashMap<String, String>());
        Preferences.init();
        Core.setProject(new RealProject(props));


        new RealProject(props).projectTMX = new ProjectTMX(props.getSourceLanguage(), props.getTargetLanguage(), false, projectFile,
                new ProjectTMX.CheckOrphanedCallback() {
                    @Override
                    public boolean existSourceInProject(String src) {
                        return true;
                    }

                    @Override
                    public boolean existEntryInProject(EntryKey key) {
                        return true;
                    }
                });
        
        return new RealProject(props);
    }
    
}
