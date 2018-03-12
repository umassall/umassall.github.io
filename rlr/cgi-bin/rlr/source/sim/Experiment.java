package sim;
import parse.*;
import watch.*;
import Random;
import expression.*;

/** All experiments extend this abstract class.
  * This object parses and loads in all the necessary function approximators,
  * data objects, models, optimizing, and learning algorithms to run
  * an experiment.  The Simulator takes care of hooking up the displays
  * and managing the user interface to create new displays.
  * If the Experiment gets its parameters from the file it's parsing,
  * and if it loads in function approximators etc with parseType,
  * then a single Experiment class can run a wide range of experiments.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 25 June 96
  * @author Leemon Baird
  */
public abstract class Experiment implements Parsable, Watchable {
  /** the applet.restartNumber last time it was checked.  If different now, then restart */
  protected int lastRestartNumber=0;
  /** this experiment and all the objects it contains register vars with watchManager*/
  protected  WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;
  /** the random number seed */
  protected IntExp seed=new IntExp(0);
  /** PRNG for initial weights, scrambling data order, and other random choices */
  protected Random rnd=new Random(seed.val);

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment. */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
  }

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public abstract String BNF(int lang);

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public abstract void unparse(Unparser u, int lang);

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public abstract Object parse(Parser p,int lang) throws ParserException;

  /** This runs the simulation.  The function returns when the simulation
    * is completely done.  As the simulation is running, it should call
    * the watchManager.update() function periodically so all the display
    * windows can be updated.
    */
  public abstract void run();

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
}
