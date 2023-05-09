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

package net.briac.omegat.plugin.odtreview;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Locale;

@SuppressWarnings("serial")
public final class ExportOdtFileChooser extends JFileChooser {
    public ExportOdtFileChooser(File baseDirectory, String dialogTitle) {
        super(baseDirectory);

        setApproveButtonText(ODTReviewPlugin.res.getString("odt.chooser.button.export"));
        setApproveButtonToolTipText(ODTReviewPlugin.res.getString("odt.chooser.button.export.tooltip"));
        setMultiSelectionEnabled(false);
        setFileHidingEnabled(true);
        setFileSelectionMode(FILES_ONLY);
        setDialogTitle(dialogTitle);
        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return ODTReviewPlugin.res.getString("odt.chooser.filter");
            }

            @Override
            public boolean accept(File f) {
                return f.isFile()
                        && f.getName().toLowerCase(Locale.ENGLISH).endsWith(ODTReviewPlugin.ODT_EXTENSION);
            }
        });
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        return f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(ODTReviewPlugin.ODT_EXTENSION);
    }
}