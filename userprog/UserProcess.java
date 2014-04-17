//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    	UserKernel.newPIDLock.acquire();
    	pid = UserKernel.newPID;
    	UserKernel.newPID++;
    	UserKernel.newPIDLock.release();
    	
    	parent = null;
    	childpid = new HashSet<Integer>();
    	childExitStatus = new HashMap<Integer , Integer>();
    	joinpid = -1;
    	
    	childExitStatusLock = new Lock();
    	joinLock = new Lock();
    	joinCond = new Condition(joinLock);
    	
    	files = new OpenFile[numFileDescriptors];
    	files[0] = UserKernel.console.openForReading();
    	files[1] = UserKernel.console.openForWriting();
    	for (int i = 2; i < numFileDescriptors; i++)
    		files[i] = null;
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
    	if (!load(name, args))
    		return false;

    	UserKernel.numRunningProcsLock.acquire();
    	UserKernel.numRunningProcs++;
    	UserKernel.numRunningProcsLock.release();
    	
    	new UThread(this).setName(name).fork();

    	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * @param vpn
     * @param isWrite
     * @return -1 if vpn is invalid
     */
    public int vpn2ppn(int vpn , boolean isWrite) {
    	if (vpn < 0 || vpn >= pageTable.length)
    		return -1;
    	if (!pageTable[vpn].valid)
    		return -1;
    	if (isWrite) {
    		if (pageTable[vpn].readOnly)
    			return -1;
    		pageTable[vpn].dirty = true;
    	}
    	pageTable[vpn].used = true;
    	return pageTable[vpn].ppn;
    }
    
    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    	byte[] memory = Machine.processor().getMemory();

    	if (vaddr < 0)
    		return 0;
    	int amount = 0;
    	
    	while (length > 0) {
    		int vpn = Processor.pageFromAddress(vaddr);
    		int voffset = Processor.offsetFromAddress(vaddr);
    		int ppn = vpn2ppn(vpn , false);
    		if (ppn == -1)
    			return amount;
    		
    		int bytesRead = Math.min(length , pageSize - voffset);
    		int paddr = ppn * pageSize + voffset;
    		System.arraycopy(memory , paddr , data , offset , bytesRead);
    		
    		length -= bytesRead;
    		vaddr += bytesRead;
    		offset += bytesRead;
    		amount += bytesRead;
    	}
    	
    	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    	byte[] memory = Machine.processor().getMemory();

    	if (vaddr < 0)
    		return 0;
    	int amount = 0;
    	
    	while (length > 0) {
    		int vpn = Processor.pageFromAddress(vaddr);
    		int voffset = Processor.offsetFromAddress(vaddr);
    		int ppn = vpn2ppn(vpn , false);
    		if (ppn == -1)
    			return amount;
    		
    		int bytesWrite = Math.min(length , pageSize - voffset);
    		int paddr = ppn * pageSize + voffset;
    		System.arraycopy(data , offset , memory , paddr , bytesWrite);
    		
    		length -= bytesWrite;
    		vaddr += bytesWrite;
    		offset += bytesWrite;
    		amount += bytesWrite;
    	}
    	
    	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;

	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    	UserKernel.memoryLock.acquire();
    	
    	if (numPages > UserKernel.freePhysPages.size()) {
    		coff.close();
    		Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		UserKernel.memoryLock.release();
    		return false;
    	}
    	
    	pageTable = new TranslationEntry[numPages];
    	for (int i = 0; i < numPages; i++) {
    		int ppn = UserKernel.freePhysPages.removeFirst();
    		pageTable[i] = new TranslationEntry(i , ppn , true , false , false , false);
    	}
    	UserKernel.memoryLock.release();

    	// load sections
    	for (int s=0; s<coff.getNumSections(); s++) {
    		CoffSection section = coff.getSection(s);

    		Lib.debug(dbgProcess, "\tinitializing " + section.getName()
    				+ " section (" + section.getLength() + " pages)");

    		for (int i=0; i<section.getLength(); i++) {
    			int vpn = section.getFirstVPN()+i;
    			int ppn = pageTable[vpn].ppn;
    			pageTable[vpn].readOnly = section.isReadOnly();
    			section.loadPage(i, ppn);
    		}
    	}

    	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	UserKernel.memoryLock.acquire();
    	for (int i = 0; i < pageTable.length; i++) {
    		UserKernel.freePhysPages.add(pageTable[i].ppn);
    	}
    	UserKernel.memoryLock.release();
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
    	if (pid == 0) {
    		Machine.halt();
    		Lib.assertNotReached("Machine.halt() did not halt machine!");
    	}
    	return 0;
    }
    
    private int handleExit(int status) {
    	for (int i = 0; i < numFileDescriptors; i++)
    		handleClose(i);
    	unloadSections();
    	
    	UserKernel.numRunningProcsLock.acquire();
    	UserKernel.numRunningProcs--;
    	if (UserKernel.numRunningProcs == 0) {
    		UserKernel.numRunningProcsLock.release();
    		Kernel.kernel.terminate();
    		return 0;
    	}
    	UserKernel.numRunningProcsLock.release();
    	
    	if (parent != null) {
    		parent.childExitStatusLock.acquire();
    		parent.childExitStatus.put(pid , status);
    		parent.childExitStatusLock.release();
    		
    		parent.joinLock.acquire();
    		if (parent.joinpid == pid) {
    			parent.joinCond.wake();
    			parent.joinpid = -1;
    		}
    		parent.joinLock.release();
    	}
    	
    	KThread.finish();
    	return 0;
    }
    
    private int handleExec(int execAddr , int argc , int argvAddrStart) {
    	String exec = readVirtualMemoryString(execAddr , 256);
    	if (exec == null)
    		return -1;
    	
    	byte[] argvAddrs = new byte[argc * 4];
    	int readAmount = readVirtualMemory(argvAddrStart , argvAddrs);
    	if (readAmount != argvAddrs.length)
    		return -1;
    	
    	String[] argv = new String[argc];
    	for (int i = 0; i < argc; i++) {
    		int argvAddr = (((int)argvAddrs[4 * i + 3] & 0xFF) << 24) |
    				(((int)argvAddrs[4 * i + 2] & 0xFF) << 16) |
    				(((int)argvAddrs[4 * i + 1] & 0xFF) << 8) |
    				((int)argvAddrs[4 * i] & 0xFF);
    		argv[i] = readVirtualMemoryString(argvAddr , 256);
    		if (argv[i] == null)
    			return -1;
    	}
    	
    	UserProcess childProc = newUserProcess();
    	childProc.parent = this;
    	childpid.add(childProc.pid);
    	if (!childProc.execute(exec , argv)) {
    		childProc.handleClose(0);
    		childProc.handleClose(1);
    		return -1;
    	}
    	return childProc.pid;
    }

    private int handleJoin(int joinpid , int statusAddr) {
    	try {
    		if (!childpid.contains(joinpid))
        		return -1;
        	childExitStatusLock.acquire();
        	while (!childExitStatus.containsKey(joinpid)) {
        		childExitStatusLock.release();
        		joinLock.acquire();
        		this.joinpid = joinpid;
        		joinCond.sleep();
        		joinLock.release();	
        		childExitStatusLock.acquire();
        	}
        	childExitStatusLock.release();
        	int status = childExitStatus.get(joinpid);
        	if (status == -1)
        		return 0;
        	else {
        		writeVirtualMemory(statusAddr , Lib.bytesFromInt(status));
        		return 1;
        	}
    	}
    	catch (Exception ex) {
    		return 0;
    	}
    }
    
    private int handleCreat(int nameAddr) {
    	try {
    		String name = readVirtualMemoryString(nameAddr , 256);
        	OpenFile file = UserKernel.fileSystem.open(name , true);
        	
        	if (UserKernel.unlinkWaitingList.contains(name))
        		return -1;
        	
        	if (file != null) {
        		if (UserKernel.files.containsKey(name)) {
        			int numRef = UserKernel.files.get(name) + 1;
        			UserKernel.files.put(name , numRef);
        		}
        		else {
        			UserKernel.files.put(name , 1);
        		}
        		
        		for (int i = 0; i < files.length; i++) {
        			if (files[i] == null) {
        				files[i] = file;
        				return i;
        			}
        		}
        	}
        	return -1;
    	}
    	catch (Exception ex) {
    		return -1;
    	}
    }

    private int handleOpen(int nameAddr) {
    	try {
    		String name = readVirtualMemoryString(nameAddr , 256);
        	OpenFile file = UserKernel.fileSystem.open(name , false);
        	
        	if (UserKernel.unlinkWaitingList.contains(name))
        		return -1;
        	
        	if (file != null) {
        		if (UserKernel.files.containsKey(name)) {
        			int numRef = UserKernel.files.get(name) + 1;
        			UserKernel.files.put(name , numRef);
        		}
        		else {
        			UserKernel.files.put(name , 1);
        		}
        		
        		for (int i = 0; i < files.length; i++) {
        			if (files[i] == null) {
        				files[i] = file;
        				return i;
        			}
        		}
        	}
        	return -1;
    	}
    	catch (Exception ex) {
    		return -1;
    	}
    }
    
    private int handleRead(int fd , int bufAddr , int size) {
    	try {
    		if (!isValidFD(fd))
    			return -1;
    		if (files[fd] != null) {
    			byte[] readBuf = new byte[size];
    			int sizeRead = files[fd].read(readBuf , 0 , size);
    			int res = writeVirtualMemory(bufAddr , readBuf , 0 , sizeRead);
    			return res;
    		}
    		return -1;
    	}
    	catch (Exception ex) {
    		return -1;
    	}
    }
    
    private int handleWrite(int fd , int bufAddr , int size) {
    	try {
    		if (!isValidFD(fd))
    			return -1;
    		if (files[fd] != null) {
    			byte[] writeBuf = new byte[size];
    			int sizeWrite = readVirtualMemory(bufAddr , writeBuf , 0 , size);
    			int res = files[fd].write(writeBuf , 0 , sizeWrite);
    			if (res != size)
    				return -1;
    			return res;
    		}
    		return -1;
    	}
    	catch (Exception ex) {
    		return -1;
    	}
    }
    
    private int handleClose(int fd) {
    	try {
    		if (!isValidFD(fd))
        		return -1;
        	OpenFile file = files[fd];
        	if (file != null) {
        		if (fd >= 2) {
        			String name = file.getName();
        			int numRef = UserKernel.files.get(name) - 1;
        			if (numRef > 0)
        				UserKernel.files.put(name , numRef);
        			else
        				UserKernel.files.remove(name);
        			
        			if (numRef == 0 && UserKernel.unlinkWaitingList.contains(name)) {
        				UserKernel.unlinkWaitingList.remove(name);
        				UserKernel.fileSystem.remove(name);
        			}
        		}
        		
        		files[fd].close();
        		files[fd] = null;
        		return 0;
        	}
        	return -1;
    	}
    	catch (Exception ex) {
    		return -1;
    	}
    }
    
    private int handleUnlink(int nameAddr) {
    	String name = readVirtualMemoryString(nameAddr , 256);
    	if (name != null) {
    		if (UserKernel.files.containsKey(name)) {
    			UserKernel.unlinkWaitingList.add(name);
    			return -1;
    		}
    		else {
    			if (UserKernel.unlinkWaitingList.contains(name))
    				UserKernel.unlinkWaitingList.remove(name);
    			if (UserKernel.fileSystem.remove(name))
    				return 0;
    		}
    	}
    	return -1;
    }
    
    private static final int
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    	switch (syscall) {
    	case syscallHalt:
    		return handleHalt();
    	
    	case syscallExit:
    		return handleExit(a0);
    		
    	case syscallExec:
    		return handleExec(a0 , a1 , a2);
    		
    	case syscallJoin:
    		return handleJoin(a0 , a1);
    		
    	case syscallCreate:
    		return handleCreat(a0);
    		
    	case syscallOpen:
    		return handleOpen(a0);
    	
    	case syscallRead:
    		return handleRead(a0 , a1 , a2);
    	
    	case syscallWrite:
    		return handleWrite(a0 , a1 , a2);
    		
    	case syscallClose:
    		return handleClose(a0);
    	
    	case syscallUnlink:
    		return handleUnlink(a0);

    	default:
    		Lib.debug(dbgProcess, "Unknown syscall " + syscall);
    		Lib.assertNotReached("Unknown system call!");
    	}
    	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;

	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** Helper functions */
    private boolean isValidFD(int fd) {
    	return (fd >= 0 && fd < numFileDescriptors);
    }
    
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    protected final int numFileDescriptors = 16;
    protected OpenFile[] files;
    
    private int pid;
    private UserProcess parent;
    private HashSet<Integer> childpid;
    private HashMap<Integer , Integer> childExitStatus;
    private int joinpid;
    
    private Lock childExitStatusLock;
    private Lock joinLock;
    private Condition joinCond;
}
