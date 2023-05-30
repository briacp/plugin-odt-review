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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

public class SourceFileSelectionModel extends DefaultTableModel {
    private static final long serialVersionUID = 5248318346086309743L;

    private static final String[] COLUMN_NAMES = new String[] { "Selected", "Source file" };
    private static final Class<?>[] COLUMN_TYPES = new Class[] { Boolean.class, Object.class };
    private static final boolean[] COLUMN_EDITABLES = new boolean[] { true, false };

    private Map<String, Boolean> fileSelection = new HashMap<>();
    private List<String> files = new ArrayList<>();

    public SourceFileSelectionModel(List<String> files) {
        super(COLUMN_NAMES, 0);
        this.files = files;
        files.forEach(file -> {
            fileSelection.put(file, Boolean.TRUE);
            addRow(new Object[] { Boolean.TRUE, file });
        });
        fireTableDataChanged();
    }

    @Override
    public Object getValueAt(int row, int col) {
        return col == 1 ? files.get(row) : fileSelection.get(files.get(row));
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        String file = files.get(row);
        fileSelection.put(file, !fileSelection.get(file));
        fireTableCellUpdated(row, col);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_TYPES[columnIndex];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return COLUMN_EDITABLES[column];
    }

    public List<String> getSelectedSourceFiles() {
        return fileSelection.entrySet().stream().filter(Entry::getValue).map(Entry::getKey)
                .collect(Collectors.toList());
    }

}