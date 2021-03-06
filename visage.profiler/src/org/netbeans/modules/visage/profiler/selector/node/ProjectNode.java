/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.visage.profiler.selector.node;

import org.netbeans.modules.profiler.selector.spi.nodes.ContainerNode;
import org.netbeans.modules.profiler.selector.spi.nodes.SelectorNode;
import org.netbeans.api.project.Project;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection;
import org.netbeans.modules.visage.profiler.utilities.VisageProjectUtilities;
import org.netbeans.modules.visage.project.VisageProject;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.netbeans.modules.profiler.selector.spi.nodes.IconResource;
import org.netbeans.modules.profiler.selector.spi.nodes.SelectorChildren;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ProjectNode extends ContainerNode {
    private static final String SOURCES_STRING = NbBundle.getMessage(ProjectNode.class,
                                                                     "SelectionTreeBuilderImpl_SourcesString"); // NOI18N
    private static final String LIBRARIES_STRING = NbBundle.getMessage(ProjectNode.class,
                                                                       "SelectionTreeBuilderImpl_LibrariesString"); // NOI18N

    private static class Children extends SelectorChildren<ProjectNode> {

        private final boolean includeSubprojects;

        public Children(boolean includeSubprojects) {
            this.includeSubprojects = includeSubprojects;
        }

        @Override
        protected List<SelectorNode> prepareChildren(ProjectNode parent) {
            List<SelectorNode> nodes = new ArrayList<SelectorNode>(2);
            nodes.add(new SourcesNode(includeSubprojects, parent));
            nodes.add(new LibrariesNode(includeSubprojects, parent));

            return nodes;
        }
    }

    private static class LibrariesNode extends ContainerNode {

        private final boolean includeSubprojects;

        public LibrariesNode(final boolean includeSubprojects, final ContainerNode parent) {
            super(LIBRARIES_STRING, IconResource.LIBRARIES_ICON, parent);
            this.includeSubprojects = includeSubprojects;
        }

        protected SelectorChildren getChildren() {
            return new VisageProjectPackages(VisageProjectPackages.PackageType.Libraries, (VisageProject)getParent().getLookup().lookup(Project.class), includeSubprojects);
        }
    }

    private static class SourcesNode extends ContainerNode {
        private final boolean includeSubprojects;

        public SourcesNode(final boolean includeSubprojects, final ContainerNode parent) {
            super(SOURCES_STRING, IconResource.PACKAGE_ICON, parent);
            this.includeSubprojects = includeSubprojects;
        }

        protected SelectorChildren getChildren() {
            return new VisageProjectPackages(VisageProjectPackages.PackageType.Source, (VisageProject)getParent().getLookup().lookup(Project.class), includeSubprojects);
        }
    }

    /** Creates a new instance of ProjectNode */
    public ProjectNode(final Project project, ContainerNode root) {
        super(ProjectUtilities.getProjectName(project), ProjectUtilities.getProjectIcon(project), root, Lookups.fixed(project)); // NOI18N
        setValid(ProjectUtilities.getClasspathInfo(project, true) != null);
    }

    public ProjectNode(Project project) {
        this(project, null);
    }

    @Override
    public Collection<SourceCodeSelection> getRootMethods(boolean all) {
        Collection<SourceCodeSelection> roots = new ArrayList<SourceCodeSelection>();
        Enumeration children = children();

        while (children.hasMoreElements()) {
            roots.addAll(((SelectorNode) children.nextElement()).getRootMethods(all));
        }

        return roots;
    }

    protected SelectorChildren getChildren() {
        return new Children(false);
    }
}
