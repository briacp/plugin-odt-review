/**************************************************************************
 OmegaT Plugin - ODT Review

 Copyright (C) 2008 Briac Pilpré - briacp@gmail.com
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

package org.omegat.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.omegat.core.data.IProject.FileInfo;
import org.omegat.core.data.TMXEntry.ExternalLinked;
import org.omegat.core.statistics.StatisticsInfo;
import org.omegat.tokenizer.ITokenizer;
import org.omegat.util.Language;

import com.devskiller.jfairy.Fairy;

import net.briac.omegat.ODTReviewPlugin;

public class ODTReviewPluginTest {

    private static final Fairy FAIRY = Fairy.create(Locale.ENGLISH);
    private static final Fairy FAIRY_TARGET = Fairy.create(Locale.FRENCH);

    public static void main(String[] args) throws Exception {
        IProject project = fakeProject();
        new ODTReviewPlugin(project).exportODT(new File("review.odt"));
        // new ODTReviewPlugin(project).importODT(new File("review.odt"));
    }

    @Test
    public void testExport() throws Exception {
        File outputFile = new File("test_review.odt");
        new ODTReviewPlugin(fakeProject()).exportODT(outputFile);
        assertTrue(outputFile.exists());
    }

    @Test
    public void testImport() throws Exception {
        File inputFile = new File("test_review.odt");
        assertTrue(inputFile.exists());
        new ODTReviewPlugin(fakeProject()).importODT(inputFile);
    }

    private static IProject fakeProject() throws Exception {
        List<FileInfo> projectFiles = new ArrayList<>();
        Map<SourceTextEntry, TMXEntry> allEntries = new HashMap<>();

        ProjectProperties projectProperties = new ProjectProperties(
                new File(FAIRY.textProducer().latinWord(2)));
        projectProperties.setSourceLanguage("en-US");
        projectProperties.setTargetLanguage("fr-FR");

        int maxFiles = FAIRY.baseProducer().randomBetween(5, 7);
        int entryId = 0;
        for (int indexFile = 0; indexFile < maxFiles; indexFile++) {

            FileInfo fileInfo = new FileInfo();
            fileInfo.filePath = FAIRY.textProducer().latinWord(2).replace(" ", "") + ".docx";

            int maxEntries = FAIRY.baseProducer().randomBetween(10, 30);

            for (int indexEntry = 0; indexEntry < maxEntries; indexEntry++) {
                String sourceText = FAIRY.textProducer().paragraph(1);
                String sourceTranslation = FAIRY_TARGET.textProducer().paragraph(1);

                EntryKey key = new EntryKey(fileInfo.filePath, sourceText, null, null, null, null);

                String comment = FAIRY.baseProducer().randomBetween(1, 100) > 75
                        ? FAIRY.textProducer().latinSentence(1)
                        : null;
                String[] props = comment == null ? null : new String[] { SegmentProperties.COMMENT, comment };
                SourceTextEntry entry = new SourceTextEntry(key, entryId++, props, sourceTranslation,
                        Collections.emptyList(), false);

                PrepareTMXEntry pte = new PrepareTMXEntry();
                pte.source = sourceText;
                pte.translation = sourceTranslation;
                allEntries.put(entry, new TMXEntry(pte, false, null));

                fileInfo.entries.add(entry);
            }
            projectFiles.add(fileInfo);
        }

        return new IProject() {

            @Override
            public TMXEntry getTranslationInfo(SourceTextEntry ste) {
                return allEntries.get(ste);
            }

            @Override
            public ProjectProperties getProjectProperties() {
                return projectProperties;
            }

            @Override
            public List<FileInfo> getProjectFiles() {
                return projectFiles;
            }

            @Override
            public void teamSyncPrepare() throws Exception {
                /* empty */
            }

            @Override
            public void teamSync() {
                /* empty */
            }

            @Override
            public void setTranslation(SourceTextEntry entry, PrepareTMXEntry trans,
                    boolean defaultTranslation, ExternalLinked externalLinked,
                    AllTranslations previousTranslations) throws OptimisticLockingFail {
                /* empty */
            }

            @Override
            public void setTranslation(SourceTextEntry entry, PrepareTMXEntry trans,
                    boolean defaultTranslation, ExternalLinked externalLinked) {
                /* empty */
            }

            @Override
            public void setSourceFilesOrder(List<String> filesList) {
                /* empty */
            }

            @Override
            public void setNote(SourceTextEntry entry, TMXEntry oldTrans, String note) {
                /* empty */
            }

            @Override
            public void saveProjectProperties() throws Exception {
                /* empty */
            }

            @Override
            public void saveProject(boolean doTeamSync) {
                /* empty */
            }

            @Override
            public void iterateByMultipleTranslations(MultipleTranslationsIterator it) {
                /* empty */
            }

            @Override
            public void iterateByDefaultTranslations(DefaultTranslationsIterator it) {
                /* empty */
            }

            @Override
            public boolean isTeamSyncPrepared() {
                return false;
            }

            @Override
            public boolean isRemoteProject() {
                return false;
            }

            @Override
            public boolean isProjectModified() {
                return false;
            }

            @Override
            public boolean isProjectLoaded() {
                return false;
            }

            @Override
            public boolean isOrphaned(EntryKey entry) {
                return false;
            }

            @Override
            public boolean isOrphaned(String source) {
                return false;
            }

            @Override
            public Map<String, ExternalTMX> getTransMemories() {
                return Collections.emptyMap();
            }

            @Override
            public ITokenizer getTargetTokenizer() {
                return null;
            }

            @Override
            public String getTargetPathForSourceFile(String sourceFile) {
                return null;
            }

            @Override
            public StatisticsInfo getStatistics() {
                return null;
            }

            @Override
            public ITokenizer getSourceTokenizer() {
                return null;
            }

            @Override
            public List<String> getSourceFilesOrder() {
                return null;
            }

            @Override
            public Map<Language, ProjectTMX> getOtherTargetLanguageTMs() {
                return null;
            }

            @Override
            public AllTranslations getAllTranslations(SourceTextEntry ste) {
                return null;
            }

            @Override
            public List<SourceTextEntry> getAllEntries() {
                return null;
            }

            @Override
            public void compileProjectAndCommit(String sourcePattern, boolean doPostProcessing,
                    boolean commitTargetFiles) throws Exception {
                /* empty */
            }

            @Override
            public void compileProject(String sourcePattern) throws Exception {
                /* empty */
            }

            @Override
            public void commitSourceFiles() throws Exception {
                /* empty */
            }

            @Override
            public void closeProject() {
                /* empty */
            }
        };
    }

    @Test
    @Ignore
    public void testTextFilterParsing() throws Exception {
        assertEquals("Header", "xxx");
    }

}
