import watch.*;
import parse.*;
import expression.*;

/** A project window.  This window displays one Project object.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 24 July 96
  * @author Leemon Baird
  */
public class ProjWin implements Parsable {
  /** The Project displayed in this window */
  public Project project;
  /** The height of the font in this window*/
  int fHeight=0;
  /** Should this Project be embedded in the Web page rather than a separate window? */
  boolean embed=false;
  /** The WatchManager used for this Project */
  WatchManager watchManager;
  /** the threadgroup that includes all the WebSim threads, and no others. */
  ThreadGroup webSimThreadGroup;

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "['embed'] ['('IntExp [','] IntExp [','] IntExp [','] IntExp')'] <Project> "+
           "//Window for a single Project. "+
           "(x,y,width,height) for the Project's window (-1=default). "+
           "'embed' means embed in the Web page instead of making it a separate window";
  }

  /** Output a description of this object that can be parsed with parse().
    */
  public void unparse(Unparser u, int lang) {
    if (embed)
      u.emitLine("embed ");
    u.emit('(');
    u.emit(project.x());
    u.emit(',');
    u.emit(project.y());
    u.emit(',');
    u.emit(project.width());
    u.emit(',');
    u.emit(project.height());
    u.emitLine(") ");
    u.emitUnparseWithClassName(project,lang,false);
  } //end method unparse

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    int winX=0,winY=0,winWidth=300,winHeight=100;
    embed=p.parseID("embed",false);
    webSimThreadGroup=Thread.currentThread().getThreadGroup();

    if (p.tChar=='(') {//get window bounds if available
      p.parseChar('(',true);
      winX=((IntExp)p.parseClass("expression.IntExp",lang,true)).val;
      p.parseChar(',',false);
      winY=((IntExp)p.parseClass("expression.IntExp",lang,true)).val;
      p.parseChar(',',false);
      winWidth=((IntExp)p.parseClass("expression.IntExp",lang,true)).val;
      p.parseChar(',',false);
      winHeight=((IntExp)p.parseClass("expression.IntExp",lang,true)).val;
      p.parseChar(')',true);
    }
    project=(Project)p.parseType("Project",lang,true);
    if (p.applet instanceof WebSim)
      project.applet=(WebSim)p.applet; //let it know which Applet created it
    project.setWatchManager();
    project.initialize(0);
    project.createWin(winX,winY,winWidth,winHeight,p.applet,embed);
    project.startThread(); //create a new thread and start it running
    return this;
  } //end method parse

  /** Initialize, either partially or completely.  initialize(0) is
    * called once, after parse() and setWatchManager() has been called
    * on every object.  Higher values for level mean only partial
    * initialization is needed.  For example, for reinforcement learning
    * experiments, level=1 at the start of an experiment, level=2 at the
    * start of a run, and level=3 at the start of a trial.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    project.initialize(level);
  }

  /** Kill all the threads and windows and graphics contexts for
    * this Project and its window.
    */
  public void destroy() {
    if (project!=null)
      project.destroy();
    if (project.window==null) {
      project.applet.remove(project.embedPanel);
    } else {
      project.window.hide();
      project.window.dispose();
    }
  }
} //end ProjWin
