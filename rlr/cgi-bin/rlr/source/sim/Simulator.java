package sim;
import java.awt.*;
import parse.*;
import watch.*;
import Project;
import pointer.*;
import DisplayList;
import sim.funApp.*;
import matrix.*;
import Logo;

/** Run a simulation, particularly involving neural nets
  * and reinforcement learning.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1, 18 July 97
  * @author Leemon Baird
  */
public class Simulator extends Project {
  //version 1.1, 18 July 97
  //version 1.0, 25 June 96
  protected Experiment[] experiment=new Experiment[1]; // experiment.run() actually performs the simulation
  protected DisplayList  displays  =new DisplayList(); // all the display windows that watch this simulation

  private Object[][] parameters=
    {{"run a simulation. "+
      "As the given experiment runs, the given displays show "+
      "how the variables change"},
     {"experiment",experiment,"runs the top-level, main loop of a simulation",
      "displays",  displays,  "list of windows and embedded frames"},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** paint the main window for the simulator */
  public void paint(Graphics g) {
    Logo.credits(g,5,5);
  }

  /** Register all variables with this WatchManager.
    * This method should register all the variables in this object and
    * in those it links to.  The name of each variable should be
    * appended to the end of the String name.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    experiment[0].setWatchManager(watchManager,wmName);
    displays.setWatchManager(watchManager,wmName+"disp/");
  }

  /** Start the project running, after all parsing is done.
    * This happens in a separate thread.
    */
  public void run() {
    experiment[0].run(); //run the experiment, returns when completely done
  } //end method run

  /** when Simulator is destroyed, so is every Display */
  public void destroy() {
    displays.destroy();
    super.destroy();
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    experiment[0].initialize(level);
    displays.initialize(level);
  }
} // end class Simulator
