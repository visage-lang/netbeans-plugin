/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javafx.source.tasklist;

import com.sun.javafx.api.tree.Tree;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.swing.Action;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.javafx.source.ClassIndex;
import org.netbeans.api.javafx.source.CompilationController;
import org.netbeans.api.javafx.source.CompilationInfo;
import org.netbeans.api.javafx.source.ElementHandle;
import org.netbeans.api.javafx.source.JavaFXSource;
import org.netbeans.api.javafx.source.Task;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.masterfs.providers.AnnotationProvider;
import org.netbeans.modules.masterfs.providers.InterceptionListener;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileStatusEvent;
import org.openide.util.Lookup;

import org.openide.util.lookup.ServiceProvider;
import static org.openide.util.ImageUtilities.assignToolTipToImage;
import static org.openide.util.ImageUtilities.loadImage;
import static org.openide.util.ImageUtilities.mergeImages;
import static org.openide.util.NbBundle.getMessage;

/**
 *
 * @author answer
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
@ServiceProvider(service=AnnotationProvider.class)
public class FXErrorAnnotator extends AnnotationProvider {
    private static final Logger LOGGER = Logger.getLogger(FXErrorAnnotator.class.getName());
    private static final boolean ISDEBUG = LOGGER.isLoggable(Level.FINEST);

    private static final String ERROR_BADGE_URL = "org/netbeans/modules/javafx/source/resources/icons/error-badge.gif"; // NOI18N
    private static final Image ERROR_BADGE_SINGLE;
    private static final Image ERROR_BADGE_FOLDER;

    static {
        URL errorBadgeIconURL = FXErrorAnnotator.class.getClassLoader().getResource(ERROR_BADGE_URL);
        String errorBadgeSingleTP = "<img src=\"" + errorBadgeIconURL + "\">&nbsp;" + getMessage(FXErrorAnnotator.class, "TP_ErrorBadgeSingle"); // NOI18N
        ERROR_BADGE_SINGLE = assignToolTipToImage(loadImage(ERROR_BADGE_URL), errorBadgeSingleTP);
        String errorBadgeFolderTP = "<img src=\"" + errorBadgeIconURL + "\">&nbsp;" + getMessage(FXErrorAnnotator.class, "TP_ErrorBadgeFolder"); // NOI18N
        ERROR_BADGE_FOLDER = assignToolTipToImage(loadImage(ERROR_BADGE_URL), errorBadgeFolderTP);
    }

    final private class ErrorCounter {
        private int count;
        final FileObject fo;
        final private ReadWriteLock lock = new ReentrantReadWriteLock();

        public ErrorCounter(FileObject fo) {
            this.count = 0;
            this.fo = fo;
        }

        public void setError(boolean err) {
            try {
                lock.writeLock().lock();
                if (err ^ isError()) {
                    if (err) {
                        this.count++;
                        filesWithModifiedStatus.add(fo);
                    } else {
                        if (this.count > 0) {
                            if (--count == 0) {
                                filesWithModifiedStatus.add(fo);
                            }
                        }
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean isError() {
            try {
                lock.readLock().lock();
                return this.count > 0;
            } finally {
                lock.readLock().unlock();
            }
        }

        public String toString() {
            return fo.getPath() + " : " + this.count + " errors"; // NOI18N
        }
    }

    final private ExecutorService processor = Executors.newSingleThreadExecutor(new ThreadFactory() {

        public Thread newThread(Runnable r) {
            return new Thread(r, "JavaFX Error Annotator Worker Thread"); // NOI18N
        }
    });

    final private ReadWriteLock errorMapLock = new ReentrantReadWriteLock();
    final private Map<FileObject, ErrorCounter> errorMap = new WeakHashMap<FileObject, ErrorCounter>();
    final private Set<FileObject> filesWithModifiedStatus = new HashSet<FileObject>();

    @Override
    public Action[] actions(Set<? extends FileObject> set) {
        return null;
    }

    @Override
    public Image annotateIcon(Image image, int iconType, Set<? extends FileObject> files) {
        boolean inError = false;
        boolean singleFile = files.size() == 1;

        if (files instanceof NonRecursiveFolder) {
            FileObject folder = ((NonRecursiveFolder) files).getFolder();
            inError = isInError(folder);
            singleFile = false;
        } else {
            for (Object o : files) {
                if (o instanceof FileObject) {
                    FileObject f = (FileObject) o;
                    if (f.isFolder()) {
                        singleFile = false;
                        if (isInError(f)) {
                            inError = true;
                            continue;
                        }
                        if (inError)
                            continue;
                    } else {
                        if (f.isData() && "fx".equals(f.getExt())) { // NOI18N
                            if (isInError(f)) {
                                inError = true;
                            }
                        }
                    }
                }
            }
        }

        if (inError) {
            //badge:
            Image i = mergeImages(image, singleFile ? ERROR_BADGE_SINGLE : ERROR_BADGE_FOLDER, 0, 8);
            boolean found = false;
            for(AnnotationProvider p : Lookup.getDefault().lookupAll(AnnotationProvider.class)) {
                if (found) {
                    Image res = p.annotateIcon(i, iconType, files);

                    if (res != null) {
                        return res;
                    }
                } else {
                    found = p == this;
                }
            }

            return i;
        }
        return null;
    }

    @Override
    public String annotateName(String string, Set<? extends FileObject> set) {
        return null;
    }

    @Override
    public String annotateNameHtml(String string, Set<? extends FileObject> set) {
        return null;
    }

    @Override
    public InterceptionListener getInterceptionListener() {
        return null;
    }

    final public static FXErrorAnnotator getInstance() {
        return Lookup.getDefault().lookup(FXErrorAnnotator.class);
    }

    public void process(final CompilationInfo ci) {
        processor.submit(new Runnable() {

            public void run() {
                if (ISDEBUG) {
                    LOGGER.finest("Processing " + ci.getFileObject().getPath()); // NOI18N
                }
                filesWithModifiedStatus.clear();
                doProcess(ci, ci.isErrors());
                if (ISDEBUG) {
                    LOGGER.finest("===== ERROR MAP ====="); // NOI18N
                    for(ErrorCounter ec : errorMap.values()) {
                        LOGGER.finest(ec.toString());
                    }
                    LOGGER.finest("====================="); // NOI18N
                }
                if (!filesWithModifiedStatus.isEmpty()) {
                    if (ISDEBUG) {
                        StringBuilder sb = new StringBuilder();
                        for(FileObject fo : filesWithModifiedStatus) {
                            sb.append(fo.getName()).append(",");
                        }
                        LOGGER.finest(sb.toString());
                    }
                    try {
                        fireFileStatusChanged(new FileStatusEvent(ci.getFileObject().getFileSystem(), filesWithModifiedStatus, true, false));
                    } catch (FileStateInvalidException ex) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                    }
                }
            }
        });
    }

    private void doProcess(CompilationInfo ci, boolean isError) {
        FileObject origFo = ci.getFileObject();
        if (ISDEBUG) {
            LOGGER.finest("Checking " + origFo); // NOI18N
        }

        final boolean err = ci.isErrors();
        boolean storedErr = getErrorCounter(origFo).isError();

        // don't attempt to process if the error status is not changing
        if (!(storedErr ^ isError)) {
            if (ISDEBUG) {
                LOGGER.finest("Not processing. Old err = " + storedErr + ", new err = " + isError); // NOI18N
            }
            return;
        }

        ErrorCounter errorCnt = getErrorCounter(origFo);

        if (isError ^ errorCnt.isError()) {
            FileObject top = FileOwnerQuery.getOwner(origFo).getProjectDirectory();
            setErrorFlag(errorCnt, isError, top);
        }

        try {
            ClassIndex index = ci.getClasspathInfo().getClassIndex();
            for(Tree t : ci.getCompilationUnit().getTypeDecls()) {
                Element e = ci.getTrees().getElement(ci.getTrees().getPath(ci.getCompilationUnit(), t));
                for(FileObject fo : index.getResources(ElementHandle.create(e),  EnumSet.of(ClassIndex.SearchKind.TYPE_REFERENCES), EnumSet.allOf(ClassIndex.SearchScope.class))) {
                    JavaFXSource.forFileObject(fo).runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController cc) throws Exception {
                            doProcess(cc, err);
                        }

                    }, false);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error compiling javafx source", e); // NOI18N
        }
    }

    private void setParentErrorFlag(FileObject fo, boolean isError, FileObject top) {
        FileObject parent = fo.getParent();
        ErrorCounter errorCnt = getErrorCounter(parent);
        setErrorFlag(errorCnt, isError, top);
    }

    private void setErrorFlag(ErrorCounter errorCnt, boolean isError, FileObject top) {
        if (isError ^ errorCnt.isError()) {
            errorCnt.setError(isError);
            if (errorCnt.fo != top) {
                setParentErrorFlag(errorCnt.fo, isError, top);
            }
        }
    }

    private boolean isInError(FileObject fo) {
        ErrorCounter ec = null;
        try {
            errorMapLock.readLock().lock();
            ec = errorMap.get(fo);
        } finally {
            errorMapLock.readLock().unlock();
        }
        if (ec == null) {
            try {
                JavaFXSource jfxs = JavaFXSource.forFileObject(fo);
                if (jfxs != null) {
                    jfxs.runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController ci) throws Exception {
                            process(ci);
                        }
                    }, true);
                }
            } catch (IOException e) {
                return true;
            }
            return false;
        }
        return ec.isError();
    }

    private ErrorCounter getErrorCounter(FileObject fo) {
        try {
            errorMapLock.writeLock().lock();
            ErrorCounter ec = errorMap.get(fo);
            if (ec == null) {
                ec = new ErrorCounter(fo);
                errorMap.put(fo, ec);
            }
            return ec;
        } finally {
            errorMapLock.writeLock().unlock();
        }
    }
}
