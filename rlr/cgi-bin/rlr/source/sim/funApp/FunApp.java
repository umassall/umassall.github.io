package sim.funApp;
import parse.*;
import matrix.*;
import watch.*;
import pointer.*;

/** All function approximators should inherit from FunApp.
  * A function approximator should store its parameters in the array weights[].
  * The gradient functions are used for various gradient-descent algorithms.
  * The store functions are used for various memory-based learning algorithms.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 31 October 96
  * @author Leemon Baird
  */
public abstract class FunApp implements Parsable, Watchable {
  //Version 1.0 4 June 96: Leemon Baird

  /** clone of the input, should be a vector with first element 1.0 */
  protected MatrixD inVect=null;
  /** clone of the output, should be a vector */
  protected MatrixD outVect=null;
  /** clone of a vector for the adjustable parameters of the function approximator */
  protected MatrixD weights=null;

  /** a vector for the gradient of error with respect to the output */
  protected MatrixD dEdOut=null;
  /** a vector for the gradient of error with respect to the weights */
  protected MatrixD dEdWeights=null;
  /** a vector for the gradient of error with respect to the inputs */
  protected MatrixD dEdIn=null;
  /** the WatchManager that variables here may be registered with*/
  protected WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;
  /* input to the net (watchable) */
  protected PMatrixD pInput  =new PMatrixD(null);
  /* output from the net (watchable) */
  protected PMatrixD pOutput =new PMatrixD(null);
  /* weights in the net (watchable) */
  protected PMatrixD pWeights=new PMatrixD(null);

  /** Register all variables with this WatchManager.
    * Any function approximator overriding this should first
    * call super.setWatchManager() for the important housekeeping done.
    */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
    watchManager.registerVar(wmName+"input",  pInput,  this);
    watchManager.registerVar(wmName+"output", pOutput, this);
    watchManager.registerVar(wmName+"weights",pWeights,this);
  }

  /** Return the variable "name" that was passed into setWatchManager.
    * This name is the prefix of the names of all watchable variables
    * within this object
    */
  public String getName() {
    return wmName;
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Define the MatrixD objects that will be used by evaluate(), findGradients(),
    * and findHessian().  First 6 should be column vectors (n by 1 matrices).
    * The last 3 parameters can be null if the Hessian is never to be calculated.
    * If a function approximator overrides this, it should first call
    * super.setIO() for important housekeeping.
    * @exception MatrixException if inputs are vectors with nonmatching sizes
    */
  public void setIO(MatrixD inVect, MatrixD outVect, MatrixD weights,
                    MatrixD dEdIn,  MatrixD dEdOut,  MatrixD dEdWeights,
                    MatrixD dEdIndIn,  MatrixD dEdOutdOut,  MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    pInput.val     =inVect;
    pOutput.val    =outVect;
    pWeights.val   =weights;
    this.inVect    =(MatrixD)inVect.clone();
    this.outVect   =(MatrixD)outVect.clone();
    this.weights   =(MatrixD)weights.clone();
    this.dEdIn     =(dEdIn     ==null) ? null : (MatrixD)dEdIn.clone();
    this.dEdOut    =(dEdOut    ==null) ? null : (MatrixD)dEdOut.clone();
    this.dEdWeights=(dEdWeights==null) ? null : (MatrixD)dEdWeights.clone();
  }

  /** return the input vector (including one element that's always 1.0)
    * that was set by setIO.
    */
  public final MatrixD getInput() {
    return pInput.val;
  }

  /** return the output vector that was set by setIO */
  public final MatrixD getOutput() {
    return pOutput.val;
  }

  /** return the weight vector that was set by setIO */
  public final MatrixD getWeights() {
    return pWeights.val;
  }

  /** store this new input/output pair as a data point for memory-based learning*/
  public void learn(double[] in, double[] out) {}

  /** Calculate the output for the given input */
  public abstract void evaluate();

  /** Calculate the output and gradient for a given input.
    * This does everything evaluate() does, plus it calculates
    * the gradient of the error with respect to the inputs and
    * weights, dEdx and dEdw,
    * User must set dEdOut before calling.
    */
  public abstract void findGradients();

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw. User
    * User must set dEdOut and dEdOutdOut before calling.
    */
  public abstract void findHessian();

  /** Return # weights needed for nIn inputs (including the first
    * one which is always 1.0), and nOut outputs.
    */
  public abstract int nWeights(int nIn,int nOut);

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


  /** This is called when the object is destroyed. Override it
    * to react to that event (e.g. kill threads, dispose of Graphics)
    */
  public void destroy() {
    if (watchManager!=null)
      watchManager.unregisterWatchable(this); //unregister all my variables
  }

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public abstract Object clone();

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    copy.watchManager=watchManager;
    copy.wmName      =wmName;
    return copy;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
}//end FunApp
