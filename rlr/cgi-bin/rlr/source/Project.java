import parse.*;
import watch.*;
import java.awt.*;

/** This defines a project.  The WebSim applet parses a string
  * consisting of descriptions of one or more Projects.  They are
  * created, the first is embedded in the Web page, and the others
  * are placed in separate windows.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 24 July 96
  * @author Leemon Baird
  */
public abstract class Project extends GWin implements Runnable, Parsable, Watchable {
  /** the Applet that created this Project. This is set by ProjWin. */
  public WebSim applet;

  /** This ensures each project's thread's name has a unique number */
  protected static int projNum=0;
  /** The thread that runs this Project's run() method */
  protected Thread myThread=null;
  /** keeps track of all variables being watched, and all displays watching them*/
  protected WatchManager watchManager=new WatchManager();
  /** the prefix of the name of every watchable variable in this project */
  protected String wmName="";

  /** constructor ensures that the background is white */
  public Project() {
    super();
    setBackground(Color.white);
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "";
  }//end BNF

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
  }//end unparse

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  } //end parse


  /** Start the project running.  This is called after parse().
    * Make it do something if the project should run in a separate thread.
    * Otherwise, just put code in functions like paint() or mouseDown(),
    * and then the project will not get a separate thread.
    */
  public abstract void run();

  /** Create a new thread and start it running this.run() */
  public void startThread() {
    projNum++;
    if (myThread!=null)
      myThread.stop();
    myThread=new Thread(applet.myThreadGroup,
                        this,    //this implements Runnable
                        "Project"+projNum+
                        "("+this.getClass().getName()+")");
    myThread.start();
  }

  /** When the window is closed or the applet dies,
    * this is called to kill the Project's thread
    */
  public void destroy() {
    if (myThread!=null)
      myThread.stop();
    super.destroy();
  }

  /** Override this to give a GWin menus.  Menus are only visible
    * when it's a separate window, not when embedded in the
    * Web page.  The menus should be added to mb.  Make sure that
    * any child overriding addMenus makes a call to super.addMenus(ab) too.
    */
  public void addMenus(MenuBar mb) {
    Menu menu=new Menu("WebSim");
    menu.add("Save all");
    menu.add("Pause");
    menu.add("Resume");
    menu.add("Plots off");
    menu.add("Plots on");
    menu.add("-------");
    menu.add("RESTART");
    mb.add(menu);
  } //end addMenus

  /** respond to the menu choices */
  public boolean action(Event e,Object w) {
    if (((String)w).equals("Pause" ))
      applet.suspendAllThreads();
    else if (((String)w).equals("Resume" ))
      applet.resumeAllThreads();
    else if (((String)w).equals("Save all" ))
      applet.unparseAll();
    else if (((String)w).equals("Plots off" ))
      Display.disableDisplays=true;
    else if (((String)w).equals("Plots on" ))
      Display.disableDisplays=false;
    else if (((String)w).equals("RESTART" )) {
      applet.restartNumber++; //each Project checks this occasionally and will restart
    }
    return super.action(e,w);
  }

//The following are needed for the project to be Watchable,

  /** Register all variables with this WatchManager.
    * This method should register all the variables in this object and
    * in those it links to.  The name of each variable should be
    * appended to the end of the String name.
    */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
  }

  /** Set the watchManager and name to be the default values for this Project */
  public void setWatchManager() {
    setWatchManager(watchManager,wmName);
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} // end class Project
