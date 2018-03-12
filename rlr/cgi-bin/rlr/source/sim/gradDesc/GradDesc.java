package sim.gradDesc;
import matrix.*;
import parse.*;
import watch.*;
import sim.errFun.*;
import sim.*;

/** This object performs some form of gradient descent.
  * The run() method repeatedly
  * changes x until a local minimum of f(x) is reached.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 25 June 96
  * @author Leemon Baird
  */
public abstract class GradDesc extends Experiment {
  /** the watchManager that watches these variables*/
  protected  WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;
  /** the function to minimize */
  ErrFun errFun=null;

  /** repeatedly change x until f(x) reaches a local minimum */
  public abstract void run();

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    */
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
  public String BNF(int lang) {
    return "";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {}

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  }
}
