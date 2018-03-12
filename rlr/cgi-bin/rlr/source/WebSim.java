import watch.*;
import parse.*;
import pointer.*;
import java.applet.Applet;
import java.util.*;
import java.awt.*;
import java.io.*;


/** Parse a string (from the Web page, a file, or a URL), and build and
  * run the objects that it describes.  It should describe one or more
  * objects of type Project.  The first will
  * be embedded on the Web page, and each of the others will be
  * placed in a separate window.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.05, 11 May 97
  * @author Leemon Baird
  */
public class WebSim extends Applet implements Parsable {
  //v. 1.05 11 May 97 variable watchManager added so VRML can interface to WebSim variables
  //v. 1.04  6 May 97
  //v. 1.03 26 Mar 97
  //v. 1.02 23 Aug 96

  /** This static variable allows any thread in memory (such as another Applet like VRMLInterface)
    * to communicate with WebSim's WatchManager.  If there are several WatchManagers,
    * then it gives the one belonging to the last WebSim Project that finished
    * initializing.  To wait for the WatchManager to become available then put it
    * into a new local variable called watchManager, run the following code:    <pre>
    *
    *            WatchManager watchManager;
    *            synchronized(WebSim.watchManagerGuard) {
    *                if (WebSim.watchManager==null)
    *                        try {
    *                            WebSim.watchManagerGuard.wait();
    *                        } catch(InterruptedException e) {
    *                        }
    *                watchManager=WebSim.watchManager;
    *            }
    *                                                                          </pre>
    */
  public static WatchManager watchManager=null;
  /** Synchronize on watchManagerGuard before attempting to read or write watchManager.
    * Why is there a separate variable just for the guard?  This is a workaround for a BUG:
    * it's needed in Netscape 3.0 for SunOs 4, but not in Netscape for Win95/NT.
    */
  public static Double watchManagerGuard=new Double(0);

  /** All projects check this occasionally, and restart when it changes */
  public static int restartNumber=0;

  /** array of all the Projects */
  protected ProjWin[] wins=null;

  protected boolean unparseAtEnd=false;  //unparse everything after parseing?
  /** The thread group containing this thread (and all other WebSim threads) */
  protected ThreadGroup myThreadGroup=null;
  /** If nonnull, the string containing the BNF code */
  protected String bnf=null;
  /** Have all threads created by WebSim (and the applet thread itself) been suspended? */
  protected PBoolean allThreadsSuspended=new PBoolean(false);
  //more variables are defined above unparseAll()

  /** constructor that does nothing */
  public WebSim() {super();} //CAFE bug: this must be typed here, even though it's supposedly redundant

  private Hashtable defUseLabels=null; //#DEF/#USE labels found while parsing, used while unparsing

  /** Instantiate a WebSim manually rather than with a browser or applet viewer.
    * The BNF code is in bnfString, and the window has upper-left corner (x,y) and
    * dimensions (width,height).
    */
  public WebSim(String bnfString,int x, int y, int width, int height) {
    super();
    bnf=bnfString;
    ClosableFrame f=new ClosableFrame(this);
    f.reshape(x,y,width,height);
    resize(width,height);
    f.add("Center",this);
    f.show();
    init();
    start();
  }

  /** Information about the applet that a browser
    * or Java interpretor can give the user
    */
  public String getAppletInfo() {
    return "WebSim, c 1996, 1997 Leemon Baird, "+
           "http://www.cs.cmu.edu/~baird, "+
           "leemon@cs.cmu.edu";
  }

  /** Parameter info that a browser or Java
    * interpretor can give the user
    */
  public String[][] getParameterInfo() {
    String[][] info = {
      {"sourceText", "string", "WebSim BNF code, in one long strng"},
      {"sourceFile", "string", "Filename containing WebSim BNF code (if no sourceText tag)"},
      {"sourceURL",  "string", "URL contining WebSim BNF code (if no sourceText or sourceFile tags)"}
     };
    return info;
  }

  /** Initialize the applet with a white background */
  public void init() {
    setBackground(Color.white);
  }//end init

  /** Parse projects and start them running in separate threads */
  public void start() {
    setBackground(Color.white);
    myThreadGroup=Thread.currentThread().getThreadGroup();
    parseAndRun();
  } //end start

