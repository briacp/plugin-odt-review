/**************************************************************************
 OmegaT Plugin - ODT Review

 Copyright (C) 2023 Briac Pilpré - briacp@gmail.com
 Home page: https://github.com/briacp/plugin-odt-review

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.core.data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.WindowConstants;

import net.briac.omegat.plugin.odtreview.ExportOdtFileChooser;

public class ProjectFileSelector {

    /**
     * Launch the application.
     */
    public static void main(String[] args) {

        try {
            JDialog dialog = new JDialog();
            dialog.setSize(900, 500);
            List<String> s = Arrays.asList("foo.txt", "bar.pdf", "baz.doc",
                    "azaezaezaE/ezafsdf/rzegvsqcvxw/vcxvzerzer/fdqsdf.txt", "aze.fd", "fdfs", "fsfdfsd",
                    "fsd sqdf", "mlkmlkmlk", "poipoipio");
            ExportOdtFileChooser efc = new ExportOdtFileChooser(new File("Foo.odt"), s, "f");

            int efcResult = efc.showSaveDialog(dialog);
            if (efcResult != JFileChooser.APPROVE_OPTION) {
                // user press 'Cancel' in project creation dialog
                return;
            }

            System.out.println(efc.getSelectedSourceFiles());

            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
