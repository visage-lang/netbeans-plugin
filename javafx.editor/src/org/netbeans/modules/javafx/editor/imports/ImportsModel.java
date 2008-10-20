/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javafx.editor.imports;

import com.sun.javafx.api.tree.ImportTree;
import com.sun.javafx.api.tree.Tree;
import org.netbeans.api.javafx.source.CompilationInfo;
import org.netbeans.modules.javafx.editor.JFXImportManager;

import javax.lang.model.element.Element;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * @author Rastislav Komara (<a href="mailto:moonko@netbeans.orgm">RKo</a>)
 * @todo documentation
 */
public final class ImportsModel {
    public static final Logger logger = Logger.getLogger(JFXImportManager.class.getName());
    private final Collection<ModelEntry> entries = new TreeSet<ModelEntry>();
    private final CompilationInfo ci;


    ImportsModel(List<? extends ImportTree> imports, CompilationInfo ci) {
        if (imports == null) throw new IllegalArgumentException("List of imports cannot be null.");
        for (ImportTree anImport : imports) {
            entries.add(new ModelEntry(anImport));
        }        
        this.ci = ci;
    }

    public void addImport(String qn) {
        entries.add(new ModelEntry(qn));
    }

    private boolean isLocal(Element e) {
        return false;
    }

    boolean isImported(Element e) {
        if (isLocal(e)) return true;
        for (ModelEntry entry : entries) {
            if (entry != null && entry.includes(e.asType().toString())) {
                entry.setUsage();
                return true;
            }
        }
        return false;
    }


/*
    void publish(final JTextComponent editor) {
        publish(editor.getDocument(), editor);
    }
    
    void publish(final Document doc, final JTextComponent editor) {
        Runnable runnable = new Runnable() {
            public void run() {
                TokenSequence<JFXTokenId> ts = getTokenSequence(doc, 0);
                final int startPos = quessImportsStart(ts);
                final int endPos = quessImportsEnd(ts, startPos);
                Reformat reformat = null;
                try {
                    Position end = doc.createPosition(endPos);
                    int length = endPos - startPos;
                    if (logger.isLoggable(Level.INFO)) {
                        logger.info(doc.getText(startPos, length) + "\n");
                        logger.info("Publishing following entries:");
                    }
                    doc.remove(startPos, length);
                    int offset = startPos;
                    boolean first = true;
                    for (ModelEntry entry : entries) {
                        if (entry.isUsed()) {
                            logger.info("\t" + entry.toImportStatement());
                            String text = (first ? "" : "\n") + entry.toImportStatement();
                            first = false;
                            doc.insertString(offset, text, null);
                            offset += text.length();
                        }
                    }
                    reformat = Reformat.get(doc);
                    reformat.lock();
                    reformat.reformat(0, end.getOffset());
                } catch (BadLocationException e) {
                    logger.severe(e.getLocalizedMessage());
                } finally {
                    if (reformat != null) {
                        reformat.unlock();
                    }
                    editor.requestFocusInWindow();
                }
            }
        };
        if (doc instanceof GuardedDocument) {
            GuardedDocument gd = (GuardedDocument) doc;
            gd.runAtomic(runnable);
        } else {
            logger.warning("Running in non atomic fashion.");
            runnable.run();
        }

    }

    private int quessImportsEnd(TokenSequence<JFXTokenId> ts, int startPos) {
        int result = startPos;
        while (ts.moveNext()) {
            JFXTokenId tid = ts.token().id();
            switch (tid) {
                case IMPORT: {
                    moveTo(ts, JFXTokenId.SEMI);
                    result = ts.offset() + 1;
                    continue;
                }
                case WS:
                    continue;
                default: {
                    return result;
                }
            }
        }
        return result;
    }

    private int quessImportsStart(TokenSequence<JFXTokenId> ts) {
        int posibbleStart = 0;
        while (ts.moveNext()) {
            JFXTokenId tid = ts.token().id();
            switch (tid) {
                case PACKAGE: {
                    moveTo(ts, JFXTokenId.SEMI);
                    posibbleStart = ts.offset() + 1;
                    continue;
                }
                case IMPORT: {
                    posibbleStart = ts.offset();
                    moveTo(ts, JFXTokenId.SEMI);
                    return posibbleStart;
                }
                case WS:
                case COMMENT:
                case LINE_COMMENT:
                case DOC_COMMENT:
                    continue;
                default: {
                    return posibbleStart;
                }
            }
        }
        return posibbleStart;
    }

    private void moveTo(TokenSequence<JFXTokenId> ts, JFXTokenId id) {
        while (ts.moveNext()) {
            if (ts.token().id() == id) return;
        }
    }


    @SuppressWarnings({"unchecked"})
    private static <T extends TokenId> TokenSequence<T> getTokenSequence(Document doc, int dotPos) {
        TokenHierarchy<Document> th = TokenHierarchy.get(doc);
        TokenSequence<T> seq = (TokenSequence<T>) th.tokenSequence();
        seq.move(dotPos);
        return seq;
    }

*/