  /** parses the BNF string and runs a set of projects. */
  public void parseAndRun() {
    Parser parser=null;
    wins=null;
    unparseAtEnd=false;
    allThreadsSuspended=new PBoolean(false);
    parser=new Parser(); //the parser that parses the string
    parser.applet=this; //let other objects know which Applet is parsing
    if (!parser.startParseString(bnf)) //parse a string given in the constructor if there is one
      if (!parser.startParseString(getParameter("sourceText"))) //else parse a string parameter if there is one
        if (!parser.startParseFile(getParameter("sourceFile"))) //else parse a file if there is one
          if (!parser.startParseURL (getParameter("sourceURL")))   //else parse a URL if there is one
            System.out.println("HTML doesn't have valid <sourceText>, <sourceFile>, or <sourceURL> tag");
    try {
      if (parser.parseID("SavedState",false))
        parse(parser,-1); //load in the state saved in the middle of an experiment
      else
        parse(parser,1);  //load in an experiment and start from the beginning
    } catch (ParserException e) {
      e.print();
    } finally {
      defUseLabels=parser.labels; //tell the unparser later about #DEF/#USE labels
      parser.close();   //close the file that was parsed
    }

    if (unparseAtEnd) {  //unparse the entire file
      unparser=new Unparser(System.out,3,-1,78,defUseLabels); //indent by 3, no max indent, 78-col lines
      try {
        unparse(unparser,1); //unparse language 1 (should be same as input)
      } finally {
        unparser.close();
      }
    }
    synchronized(watchManagerGuard) { //let any other applet in memory (e.g. VRMLInterface) be able access the WatchManager
      watchManager=wins[0].project.watchManager;
      watchManagerGuard.notifyAll();
    }
    invalidate();
    validate();
  }//end parseAndRun

  /** The browser calls this when the document is no longer on
    * the screen.  It should kill all threads and release all
    * graphics contexts.
    */
  public void stop() {
    //this is a hack.  Netscape ought to do this
    //automatically, but it doesn't, so I'll do it here.
    //It's possible that other browsers might not like this.
    //To test it, load a WebSim page, go elsewhere, and return.
    //If the elsewhere is another WebSim page running the
    //ShowThreads project, then you can see whether the threads
    //from the first page were properly killed.
    System.out.println("WebSim quitting");/**/
    destroyAll();
    super.stop();
  }//end stop

  /** close all windows, kill all threads, dispose of all Graphics contexts */
  public void destroyAll() {
    if (wins!=null)
      for (int i=0;i<wins.length;i++)
        if (wins[i]!=null) {
          wins[i].destroy();
        }
  }

  /** The browser calls this when quitting.
    * It should kill all threads and release all
    * graphics contexts.
    */
  public void destroy() {
    //the following is a hack.  Netscape ought to do this
    //automatically, but it doesn't, so I'll do it here.
    //It's possible that other browsers might not like this.
    //To test it, load a WebSim page, go elsewhere, and return.
    //If the elsewhere is another WebSim page running the
    //ShowThreads project, then you can see whether the threads
    //from the first page were properly killed.
    destroyAll();
    super.destroy();
  }//end destroy

  /** stop all threads created by WebSim */
  public void stopAllThreads() {
    myThreadGroup.stop();
  }

  /** suspend all threads created by WebSim */
  public void suspendAllThreads() {
    synchronized(allThreadsSuspended) {
///**/      if (allThreadsSuspended.val)
///**/        return;

      //myThreadGroup.suspend();  /**///this didn't work under Netscape
      Thread[] active=new Thread[myThreadGroup.activeCount()];
      myThreadGroup.enumerate(active);
      for (int i=0;i<active.length;i++)
        if (active[i]!=Thread.currentThread()) { //don't suspend myself
          active[i].suspend();
        }

      allThreadsSuspended.val=true;
    }
  }

  /** resume all threads created by WebSim that have been suspended */
  public void resumeAllThreads() {
    synchronized(allThreadsSuspended) {
      if (!allThreadsSuspended.val)
        return;
      myThreadGroup.resume();
      allThreadsSuspended.val=false;
    }
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "['unparse'] ProjWin * //Parse and run a WebSim program. "+
           "Optionally, unparse everything back out to standard out "+
           "immediately after parsing everything.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (unparseAtEnd)
      u.emitLine("unparse");
    if (wins!=null)
      for (int i=0;i<wins.length;i++) {
        u.emitLine();
        u.emitUnparse(wins[i],lang);
        u.emitLine();
      }
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector winsVector=null; //every ProjWin parsed
    unparseAtEnd=p.parseID("unparse",false);
    show();
    winsVector=p.parseClassList("ProjWin",lang,false);
    if (winsVector!=null) {
      wins=new ProjWin[winsVector.size()];
      winsVector.copyInto(wins);
    }

    return this;
  } //end method parse

  //the following are used in unparseAll() and in paint() to save
  //the unparse of everything to disk.
  Frame            f          =null; //parent of the Dialog
  boolean          write      =true; //should overwrite the existing file?
  boolean          writeToDisk=true; //should write file to disk?
  FileDialog       fd         =null;
  File             fn         =null;
  SaveAllDialog    s          =null;
  FileOutputStream file       =null;
  PrintStream      ps         =null;
  Unparser         unparser   =null;

