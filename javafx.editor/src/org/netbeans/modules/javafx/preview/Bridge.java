/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.modules.javafx.preview;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.rmi.registry.*;

import javax.swing.UIManager;
import org.openide.execution.NbProcessDescriptor;
import org.openide.util.Exceptions;
import org.openide.modules.ModuleInstall;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;
import org.netbeans.api.javafx.source.CompilationController;
import org.netbeans.api.javafx.source.JavaFXSource;
import org.netbeans.api.javafx.source.JavaFXSource.Phase;
import org.netbeans.api.javafx.source.Task;
import org.netbeans.modules.javafx.editor.JavaFXDocument;

public class Bridge extends ModuleInstall {
    private static NbProcessDescriptor nb = null;
    private static HashMap <Document, PreviewSideServerFace> previewSideServerFaces = new HashMap <Document, PreviewSideServerFace>();
    private static HashMap <Document, NBSidePreviewServer> nbSideServers = new HashMap <Document, NBSidePreviewServer>();

    @Override
    public boolean closing() {
        try {
            previewDispatcher.terminate();
        } catch (RemoteException ex) {
            Exceptions.printStackTrace(ex);
        }
        return super.closing();
    }
    
    static boolean hasWhitespace(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }  
    
    public synchronized static void closePreview(JavaFXDocument document) {
        if (previewSideServerFaces.containsKey(document)) {
            try {
                previewDispatcher.stopPreview(document.hashCode());
                Integer hashCode = document.hashCode();
                registry.unbind(NB_SIDE + " " + hashCode);
                UnicastRemoteObject.unexportObject(nbSideServers.get(document), true);
                previewSideServerFaces.remove(document);
                nbSideServers.remove(document);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void moveToFront(JavaFXDocument document) {
        if (previewSideServerFaces.containsKey(document)) {
            try {
                previewSideServerFaces.get(document).moveToFront();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public synchronized static PreviewSideServerFace getPreview(JavaFXDocument document) {
        if (previewSideServerFaces.containsKey(document)) {
            return previewSideServerFaces.get(document);
        } else {
            PreviewSideServerFace previewSideServerFace = null;
            try {
                NBSidePreviewServer preview = new NBSidePreviewServer(document);
                UnicastRemoteObject.unexportObject(preview, true);
                NBSideServerFace stub = (NBSideServerFace) UnicastRemoteObject.exportObject(preview, 0);     
                Integer hashCode = document.hashCode();
                registry.rebind(NB_SIDE + " " + hashCode, stub);                                                                    
                previewDispatcher.createPreview(document.hashCode(), document.getDataObject().getPrimaryFile().getNameExt(), document.getPreviewLocation(), document.getPreviewSize());
                previewSideServerFace = (PreviewSideServerFace) registry.lookup(PREVIEW_SIDE + " " + hashCode);
                preview.setPreviewSideServerFace(previewSideServerFace);
                previewSideServerFaces.put(document, previewSideServerFace);
                nbSideServers.put(document, preview);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return previewSideServerFace;
        }
    }
    
    static void startClient () {
        String home = System.getProperty(JAVA_HOME);
        String exePath = null;
        String exe = JAVA;
        if (home.length() > 0) {
            String os_arch = System.getProperty("os.arch");                     //NOI18
            if ("SunOS".equals(System.getProperty("os.name")) &&                //NOI18
               ("sparcv9".equals(os_arch) || "amd64".equals(os_arch))) {        //NOI18
                exePath = home + File.separator + "bin" + File.separator +      //NOI18
                    os_arch + File.separator + exe;
            } else {
                exePath = home + File.separator + "bin" + File.separator + exe; //NOI18
            }
        } else {
            exePath = exe;
        }
        if (hasWhitespace(exe)) {
            exePath = "\"" + exePath + "\"";
        }
        
        String classs = "org/netbeans/modules/javafx/preview/Main";                                                                             //NOI18
        String path = Bridge.class.getClassLoader().getResource(classs + ".class").getPath();                                                   //NOI18
        String jarPath = path.substring(0, path.indexOf('!')).substring(5);
        String args = "-Djava.class.path=\"" + System.getProperty("java.class.path") + File.pathSeparator + jarPath + File.pathSeparator +      // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-loaders.jar" + File.pathSeparator +                                         // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-nodes.jar" + File.pathSeparator +                                           // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-execution.jar" + File.pathSeparator +                                       // NOI18
                System.getProperty(NB_HOME) + "/core/org-openide-filesystems.jar" + File.pathSeparator +                                        // NOI18
                System.getProperty(NB_HOME) + "/modules/org-netbeans-swing-tabcontrol.jar" + File.pathSeparator +                               // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-awt.jar" + File.pathSeparator +                                             // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-windows.jar" + File.pathSeparator +                                         // NOI18
                System.getProperty(NB_HOME) + "/lib/org-openide-util.jar" + File.pathSeparator +                                                // NOI18
                System.getProperty(NB_HOME) + "/modules/org-openide-text.jar" + "\" " +                                                         // NOI18
        "" + //"-agentlib:jdwp=transport=dt_socket,address=8003,server=y,suspend=n " +                                                                 // NOI18
        classs;
        
        nb = new NbProcessDescriptor(exePath, args);
        try {
            nb.exec();
        } catch (Throwable ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    static private Registry registry = null;
    static private NBSideDispatchingServer nbDispatcher = null;
    static private PingThread pingThread = null;
    static PreviewSideDispatchingServerFace previewDispatcher = null;
    private static volatile boolean isStarted = false;
    private static EventListenerList startListeners = new EventListenerList();
    
    public static void addStartListener(ChangeListener l) {
        startListeners.add(ChangeListener.class, l);
    }
    
    public static boolean isStarted() {
        return isStarted;
    }
    
    private static void notifyStarted() {
        ChangeEvent e = new ChangeEvent(Bridge.class);
        for (ChangeListener l : startListeners.getListeners(ChangeListener.class)) {
            l.stateChanged(e);
        }
    }
    
    public static void start() {
        new Thread() {
            @Override
            public void run() {
                try {
                    registry = LocateRegistry.createRegistry(1099);

                    nbDispatcher = new NBSideDispatchingServer();
                    UnicastRemoteObject.unexportObject(nbDispatcher, true);
                    NBSideDispatchingServerFace nbSideDispatchingStub = (NBSideDispatchingServerFace) UnicastRemoteObject.exportObject(nbDispatcher, 0);     
                    registry.rebind(NB_SIDE, nbSideDispatchingStub);

                    startClient();

                    while (!nbDispatcher.isPreviewStarted()) {
                        Thread.sleep(100);
                    }

                    previewDispatcher = (PreviewSideDispatchingServerFace) registry.lookup(PREVIEW_SIDE);
                    previewDispatcher.setLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
                    pingThread = new PingThread();
                    pingThread.start();
                    isStarted = true;
                    notifyStarted();
                } catch (Exception ex) {
                    Logger.getLogger(Bridge.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    
    static class PingThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    if (previewDispatcher != null)
                        previewDispatcher.ping();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        interrupt();
                    }
                } catch (Throwable ex) {
                    if (!isInterrupted()) restart();
                    interrupt();
                }
            }
        }
    }
    
    public synchronized static void restart() {
        pingThread.interrupt();
        new Thread() {
            @Override
            public void run() {
                isStarted = false;
                notifyStarted();
                try {
                    previewDispatcher.terminate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                nbDispatcher.clearStartedStatus();
                previewDispatcher = null;

                previewSideServerFaces.clear();
                Object docs[] = nbSideServers.keySet().toArray();
                nbSideServers.clear();

                startClient();

                while (!nbDispatcher.isPreviewStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                try {
                    previewDispatcher = (PreviewSideDispatchingServerFace) registry.lookup(PREVIEW_SIDE);
                    previewDispatcher.setLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
                    for (Object doc : docs) {
                        final JavaFXSource js = JavaFXSource.forDocument((JavaFXDocument)doc);
                        try {
                            js.runUserActionTask(new Task<CompilationController>() {
                                public void run(CompilationController controller) throws Exception {
                                    controller.toPhase(Phase.CODE_GENERATED);
                                    PreviewCodeGenerate.process(controller);
                                }
                            }, true);
                        } catch (Exception exx) {
                            exx.printStackTrace();
                        }
                    }
                    pingThread = new PingThread();
                    pingThread.start();
                    isStarted = true;
                    notifyStarted();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }.start();
    }
    
    private static String PREVIEW_SIDE = "PreviewSide";   // NOI18
    private static String NB_SIDE = "NBSide";             // NOI18
    private static String NB_HOME = "netbeans.home";      // NOI18
    private static String JAVA_HOME = "java.home";        // NOI18
    private static String JAVA = "java";                  // NOI18
}