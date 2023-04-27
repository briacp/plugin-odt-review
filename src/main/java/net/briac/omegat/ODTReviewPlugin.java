/**************************************************************************
 OmegaT Plugin - ODT Review

 Copyright (C) 2023 Briac Pilpré - briacp@gmail.com
 Home page: https://github.com/briacp/plugin-omt-package

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

package net.briac.omegat;

import static org.omegat.core.Core.getMainWindow;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.odftoolkit.odfdom.dom.OdfDocumentNamespace;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.odftoolkit.odfdom.pkg.OdfName;
import org.odftoolkit.odfdom.type.Color;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.PageLayoutProperties;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.style.StyleTypeDefinitions.HorizontalAlignmentType;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.CellRange;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.text.Header;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.data.IProject;
import org.omegat.core.data.IProject.FileInfo;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.gui.main.IMainMenu;
import org.omegat.util.Log;
import org.openide.awt.Mnemonics;

public class ODTReviewPlugin {

    private static final int TABLE_COLUMNS_COUNT = 4;

    private static final int COL_INDEX = 0;
    private static final int COL_SOURCE = 1;
    private static final int COL_TARGET = 2;
    private static final int COL_COMMENT = 3;

    private static final int SIZE_COL_INDEX = 15;
    private static final int SIZE_COL_SOURCE = 90;
    private static final int SIZE_COL_TARGET = 90;
    private static final int SIZE_COL_COMMENT = 65;

    private static final String HEADER_TABLE = "_header";
    private static final OdfName PROTECTED_CELL = OdfName.newName(OdfDocumentNamespace.TABLE, "protected");
    private static final String TRUE = Boolean.TRUE.toString();

    private static final Font FONT_HEADER_1 = new Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12,
            Color.BLACK);
    private static final Font FONT_HEADER_2 = new Font("Arial", StyleTypeDefinitions.FontStyle.BOLDITALIC, 14,
            Color.BLACK);
    private static final Logger LOGGER = Logger.getLogger(ODTReviewPlugin.class.getName());

    protected static final ResourceBundle res = ResourceBundle.getBundle("odt-review", Locale.getDefault());

    private static JMenuItem importODTReview;
    private static JMenuItem exportODTReview;

    private IProject project;

    public ODTReviewPlugin(IProject project) {
        this.project = project;
    }

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        ODTReviewPlugin.log(Level.INFO, "Loading ODTReviewPlugin");

        CoreEvents.registerProjectChangeListener(
                e -> onProjectStatusChanged(Core.getProject().isProjectLoaded()));

        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                IMainMenu menu = getMainWindow().getMainMenu();
                JMenu projectMenu = menu.getProjectMenu();

                int startMenuIndex = projectMenu.getItemCount() - 6;

                importODTReview = new JMenuItem();
                Mnemonics.setLocalizedText(importODTReview, res.getString("odt.menu.import"));
                importODTReview.addActionListener(e -> projectImportODTReview());

                projectMenu.add(new JPopupMenu.Separator(), startMenuIndex++);
                projectMenu.add(importODTReview, startMenuIndex++);

                exportODTReview = new JMenuItem();
                Mnemonics.setLocalizedText(exportODTReview, res.getString("odt.menu.export"));
                exportODTReview.addActionListener(e -> projectExportODTReview());
                projectMenu.add(exportODTReview, startMenuIndex);

                onProjectStatusChanged(false);
            }

            private void projectExportODTReview() {
                log(Level.INFO, "TODO - Display GUI for selecting files to export and output filename");
                log(Level.INFO, "odtr: " + new ODTReviewPlugin(Core.getProject()));
                new ODTReviewPlugin(Core.getProject()).exportODT(new File("review.odt"));
            }

            private void projectImportODTReview() {
                log(Level.INFO, "TODO - GUI for importing reviewed ODT");
                new ODTReviewPlugin(Core.getProject()).importODT(new File("review.odt"));
            }

            @Override
            public void onApplicationShutdown() {
                /* empty */
            }
        });
    }

    /**
     * Export the segments in an ODT file, with the source, target and comments.
     */
    public void exportODT(File output) {
        log(Level.INFO, res.getString("odt.file.saving"));
        try (TextDocument odt = TextDocument.newTextDocument()) {

            // Setup header and footer
            Header docHeader = odt.getHeader();
            Table tableHeader = docHeader.addTable(2, 2);
            tableHeader.setTableName(HEADER_TABLE);
            tableHeader.getCellByPosition(0, 0).setStringValue(res.getString("table.header"));
            Cell cellRight = tableHeader.getCellByPosition(1, 0);
            cellRight.setStringValue(String.format(res.getString("table.header.project"),
                    project.getProjectProperties().getProjectName()));
            cellRight.setHorizontalAlignment(HorizontalAlignmentType.RIGHT);

            // Switch page orientation
            for (Iterator<StyleMasterPageElement> it = odt.getOfficeMasterStyles().getMasterPages(); it
                    .hasNext();) {
                StyleMasterPageElement page = it.next();
                String pageLayoutName = page.getStylePageLayoutNameAttribute();
                OdfStylePageLayout pageLayoutStyle = page.getAutomaticStyles().getPageLayout(pageLayoutName);
                PageLayoutProperties pageLayoutProps = PageLayoutProperties
                        .getOrCreatePageLayoutProperties(pageLayoutStyle);
                double tmp = pageLayoutProps.getPageWidth();
                pageLayoutProps.setPageWidth(pageLayoutProps.getPageHeight());
                pageLayoutProps.setPageHeight(tmp);
            }

            odt.addParagraph(res.getString("doc.warning"));

            // For each selected project files, add the entries
            List<FileInfo> files = project.getProjectFiles();
            for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
                if (fileIndex > 0) {
                    odt.addParagraph("");
                    odt.addPageBreak();
                }

                FileInfo currentFile = files.get(fileIndex);

                List<SourceTextEntry> fileEntries = currentFile.entries;
                int numberOfEntries = fileEntries.size();
                log(Level.INFO,
                        String.format(res.getString("odt.file"), currentFile.filePath, numberOfEntries));
                Table table = createTable(odt, numberOfEntries, currentFile.filePath);
                int rowIndex = 2;
                for (SourceTextEntry ste : fileEntries) {
                    int entryNumber = ste.entryNum();
                    String sourceText = ste.getSrcText();
                    String translation = project.getTranslationInfo(ste) != null
                            ? project.getTranslationInfo(ste).translation
                            : null;
                    if (translation != null && translation.isEmpty()) {
                        translation = res.getString("empty.translation");
                    }
                    String comment = Optional.ofNullable(ste.getComment()).orElse("");
                    addSegment(table, rowIndex++, entryNumber, sourceText, translation, comment);
                }
            }

            odt.save(output);
            log(Level.INFO, String.format(res.getString("odt.file.saved"), output.getAbsolutePath()));
        } catch (Exception e) {
            Log.logErrorRB(e, "Error exporting ODT file");
        }
    }

    private void addSegment(Table table, int rowIndex, int entryNumber, String sourceText, String translation,
            String comment) {
        Cell cellId = table.getCellByPosition(COL_INDEX, rowIndex);
        cellId.setStringValue(Integer.toString(entryNumber));
        protectCell(cellId);

        Cell cellSource = table.getCellByPosition(COL_SOURCE, rowIndex);
        cellSource.setStringValue(sourceText);
        protectCell(cellSource);

        table.getCellByPosition(COL_TARGET, rowIndex).setStringValue(translation);
        table.getCellByPosition(COL_COMMENT, rowIndex).setStringValue(comment);
    }

    private void protectCell(Cell cellSource) {
        cellSource.getListContainerElement().setOdfAttributeValue(PROTECTED_CELL, TRUE);
    }

    private Table createTable(TextDocument odt, int maxSegments, String sourceFile) {
        Table table = odt.addTable(maxSegments + 2, TABLE_COLUMNS_COUNT);

        table.setTableName(sourceFile);
        Cell cellFilename = setHeaderCell(table, 0, 0,
                String.format(res.getString("table.header.file"), sourceFile), true);
        protectCell(cellFilename);
        CellRange cellRange = table.getCellRangeByPosition(0, 0, TABLE_COLUMNS_COUNT - 1, 0);
        cellRange.merge();

        ProjectProperties projectProperties = project.getProjectProperties();
        setHeaderCell(table, COL_INDEX, 1, res.getString("table.header.id"));
        setHeaderCell(table, COL_SOURCE, 1,
                String.format(res.getString("table.header.source"), projectProperties.getSourceLanguage()));
        setHeaderCell(table, COL_TARGET, 1,
                String.format(res.getString("table.header.target"), projectProperties.getTargetLanguage()));
        setHeaderCell(table, COL_COMMENT, 1, res.getString("table.header.comment"));

        table.getColumnByIndex(COL_INDEX).setWidth(SIZE_COL_INDEX);
        table.getColumnByIndex(COL_SOURCE).setWidth(SIZE_COL_SOURCE);
        table.getColumnByIndex(COL_TARGET).setWidth(SIZE_COL_TARGET);
        table.getColumnByIndex(COL_COMMENT).setWidth(SIZE_COL_COMMENT);

        return table;
    }

    private Cell setHeaderCell(Table table, int col, int row, String text) {
        return setHeaderCell(table, col, row, text, false);
    }

    private Cell setHeaderCell(Table table, int col, int row, String text, boolean isItalic) {
        Cell cell = table.getCellByPosition(col, row);
        cell.setHorizontalAlignment(HorizontalAlignmentType.CENTER);
        cell.setFont(isItalic ? FONT_HEADER_2 : FONT_HEADER_1);
        cell.setDisplayText(text);
        return cell;
    }

    /**
     * Export the segments in an ODT file, with the source, target and comments.
     */
    public void importODT(File input) {
        log(Level.INFO, String.format(res.getString("odt.file.importing"), input.getAbsolutePath()));
        try (TextDocument odt = TextDocument.loadDocument(input)) {
            for (Table table : odt.getTableList()) {
                if (table.getTableName().equals(HEADER_TABLE)) {
                    continue;
                }

                System.out.println("File => " + table.getTableName());
                int rowCount = table.getRowCount();
                for (int rowIndex = 2; rowIndex < rowCount; rowIndex++) {
                    System.out.println(
                            "Id      :" + table.getCellByPosition(COL_INDEX, rowIndex).getStringValue());
                    System.out.println(
                            "Source  :" + table.getCellByPosition(COL_SOURCE, rowIndex).getStringValue());
                    System.out.println(
                            "Target  :" + table.getCellByPosition(COL_TARGET, rowIndex).getStringValue());
                    System.out.println(
                            "Comment :" + table.getCellByPosition(COL_COMMENT, rowIndex).getStringValue());
                }
            }
        } catch (Exception e) {
            Log.logErrorRB(e, "Error importing ODT file");
        }
    }

    /** Plugin unloader. */
    public static void unloadPlugins() {
        /* empty */
    }

    /** The import/export review are only available when a project is loaded. */
    private static void onProjectStatusChanged(boolean isProjectLoaded) {
        if (exportODTReview != null) {
            exportODTReview.setEnabled(isProjectLoaded);
        }
        if (importODTReview != null) {
            importODTReview.setEnabled(isProjectLoaded);
        }
    }

    private static void log(Level l, String message, Object... parameters) {
        LogRecord rec = new LogRecord(l, message);
        rec.setParameters(parameters);
        rec.setLoggerName(LOGGER.getName());
        LOGGER.log(rec);
    }

}