  /** Unparse everything into a file.*/
  public void unparseAll() {
    f          =new Frame("");
    write      =true; //should overwrite the existing file?
    writeToDisk=true; //should write file to disk?
    fd         =new FileDialog(f,"Enter filename for saving WebSim projects");
    fn         =null;
    s          =null;
    file       =null;
    ps         =null;
    unparser   =null;

    try {
      suspendAllThreads();
      fd.show();
      fn=null;
      if (fd.getDirectory()!=null && fd.getFile()!=null)
        fn=new File(fd.getDirectory(),fd.getFile());
      if (fn==null) //user hit CANCEL in the file dialog
        resumeAllThreads();
      else if (fn.exists())
        s=new SaveAllDialog(this,"WARNING: "+fd.getFile()+" exists.  Overwrite it?");
      else
        saveFile();
    } catch (Throwable ee) { //don't have disk access priv., etc.
      System.out.println("Error while writing image to disk");
      System.out.println(ee);
      ee.printStackTrace();
    }
  }//end unparseAll

  public void saveFile() {
    try {
      file    =new FileOutputStream(fn); //or say: file=System.out to save to standard out
      ps      =new PrintStream(file);
      unparser=new Unparser(ps,2,-1,78,defUseLabels); //indent by 1, no max indent, 78-col lines

      unparser.emitLine("<html>");
      unparser.emitLine("<body text=\"#000000\" BGCOLOR=\"#FFFFFF\">");
      unparser.emitLine("<h1 align=center>");
      unparser.emitLine("   <img hspace=20 vspace=0 border=0 align=center src=logob.gif> ");
      unparser.emitLine("   WebSim Save "+new Date());
      unparser.emitLine("</h1>");
///**/      unparser.emitLine("<applet archive=\"../classes.jar\"");
      unparser.emitLine("<applet ");
      unparser.emitLine("        codebase=\"../classes\"");
      unparser.emitLine("        code=\"WebSim.class\" width="+
                        bounds().width+" height="+bounds().height+">");
      unparser.emitLine("<param name=sourceText value=\"");
      unparser.emitLine("SavedState");
      unparse(unparser,-1); //language -1 means "1 with enough state to continue where you left off"
      unparser.emitLine("\">");
      unparser.emitLine("<hr>This Java applet requires a Java-aware browser ");
      unparser.emitLine("    such as Netscape 2.0 or higher<hr>");
      unparser.emitLine("</applet>");
      unparser.emitLine("</body>");
      unparser.emitLine("</html>");

    } catch (Throwable ee) { //disk full, etc.
      System.out.println("Error while writing image to disk");
      System.out.println(ee);
      ee.printStackTrace();
    } finally {
      try {
        if (ps!=null)
          ps.flush();
        if (file!=null)
          file.close();
      } catch (Throwable eee) {
         System.out.println(""+eee);
         eee.printStackTrace();
      }
      if (unparser!=null)
        unparser.close();
      resumeAllThreads();
    }
  }//end saveFile

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} // end class WebSim


/////////////////////////////////////
/////////////////////////////////////
/////////////////////////////////////


// window with working close button,
class ClosableFrame extends Frame {
  WebSim websim=null;

  /** remember the WebSim to destroy when this window is closed */
  public ClosableFrame(WebSim w) {
    super();
    websim=w;
  }

  /** close window or pass event to first component */
  public boolean handleEvent(Event e) {
    if (e.id==Event.WINDOW_DESTROY) {
      websim.stop();
      websim.destroy();
      dispose();
      return true; //don't propagate further
    }
    else
      return super.handleEvent(e); //just let the event propagate
  }
}//end ClosableFrame


////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
////////////////////////////////////////////////////////


// Tell user file exists, and save file if user chooses "OK" rather than "CANCEL".
class SaveAllDialog extends Dialog {
  private Button okButton=new Button("OK");
  private Button cancelButton=new Button("CANCEL");
  private Panel buttons=new Panel();
  private Frame unusedFrame=new Frame(); //parent of file dialog
  private WebSim creator=null; //object that created this dialog

  /** create an alert with the given title and message to the user */
  public SaveAllDialog(WebSim c,String msg) {
    super(null,msg,true);
    creator=c;
    setLayout(new BorderLayout());
    buttons.setLayout(new FlowLayout());
    buttons.add(okButton);
    buttons.add(cancelButton);
    add("South",buttons);
    add("Center",new Label(msg,Label.CENTER));
    resize(300,200);
    show();
  }

  /** listen for OK or CANCEL being clicked */
  public boolean handleEvent(Event e) {
    if (e.id==Event.ACTION_EVENT) { //clicking a button is ACTION_EVENT
      if (e.target==okButton) {
        hide();
        dispose();
        creator.saveFile();
      } else if (e.target==cancelButton) {
        hide();
        dispose();
        creator.resumeAllThreads();
      }
      return true;
    }
    return false; //let action other than OK and CANCEL propagate
  }//end handleEvent
}//end class SaveAllDialog
