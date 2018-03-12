package sim.display;
import Display;
import watch.*;
import pointer.*;
import java.awt.*;
import java.util.Vector;
import parse.*;
import sim.funApp.*;
import matrix.*;
import expression.*;
import fix.*;

/** Classes inheriting from Plot represent one layer in a Graph2D display,
  * or one layer on the bottom of the cube in a Graph3D display.
  *    <p>This code is (c) 1996,1997 Ansgar Laubsch and Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0 15 May 97
  * @author Leemon Baird
  */
public abstract class Plot implements Parsable, Watchable, Watcher {
  /** the WatchManager that this class registers with */
  protected WatchManager watchManager=null;

  /** the prefix for all registered variables */
  protected String wmName=null;

  /** the Display that this plot will be drawn on */
  public Display parentDisplay=null;

  /** Record any data being watched, but don't redraw the screen.
    * This is called whenever a trigger variable is changed for the Nth time.
    * If not overridden, it assumes a parameter was changed, and just
    * redraws the screen.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    parentDisplay.repaint();
  }

  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    wmName=name;
    watchManager=wm;
    wm.registerParameters(this,name);
  }

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Get the WatchManager being used */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** One of the watched variables has been unregistered.
    */
  public void unregister(String watchedVar) {
    update(null,null,null); //redraw the new screen
  } //end unregister

  /** Put preferred autoscaling bounds into the variables pointed to by these
    * four pointers.  Change the variables only if the bounds should be
    * expanded to be larger than what the variables already say.
    */
  public void autoscaleBounds(PDouble xMin, PDouble xMax,
                              PDouble yMin, PDouble yMax) {
  }

  /** Clean up threads, Graphics contexts, etc., and let owned objects do the same */
  public void destroy() {
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "";
  }

  /** Output a description of this object that can be parsed with parse().*/
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  }

  /** Draw the plot.
    * The region of mathematical space to draw is (xMin,yMin)-(xMax,yMax), where
    * (xMin,yMin) is plotted at screen coordinates (startX,startY), and
    * (xMax,yMin) is plotted at screen coordinates (xAxisX,xAxisY), and
    * (xMin,yMax) is plotted at screen coordinates (yAxisX,yAxisY).
    */
  public abstract void drawAll(Graphics g, double xMin,   double xMax,
                                  double yMin,   double yMax,
                                  int    startX, int    startY,
                                  int    xAxisX, int    xAxisY,
                                  int    yAxisX, int    yAxisY);

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} // end of class Plot
