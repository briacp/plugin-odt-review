/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2000-2006 Keith Godfrey and Maxym Mykhalchuk
               2010 Volker Berlin
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

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

package net.briac.omegat;

import static org.omegat.core.Core.getMainWindow;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
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
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.gui.main.IMainMenu;
import org.omegat.util.Log;
import org.openide.awt.Mnemonics;

import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.text.TextProducer;

public class ODTReviewPlugin {

    private static final int COL_INDEX = 0;
    private static final int COL_SOURCE = 1;
    private static final int COL_TARGET = 2;
    private static final int COL_COMMENT = 3;

    private static final String HEADER_TABLE = "_header";

    private static final int TABLE_COLUMNS_COUNT = 4;

    private static final Font FONT_HEADER_1 = new Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12, Color.BLACK);
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

        CoreEvents.registerProjectChangeListener(e -> onProjectStatusChanged(Core.getProject().isProjectLoaded()));

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
                log(Level.INFO, "TODO - Display GUI for selecting export files");
                new ODTReviewPlugin(Core.getProject()).exportODT(new File("review.odt"));
            }

            private void projectImportODTReview() {
                log(Level.INFO, "TODO - Import ODT");
                new ODTReviewPlugin(Core.getProject()).importODT(new File("review.odt"));
            }

            @Override
            public void onApplicationShutdown() {
                /* empty */
            }
        });
    }

    /** Export the segments in an ODT file, with the source, target and comments. */
    public void exportODT(File output) {

        try (TextDocument odt = TextDocument.newTextDocument()) {

            // Setup header and footer
            Header docHeader = odt.getHeader();
            Table tableHeader = docHeader.addTable(1, 2);
            tableHeader.setTableName(HEADER_TABLE);
            tableHeader.getCellByPosition(0, 0).setStringValue(res.getString("table.header"));
            Cell cellRight = tableHeader.getCellByPosition(1, 0);
            cellRight.setStringValue(project.getProjectProperties().getProjectName());
            cellRight.setHorizontalAlignment(HorizontalAlignmentType.RIGHT);

            // Switch page orientation
            for (Iterator<StyleMasterPageElement> it = odt.getOfficeMasterStyles().getMasterPages(); it.hasNext();) {
                StyleMasterPageElement page = it.next();
                String pageLayoutName = page.getStylePageLayoutNameAttribute();
                OdfStylePageLayout pageLayoutStyle = page.getAutomaticStyles().getPageLayout(pageLayoutName);
                PageLayoutProperties pageLayoutProps = PageLayoutProperties
                        .getOrCreatePageLayoutProperties(pageLayoutStyle);
                double tmp = pageLayoutProps.getPageWidth();
                pageLayoutProps.setPageWidth(pageLayoutProps.getPageHeight());
                pageLayoutProps.setPageHeight(tmp);
            }

            // odt.addParagraph("This is my very first ODF test");
            Fairy sourceFairy = Fairy.create(Locale.ENGLISH);
            Fairy targetFairy = Fairy.create(Locale.FRENCH);
            int maxFiles = sourceFairy.baseProducer().randomBetween(3, 6);

            for (int fileIndex = 0; fileIndex < maxFiles; fileIndex++) {
                if (fileIndex > 0) {
                    odt.addParagraph("");
                    odt.addPageBreak();
                }
                int maxSegments = sourceFairy.baseProducer().randomBetween(10, 20);
                TextProducer sourceText = sourceFairy.textProducer();
                Table table = createTable(odt, maxSegments, new File(sourceText.latinWord(1) + ".odt"));
                for (int segmentIndex = 2; segmentIndex < maxSegments; segmentIndex++) {
                    String source = sourceText.paragraph(1);
                    String target = targetFairy.textProducer().paragraph(1);
                    String comment = sourceText.latinSentence(5);
                    addSegment(table, segmentIndex, source, target, comment);
                }
            }

            // odt.addParagraph("That's all folks!");

            odt.save(output);
        } catch (Exception e) {
            Log.logErrorRB(e, "Error exporting ODT file");
        }
    }

    /** Export the segments in an ODT file, with the source, target and comments. */
    public void importODT(File input) {
        try (TextDocument odt = TextDocument.loadDocument(input)) {
            for (Table table : odt.getTableList()) {
                if (table.getTableName().equals(HEADER_TABLE)) {
                    continue;
                }
                
                System.out.println("File => " + table.getTableName());
                int rowCount = table.getRowCount();
                for (int rowIndex = 2; rowIndex < rowCount; rowIndex++) {
                    System.out.println("Id      :" + table.getCellByPosition(COL_INDEX, rowIndex).getDisplayText());
                    System.out.println("Source  :" + table.getCellByPosition(COL_SOURCE, rowIndex).getDisplayText());
                    System.out.println("Target  :" + table.getCellByPosition(COL_TARGET, rowIndex).getDisplayText());
                    System.out.println("Comment :" + table.getCellByPosition(COL_COMMENT, rowIndex).getDisplayText());
                }
            }
        } catch (Exception e) {
            Log.logErrorRB(e, "Error importing ODT file");
        }
    }
    
    private void addSegment(Table table, int rowIndex, String source, String target, String comment) {
        table.getCellByPosition(COL_INDEX, rowIndex).setDisplayText(Integer.toString(rowIndex));
        table.getCellByPosition(COL_SOURCE, rowIndex).setDisplayText(source);
        table.getCellByPosition(COL_TARGET, rowIndex).setDisplayText(target);
        table.getCellByPosition(COL_COMMENT, rowIndex).setDisplayText(comment);
    }

    private Table createTable(TextDocument odt, int maxSegments, File sourceFile) {
        Table table = odt.addTable(maxSegments, TABLE_COLUMNS_COUNT);
        Cell cell = table.getCellByPosition(0, 0);
        cell.setCellBackgroundColor(Color.GRAY);
        CellRange cellRange = table.getCellRangeByPosition(0, 0, TABLE_COLUMNS_COUNT - 1, 0);
        cellRange.merge();

        table.setTableName(sourceFile.toString());
        setHeaderCell(table, 0, 0, String.format(res.getString("table.header.file"), sourceFile.getName()), true);

        ProjectProperties projectProperties = Core.getProject().getProjectProperties();
        setHeaderCell(table, COL_INDEX, 1, res.getString("table.header.id"));
        table.getColumnByIndex(COL_INDEX).setWidth(20);
        setHeaderCell(table, COL_SOURCE, 1,
                String.format(res.getString("table.header.source"), projectProperties.getSourceLanguage()));
        setHeaderCell(table, COL_TARGET, 1,
                String.format(res.getString("table.header.target"), projectProperties.getTargetLanguage()));
        setHeaderCell(table, COL_COMMENT, 1, res.getString("table.header.comment"));

        return table;
    }

    private void setHeaderCell(Table table, int col, int row, String text) {
        setHeaderCell(table, col, row, text, false);
    }

    private void setHeaderCell(Table table, int col, int row, String text, boolean isItalic) {
        Cell cell = table.getCellByPosition(col, row);
        cell.setHorizontalAlignment(HorizontalAlignmentType.CENTER);
        cell.setFont(isItalic ? FONT_HEADER_2 : FONT_HEADER_1);
        cell.setDisplayText(text);
    }

    /**
     * Plugin unloader.
     */
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

    public static void log(String message, Object... parameters) {
        Log.logDebug(LOGGER, message, parameters);
    }

    public static void log(Level l, String message, Object... parameters) {
        LogRecord rec = new LogRecord(l, message);
        rec.setParameters(parameters);
        rec.setLoggerName(LOGGER.getName());
        LOGGER.log(rec);
    }

}
