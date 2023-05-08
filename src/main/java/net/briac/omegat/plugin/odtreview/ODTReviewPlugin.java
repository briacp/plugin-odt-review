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

import static org.omegat.core.Core.getMainWindow;

import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.odftoolkit.odfdom.dom.OdfDocumentNamespace;
import org.odftoolkit.odfdom.dom.element.draw.DrawGradientElement;
import org.odftoolkit.odfdom.dom.element.style.StyleGraphicPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.element.style.StyleParagraphPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.odftoolkit.odfdom.pkg.OdfName;
import org.odftoolkit.odfdom.type.Color;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.style.Border;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.PageLayoutProperties;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.style.StyleTypeDefinitions.HorizontalAlignmentType;
import org.odftoolkit.simple.style.StyleTypeDefinitions.SupportedLinearMeasure;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.CellRange;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.text.Header;
import org.odftoolkit.simple.text.Paragraph;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.data.IProject;
import org.omegat.core.data.IProject.FileInfo;
import org.omegat.core.data.PrepareTMXEntry;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.data.TMXEntry;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.main.IMainMenu;
import org.omegat.gui.main.IMainWindow;
import org.omegat.util.Log;
import org.omegat.util.gui.UIThreadsUtil;
import org.openide.awt.Mnemonics;

@SuppressWarnings({ "java:S2142", "java:S1192" })
public class ODTReviewPlugin {

    /** Id of the reviewer used when updating translations. */
    private static final String ODT_REVIEWER_ID = "odt-review";
    private static final String STYLE_WARNING_PARA = "odt-review-warning";
    private static final String STYLE_WARNING_PARA_TEXT = "odt-review-warning-text";
    private static final String STYLE_WARNING_GRADIENT = "odt-review-warning-gradient";

    private static final int TABLE_COLUMNS_COUNT = 4;

    private static final int COL_INDEX = 0;
    private static final int COL_SOURCE = 1;
    private static final int COL_TARGET = 2;
    private static final int COL_NOTE = 3;

    private static final int SIZE_COL_INDEX = 15;
    private static final int SIZE_COL_SOURCE = 90;
    private static final int SIZE_COL_TARGET = 90;
    private static final int SIZE_COL_NOTE = 65;

    private static final String HEADER_TABLE = "_header";
    private static final OdfName PROTECTED_CELL = OdfName.newName(OdfDocumentNamespace.TABLE, "protected");
    private static final String TRUE = Boolean.TRUE.toString();

