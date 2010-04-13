/*
 * Created on Mar 29, 2004
 */
package debugger.tests_se;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ThreadStartRequest;

import debugger.framework.DebugService;
import debugger.framework.DebuggerTest;
import debugger.framework.VMCapabilities;

/**
 * @author kh148139
 */
public class VMTest01 implements DebuggerTest {

    private final static int BREAKPOINT_LINE = 7;

    private final static long SUSPEND_DURATION = 1000;

    public static String THREAD_CLASS_NAME = "main";
    public static String MAIN_CLASS_NAME = "testsuite.Main";

    private DebugService debugService;
    private ArrayList requests = new ArrayList();

    private java.io.PrintStream ref;

    public VMTest01(DebugService debugService, java.io.PrintStream ref) {
        this.ref = ref;
        this.debugService = debugService;
        this.debugService.addDebuggerTest(this);
    }

    public boolean isVMCompatible(VMCapabilities vmCapabilities) {
        return true;
    }

    public void eventUpdate(DebugService service, Event event) {
    	long vmSuspendStart = 0;
        if (event instanceof ThreadStartEvent) {
            ThreadStartEvent threadStartEvent = (ThreadStartEvent) event;
            String threadClassName = threadStartEvent.thread().referenceType().name();
            System.out.println(" t " + threadClassName );
            if (threadClassName.equals(THREAD_CLASS_NAME)) {
                ref.println("\t\tThreadStartEvent : " + threadClassName);
                System.out.println(" - " + "ThreadStartEvent : " + threadClassName);
                ReferenceType refType = threadStartEvent.thread().referenceType();
                try {
                    this.debugService.setBreakPoint(refType, BREAKPOINT_LINE);
                } catch (AbsentInformationException e) {
                    e.printStackTrace();
                    e.printStackTrace(ref);
                } catch (IncompatibleThreadStateException e) {
                    e.printStackTrace();
                    e.printStackTrace(ref);
                }
                ref.println("\t\tsuspendVM : " + threadClassName);
                System.out.println(" - " + "suspendVM : " + threadClassName );
                vmSuspendStart = System.currentTimeMillis();
                this.debugService.suspendVM(SUSPEND_DURATION);
            }
        }
        else if (event instanceof BreakpointEvent) {
        	BreakpointEvent brev = (BreakpointEvent) event;
        	ReferenceType refType = brev.thread().referenceType();
        	long timeDiff = System.currentTimeMillis() - vmSuspendStart;
        	if (timeDiff < SUSPEND_DURATION) {
        		ref.println("\t\tERR - VM suspend duration is " + SUSPEND_DURATION + "ms however BreakpointEvent came after only " + timeDiff + "ms");
        	}
        	else {
        		ref.println("\t\tOK - VM suspend duration is " + SUSPEND_DURATION + "ms BreakpointEvent came after the suspend duration expired");
        	}
            BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            Location location = breakpointEvent.location();
            ref.println("\t\tBreakpoint reached in " + location.declaringType().name() + " line " + location.lineNumber());
            List classes = this.debugService.getVM().allClasses();
            ref.println("\n\t\tNum classes in VM: " + classes.size());
            for (Iterator iter = classes.iterator(); iter.hasNext();) {
				ReferenceType classRef = (ReferenceType) iter.next();
				//System.out.println("Class in VM : " + classRef.name());
				ref.println("\t\t\tClass : " + classRef.name());
			}
            List threads = this.debugService.getVM().allThreads();
            ref.println("\n\t\tNum threads in VM: " + threads.size());
            for (Iterator iter = threads.iterator(); iter.hasNext();) {
				ThreadReference threadRef = (ThreadReference) iter.next();
				//System.out.println("Thread in VM :" + threadRef.name());
				ref.println("\t\t\tThread in VM :" + threadRef.name());
			}
            this.debugService.getVM().exit(0);
            ref.println("\n\t\tException should follow:\n");
            try {
				//this should NOT work if the exit command went well
				this.debugService.setBreakPoint(refType, BREAKPOINT_LINE + 1);
			} catch (AbsentInformationException e) {
				e.printStackTrace();
				e.printStackTrace(ref);
			} catch (IncompatibleThreadStateException e) {
				e.printStackTrace();
				e.printStackTrace(ref);
			}
        }
    }

    public void registerRequests(EventRequestManager eventRequestManager) {
        ThreadStartRequest tsr = eventRequestManager.createThreadStartRequest();
        tsr.enable();
        this.requests.add(tsr);
    }

    public void unregisterRequests(EventRequestManager eventRequestManager) {
        eventRequestManager.deleteEventRequests(requests);
    }

}