    void optimize() {

    }

    void append(ModelEntry modelEntry) {
        entries.add(modelEntry);
    }

    public Iterable<? extends ModelEntry> getEntries() {
        return entries;
    }

//    private static class FixItemComparator implements Comparator<FixItem> {
//        public static final Collator collator = Collator.getInstance();
//        public static final Comparator<FixItem> instance = new FixItemComparator();
//
//        public static Comparator<FixItem> get() {
//            return instance;
//        }
//
//        public int compare(FixItem o1, FixItem o2) {
//            if (o1.getSortPriority() == o2.getSortPriority()) {
//                return collator.compare(o1.getElement(), o2.getElement());
//            }
//            return o1.getSortPriority() - o2.getSortPriority();
//        }
//    }

    static class ModelEntry implements Comparable<ModelEntry> {
        String type;
        ImportTree tree;
        boolean stared;
        boolean dStared;
        boolean isUsed = false;

        private ModelEntry(ImportTree tree) {
            this.tree = tree;
            Tree qi = tree.getQualifiedIdentifier();
            type = qi.toString();
            verifyType();
        }

        private void verifyType() {
            stared = type.endsWith(".*");
            dStared = type.endsWith(".**");
            if (stared || dStared) {
                int index = type.indexOf(".*");
                type = type.substring(0, index);
            }
        }

        ModelEntry(String type) {
            this.type = type;
            verifyType();
            isUsed = true;
        }

        boolean includes(String type) {
            if (type == null) return false;
            if (dStared) {
                return type.startsWith(this.type);
            } else if (stared) {
                int dotIndex = type.lastIndexOf('.');
                return dotIndex > -1 && this.type.equals(type.substring(0, dotIndex));
            }
            return this.type.equals(type) || canBeThisType(type);
        }

        private boolean canBeThisType(String type) {
            int index = this.type.lastIndexOf('.');
            return index > 0 && this.type.substring(index).equals(type);
        }

        String toImportStatement() {
            return "import " + type + (stared ? ".*" : "") + (dStared ? ".**" : "") + ";";
        }

        void setUsage() {
            isUsed = true;
        }

        boolean isUsed() {
            return isUsed;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModelEntry that = (ModelEntry) o;

//            if (dStared != that.dStared) return false;
//            if (stared != that.stared) return false;
//            if (tree != null ? !tree.equals(that.tree) : that.tree != null) return false;
            if (type != null ? !type.equals(that.type) : that.type != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (type != null ? type.hashCode() : 0);
//            result = 31 * result + (tree != null ? tree.hashCode() : 0);
//            result = 31 * result + (stared ? 1 : 0);
//            result = 31 * result + (dStared ? 1 : 0);
            return result;
        }

        public int compareTo(ModelEntry o) {
            return type != null ? o != null ? type.compareToIgnoreCase(o.type) : -1 : 1;
        }


        public String toString() {
            return "ModelEntry[" +
                    "type='" + type + '\'' +
                    ']';
        }
    }


}