    private static final Font FONT_HEADER_1 = new Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12,
            Color.BLACK);
    private static final Font FONT_HEADER_2 = new Font("Arial", StyleTypeDefinitions.FontStyle.BOLDITALIC, 14,
            Color.BLACK);
    private static final Logger LOGGER = Logger.getLogger(ODTReviewPlugin.class.getName());

    protected static final ResourceBundle res = ResourceBundle.getBundle(ODT_REVIEWER_ID,
            Locale.getDefault());
    protected static final String ODT_EXTENSION = ".odt";

    private static JMenuItem importODTReview;
    private static JMenuItem exportODTReview;

    private IProject project;
    private List<Integer> changedEntries = new ArrayList<>();
    private int updatedTranslations = 0;
    private int updatedComments = 0;

    public ODTReviewPlugin(IProject project) {
        this.project = project;
    }

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        CoreEvents.registerProjectChangeListener(
                e -> onProjectStatusChanged(Core.getProject().isProjectLoaded()));

        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                IMainMenu menu = getMainWindow().getMainMenu();
                JMenu projectMenu = menu.getProjectMenu();

                int startMenuIndex = projectMenu.getItemCount() - 6;

                exportODTReview = new JMenuItem();
                Mnemonics.setLocalizedText(exportODTReview, res.getString("odt.menu.export"));
                exportODTReview.addActionListener(e -> projectExportODTReview());
                projectMenu.add(exportODTReview, startMenuIndex++);

                importODTReview = new JMenuItem();
                Mnemonics.setLocalizedText(importODTReview, res.getString("odt.menu.import"));
                importODTReview.addActionListener(e -> projectImportODTReview());
                projectMenu.add(importODTReview, startMenuIndex++);

                projectMenu.add(new JPopupMenu.Separator(), startMenuIndex);

                onProjectStatusChanged(false);
            }

            private void projectExportODTReview() {
                // Deactivate current segment
                UIThreadsUtil.mustBeSwingThread();
                Core.getEditor().commitAndDeactivate();

                IProject currentProject = Core.getProject();
                ProjectProperties props = currentProject.getProjectProperties();

                // By default, the file is named
                // "[project-name]_[source-lang]-[target-lang]_review.odt"
                String defaultFilename = String.format("%s_%s-%s_review.%s", props.getProjectName(),
                        props.getSourceLanguage(), props.getTargetLanguage(), ODT_EXTENSION);

                File rootDir = props.getProjectRootDir();

                // TODO - Add checkboxes to select the source files to export

                ExportOdtFileChooser efc = new ExportOdtFileChooser(rootDir,
                        res.getString("odt.chooser.export"));
                efc.setSelectedFile(new File(defaultFilename));
                int efcResult = efc.showOpenDialog(Core.getMainWindow().getApplicationFrame());
                if (efcResult != JFileChooser.APPROVE_OPTION) {
                    // user press 'Cancel' in project creation dialog
                    return;
                }

                final File odtFile = efc.getSelectedFile();

                new ODTReviewPlugin(currentProject).exportODT(odtFile);
            }

            private void projectImportODTReview() {
                // Deactivate current segment
                UIThreadsUtil.mustBeSwingThread();

                IProject currentProject = Core.getProject();
                ProjectProperties props = currentProject.getProjectProperties();
                ImportOdtFileChooser ifc = new ImportOdtFileChooser(props.getProjectRootDir(),
                        res.getString("odt.chooser.import"));

                // ask for ODT file
                int ifcResult = ifc.showOpenDialog(Core.getMainWindow().getApplicationFrame());
                if (ifcResult != JFileChooser.APPROVE_OPTION) {
                    // user press 'Cancel' in project creation dialog
                    return;
                }
                final File odtFile = ifc.getSelectedFile();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        IMainWindow mainWindow = Core.getMainWindow();
                        Cursor hourglassCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
                        Cursor oldCursor = mainWindow.getCursor();
                        mainWindow.setCursor(hourglassCursor);
                        showStatusMessage(res.getString("odt.status.importing"));

                        IEditor editor = Core.getEditor();
                        editor.commitAndDeactivate();

                        ODTReviewPlugin odt = new ODTReviewPlugin(currentProject);
                        odt.importODT(odtFile);
                        if (!odt.changedEntries.isEmpty()) {
                            editor.refreshViewAfterFix(odt.changedEntries);
                        }

                        showStatusMessage(res.getString("odt.status.imported"));
                        mainWindow.setCursor(oldCursor);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {

                            get();
                            SwingUtilities.invokeLater(Core.getEditor()::requestFocus);
                        } catch (Exception ex) {
                            Log.logErrorRB(ex, "PP_ERROR_UNABLE_TO_READ_PROJECT_FILE");
                            getMainWindow().displayErrorRB(ex, "PP_ERROR_UNABLE_TO_READ_PROJECT_FILE");
                        }
                    }
                }.execute();
            }

            @Override
            public void onApplicationShutdown() {
                /* empty */
            }
        });
    }

    public void exportODT(File output) {
        exportODT(output, project.getProjectFiles());
    }

    /**
     * Export the segments in an ODT file, with the source, target and notes.
     */
    public void exportODT(File output, List<FileInfo> includedSourceFiles) {
        log(Level.INFO, res.getString("odt.file.saving"));
        try (TextDocument odt = TextDocument.newTextDocument()) {

            setupDocument(odt);

            // For each selected project files, add the entries
            for (int fileIndex = 0; fileIndex < includedSourceFiles.size(); fileIndex++) {
                if (fileIndex > 0) {
                    odt.addParagraph("");
                    odt.addPageBreak();
                }

                FileInfo currentFile = includedSourceFiles.get(fileIndex);

                List<SourceTextEntry> fileEntries = currentFile.entries;
                int numberOfEntries = fileEntries.size();
                log(Level.INFO,
                        String.format(res.getString("odt.file"), currentFile.filePath, numberOfEntries));
                Table table = createTable(odt, numberOfEntries, currentFile.filePath);
                int rowIndex = 2;
                for (SourceTextEntry ste : fileEntries) {
                    TMXEntry en = project.getTranslationInfo(ste);
                    int entryNumber = ste.entryNum();
                    String sourceText = ste.getSrcText();
                    String translation = en != null ? en.translation : null;
                    if (translation != null && translation.isEmpty()) {
                        translation = res.getString("empty.translation");
                    }
                    String note = en != null ? en.note : "";
                    addSegment(table, rowIndex++, entryNumber, sourceText, translation, note);
                }
            }

            odt.save(output);
            log(Level.INFO, String.format(res.getString("odt.file.saved"), output.getAbsolutePath()));

            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                    String.format(res.getString("odt.file.saved"), output.getAbsolutePath()),
                    res.getString("dialog.import.title"), JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            Log.logErrorRB(e, res.getString("odt.error.export"));

            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), res.getString("odt.error.export"),
                    res.getString("dialog.export.title"), JOptionPane.ERROR_MESSAGE);

        }
    }

    private void setupDocument(TextDocument odt) {
        OdfOfficeStyles styles = odt.getOrCreateDocumentStyles();

        // Setup header and footer
        Header docHeader = odt.getHeader();
        Table tableHeader = docHeader.addTable(1, 2);
        tableHeader.setTableName(HEADER_TABLE);

        tableHeader.getCellByPosition(0, 0).setStringValue(res.getString("table.header"));
        Cell cellRight = tableHeader.getCellByPosition(1, 0);
        cellRight.setStringValue(String.format(res.getString("table.header.project"),
                project.getProjectProperties().getProjectName()));
        protectCell(cellRight);
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

        // Warning text with style
        OdfStyle warningStyle = styles.newStyle(STYLE_WARNING_PARA, OdfStyleFamily.Paragraph);

        StyleParagraphPropertiesElement propPara = warningStyle.newStyleParagraphPropertiesElement();
        propPara.setFoMarginBottomAttribute("0.55cm");
        propPara.setFoPaddingAttribute("0.5cm");
        propPara.setFoBorderAttribute("1.5pt solid #f10d0c");

        DrawGradientElement styleGradient = styles.newDrawGradientElement(STYLE_WARNING_GRADIENT);
        styleGradient.setDrawNameAttribute(STYLE_WARNING_GRADIENT);
        styleGradient.setDrawDisplayNameAttribute("OmT Warning Gradient");
        styleGradient.setDrawStyleAttribute("linear");
        styleGradient.setDrawStartColorAttribute("#ffd7d7");
        styleGradient.setDrawEndColorAttribute("#f7d1d5");
        styleGradient.setDrawStartIntensityAttribute("100%");
        styleGradient.setDrawEndIntensityAttribute("100%");
        styleGradient.setDrawAngleAttribute("330deg");
        styleGradient.setDrawBorderAttribute("20%");

        StyleGraphicPropertiesElement propGraphic = warningStyle.newStyleGraphicPropertiesElement();
        propGraphic.setDrawFillAttribute("gradient");
        propGraphic.setDrawGradientStepCountAttribute(0);
        propGraphic.setDrawFillGradientNameAttribute(STYLE_WARNING_GRADIENT);

        StyleTextPropertiesElement propText = warningStyle
                .newStyleTextPropertiesElement(STYLE_WARNING_PARA_TEXT);
        propText.setFoFontStyleAttribute("italic");
        propText.setFoFontSizeAttribute("13pt");

        Paragraph paraWarning = odt.addParagraph(res.getString("doc.warning"));
        paraWarning.getOdfElement().setStyleName(STYLE_WARNING_PARA);
    }

    private void addSegment(Table table, int rowIndex, int entryNumber, String sourceText, String translation,
            String note) {
        Cell cellId = table.getCellByPosition(COL_INDEX, rowIndex);
        cellId.setStringValue(Integer.toString(entryNumber));
        protectCell(cellId);

        Cell cellSource = table.getCellByPosition(COL_SOURCE, rowIndex);
        cellSource.setStringValue(sourceText);
        protectCell(cellSource);

        table.getCellByPosition(COL_TARGET, rowIndex).setStringValue(translation);
        table.getCellByPosition(COL_NOTE, rowIndex).setStringValue(note);
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
        // Fix missing right border after the merge
        cellFilename.getStyleHandler().getTableCellPropertiesForWrite()
                .setRightBorder(new Border(Color.BLACK, 0.05, SupportedLinearMeasure.PT));

        ProjectProperties projectProperties = project.getProjectProperties();
        setHeaderCell(table, COL_INDEX, 1, res.getString("table.header.id"));
        setHeaderCell(table, COL_SOURCE, 1,
                String.format(res.getString("table.header.source"), projectProperties.getSourceLanguage()));
        setHeaderCell(table, COL_TARGET, 1,
                String.format(res.getString("table.header.target"), projectProperties.getTargetLanguage()));
        setHeaderCell(table, COL_NOTE, 1, res.getString("table.header.note"));

        table.getColumnByIndex(COL_INDEX).setWidth(SIZE_COL_INDEX);
        table.getColumnByIndex(COL_SOURCE).setWidth(SIZE_COL_SOURCE);
        table.getColumnByIndex(COL_TARGET).setWidth(SIZE_COL_TARGET);
        table.getColumnByIndex(COL_NOTE).setWidth(SIZE_COL_NOTE);

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
     * Export the segments in an ODT file, with the source, target and notes.
     */
    public void importODT(File input) {
        log(Level.INFO, String.format(res.getString("odt.file.importing"), input.getAbsolutePath()));

        // Convert the project entries to a Map for quick access later on.
        Map<Integer, SourceTextEntry> allEntries = project.getAllEntries().stream()
                .collect(Collectors.toMap(SourceTextEntry::entryNum, Function.identity()));

        try (TextDocument odt = TextDocument.loadDocument(input)) {
            boolean projectChecked = false;
            String reviewProject = "?";
            String projectName = project.getProjectProperties().getProjectName();

            for (Table table : odt.getTableList()) {
                if (!table.getTableName().equals(HEADER_TABLE)) {
                    continue;
                }

                reviewProject = table.getCellByPosition(1, 0).getStringValue()
                        .replace(res.getString("table.header.project").replace("%s", ""), "");

                if (!projectName.equals(reviewProject)) {
                    JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                            String.format(res.getString("odt.warning.file.mismatch"), input.getAbsolutePath(),
                                    reviewProject),
                            res.getString("odt.error.import"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                projectChecked = true;
            }

            if (!projectChecked) {
                log(Level.WARNING, res.getString("odt.warning.noHeader"));
                int noHeader = JOptionPane.showConfirmDialog(JOptionPane.getRootFrame(),
                        String.format(res.getString("odt.warning.noHeader"), reviewProject, projectName),
                        res.getString("odt.error.import"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (noHeader == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            for (Table table : odt.getTableList()) {
                if (table.getTableName().equals(HEADER_TABLE)) {
                    continue;
                }

                log(Level.FINEST, String.format("File %s", table.getTableName()));
                int rowCount = table.getRowCount();
                for (int rowIndex = 2; rowIndex < rowCount; rowIndex++) {
                    int entryNum = Integer
                            .parseInt(table.getCellByPosition(COL_INDEX, rowIndex).getStringValue());
                    if (!allEntries.containsKey(entryNum)) {
                        log(Level.FINE, String.format("Cannot find segment #%d in the project", entryNum));
                        continue;
                    }

                    updateSegment(table, rowIndex, allEntries.get(entryNum));

                }
            }

            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                    String.format(res.getString("dialog.import.successful"), input.getAbsolutePath(),
                            updatedTranslations, updatedComments),
                    res.getString("dialog.import.title"), JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            Log.logErrorRB(e, res.getString("odt.error.import"));

            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                    String.format(res.getString("dialog.import.error"), input.getAbsolutePath()),
                    res.getString("dialog.import.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @see org.omegat.gui.editor.filter.ReplaceFilter.replaceAll()
     */
    private void updateSegment(Table table, int rowIndex, SourceTextEntry ste) {
        String sourceText = table.getCellByPosition(COL_SOURCE, rowIndex).getStringValue();
        String targetTranslation = table.getCellByPosition(COL_TARGET, rowIndex).getStringValue();
        String note = table.getCellByPosition(COL_NOTE, rowIndex).getStringValue();

        log(Level.FINEST, String.format("Id     : %d", ste.entryNum()));
        log(Level.FINEST, String.format("Source : %s", sourceText));
        log(Level.FINEST, String.format("Target : %s", targetTranslation));
        log(Level.FINEST, String.format("Note   : %s", note));

        // Translation has changed during the review
        TMXEntry en = Core.getProject().getTranslationInfo(ste);
        boolean hasChanged = false;
        if (hasReviewChanges(en, sourceText, targetTranslation, note)) {
            PrepareTMXEntry prepare = new PrepareTMXEntry(en);

            if (!prepare.translation.equals(targetTranslation)) {
                updatedTranslations++;
                prepare.translation = targetTranslation;
                prepare.changer = ODT_REVIEWER_ID;
                hasChanged = true;
            }

            if (!note.isEmpty()) {
                updatedComments++;
                String reviewerNote = String.format(res.getString("reviewer.note"), note);
                prepare.note = prepare.note != null && !prepare.note.isEmpty()
                        ? prepare.note + "\n---\n" + reviewerNote
                        : reviewerNote;
                hasChanged = true;
            }

            if (hasChanged) {
                Core.getProject().setTranslation(ste, prepare, en.defaultTranslation, null);
                changedEntries.add(ste.entryNum());
            }
        }

    }

    /**
     * For a given segmentNum, if the source text is the same (in case we tried
     * to apply the odt to another project) and either the translation or the
     * notes are different, we update the segment.
     */
    private boolean hasReviewChanges(TMXEntry en, String source, String translation, String note) {
        return en != null && source.equals(en.source)
                && (!translation.equals(en.translation) || !note.equals(en.note));
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

    /** Hack to display a message other than a Bundle.properties string */
    private static void showStatusMessage(String msg) {
        Core.getMainWindow().showStatusMessageRB("app-version-template-pretty", msg, "");
    }

}