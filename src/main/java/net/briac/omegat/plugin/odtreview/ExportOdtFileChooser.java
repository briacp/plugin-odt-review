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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public final class ExportOdtFileChooser extends JFileChooser {
    private static final long serialVersionUID = -5208753817437732831L;
    private SourceFileSelectionModel tableModel;

    public ExportOdtFileChooser(File baseDirectory, List<String> sourceFiles, String dialogTitle) {
        super(baseDirectory);

        setAccessory(createSourceFilePicker(sourceFiles));

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

    private JPanel createSourceFilePicker(List<String> sourceFiles) {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(new JLabel(ODTReviewPlugin.res.getString("odt.chooser.source.label")), BorderLayout.NORTH);

        tableModel = new SourceFileSelectionModel(sourceFiles);
        JTable tableFileSelection = new JTable(tableModel);
        tableFileSelection.setPreferredScrollableViewportSize(new Dimension(500, 70));
        tableFileSelection.setFillsViewportHeight(true);

        TableColumnModel columnModel = tableFileSelection.getColumnModel();
        TableColumn colCheckbox = columnModel.getColumn(0);
        colCheckbox.setResizable(false);
        colCheckbox.setPreferredWidth(60);
        colCheckbox.setMinWidth(60);
        colCheckbox.setMaxWidth(60);
        tableFileSelection.setRowSelectionAllowed(false);
        JScrollPane tablePane = new JScrollPane(tableFileSelection);
        panel.add(tablePane);
        return panel;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        return f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(ODTReviewPlugin.ODT_EXTENSION);
    }

    public List<String> getSelectedSourceFiles() {
        return tableModel.getSelectedSourceFiles();
    }
}