package sim.errFun;
import matrix.*;
import watch.*;
import sim.funApp.*;
import sim.*;
import Random;
import expression.*;
import parse.*;


/** An ErrFun represents an error function to be minimized,
  * such as mean squared output error for a neural net, or
  * mean squared Bellman residual for a reinforcement-learning system,
  * or mean squared interference for localizing.
  * It is a function f(x) that maps vectors to scalars.
  * At a minimum, the evaluate() method should return something.
  * The getGradient() and getHessian() methods may or may not do
  * anything useful, though they may be needed if gradient descent is to
  * be performed on this function.  The evaluate() method should always
  * be called before calling getGradient() or getHessian();
  * The get*() functions return MatrixD objects for the inputs and outputs.
  * These vectors can be read and set to access the current input/output values.
  * By default, this object contains variables and code for dealing with one
  * function approximator, though these can be ignored or overridden if there
  * are zero or two or more function approximators involved.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.3, 21 July 97
  * @author Leemon Baird
  */
public abstract class ErrFun implements Parsable, Watchable {
  //1.3 21 July 97 Added the evaluateAgain() method - Mance Harmon
  //1.2 7 Nov 96 "init" works with multiple outputs in SupervisedLearning
  //1.1 4 Nov 96 new name and package, and changed to inherit from Experiment
  //1.0 25 Jun 96

  /** the function approximator whose weights will be trained */
  protected FunApp function=null;
  /** The input vector to the function approximator */
  protected MatrixD inputs=null;
  /** The output vector from the function approximator */
  protected MatrixD outputs=null;
  /** gradient of mean squared error  wrt inputs*/
  protected MatrixD dEdIn=null;
  /** The correct output that the function approximator learns to give */
  protected MatrixD dEdOut=null;
  /** Second derivative of error wrt input to network wrt input to network */
  protected MatrixD dEdIndIn=null;
  /** Second derivative of error wrt output to network wrt output to network */
  protected MatrixD dEdOutdOut=null;
  /** Second derivative of error wrt weights wrt weights to network */
  protected MatrixD dEdWeightsdWeights=null;
  /** all the weights in the function approximator as a column vector */
  protected MatrixD weights=null;
  /** gradient of mean squared error wrt weights*/
  protected MatrixD dEdWeights=null;
  /** gradient of mean squared error summed for all training examples */
  protected MatrixD dEdWeightsSum=null;
  /** hessian of mean squared error wrt weights */
  protected MatrixD hessian=null;

  /** this experiment and all the objects it contains register vars with watchManager*/
  protected  WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects within the ErrFun. */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
    if (function!=null)
      function.setWatchManager(wm,name+"function/");
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

  /////////////////////////////////////////////////////////////////////

  // the following are called by the gradient descent algorithm.
  // f(x) is the error for a given weight vector x.
  // Depending on the algorithm and settings in the HTML file,
  // this error may be the true error or an unbiased estimate of
  // the true error

  /** The input x sent to the function f(x) (a column vector)*/
  public MatrixD getInput() {
    return weights;
  }

  /** The gradient of f(x) with respect to x (a column vector)*/
  public MatrixD getGradient() {
    return dEdWeights;
  }

  /** The hessian of f(x) with respect to x (a square matrix)*/
  public MatrixD getHessian() { //if hessian is desired, create it
    if (hessian!=null)
      return hessian;
    hessian=new MatrixD(weights.size,weights.size);
    return hessian;
  }

  /** update the fHessian vector based on the current fInput vector */
  public void findHessian() {
   //this may be implemented, or not
  }

  /** return the scalar output for the current fInput vector. */
  public abstract double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHessian,boolean rememberNoise);

  /** update the fGradient vector based on the current fInput vector */
  public abstract void findGradient();

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    if (function!=null)
      function.initialize(level);
  }
}//end ErrFun
