package sim.data;
import watch.*;
import parse.*;
import Random;

/** a Data object represents a set of pairs of input/output vectors,
  * suitable for use with supervised learning.  It might contain an
  * array of numbers, a function that generates them randomly, or a
  * function that reads them from a file.  All data objects should
  * extend Data.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 24 June 96
  * @author Leemon Baird
  */
public abstract class Data implements Watchable, Parsable {
  /** number of elements in the input vector (including first which is 1.0)*/
  protected int inSize=0;
  /** number of elements in the output vector */
  protected int outSize=0;
  /** number of input/output pairs (-1 if infinite) */
  protected int nPairs=0;
  /** the WatchManager that variables here may be registered with*/
  protected WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;

  /** Register all variables with this WatchManager. */
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

  /** Return the number of elements in the input vector.
    * This includes the first element which is always a 1.0
    * (for Data objects that do that).
    */
  public int inSize() {return inSize;}

  /** return the number of elements in the output vector */
  public int outSize() {return outSize;}

  /** return the number of input/output pairs (-1 if infinite) */
  public int nPairs() {return nPairs;}

  /** Close source files, etc.*/
  public void stop() {}

  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * in[0] will always be set to 1.0.
    * A randomly-chosen data point will be returned.
    */
  public abstract void getData(double[] in, double[] out, Random rnd);

  /** Put the nth input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * in[0] will always be set to 1.0.
    * An exception is raised if n<0 or n>=nPairs.
    * If number of pairs is infinite, then exception is always thrown.
    * @exception ArrayIndexOutOfBoundsException arrays were too small or there is no "nth" data item
    */
  public abstract void getData(int n,double[] in,double[] out) throws ArrayIndexOutOfBoundsException;

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

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
}//end Data
