package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import java.util.*;
import Random;

/** Average together the outputs of multiple error functions to form a single
  * error function.  This allows gradient descent on several different
  * error functions simultaneously.  This class assumes that all the individual
  * error functions have the same number of inputs (e.g. neural
  * networks with the same number of weights).
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.02, 23 July 97
  * @author Leemon Baird
  */
public class ErrAvg extends ErrFun {
  // 1.02 changed interface to match ErrFun - MH
  // 1.01 added evaluateAgain()

  //Version 10 15 Apr 97 simple ErrAvg implementation.
  //                     Right now this code is a little
  //                     inefficient since it recopies the first input vector
  //                     into all the others on each time step.


  /** each of the error functions to average */
  protected ErrFun[] errFun=null;

  /** the gradient from each individual errFun */
  protected MatrixD[] errFunGradient=null;

  /** the weights for each errFun */
  protected MatrixD[] weights=null;

  /** the average of the gradients from all batchSize calls to errFun */
  protected MatrixD gradient=null;

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    for (int i=0;i<errFun.length;i++)
      errFun[i].setWatchManager(wm,name+"err"+i+"/");
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' <sim.errFun.ErrFun> * '}'" +
           "//Call all these error functions and average their errors."+
           "This allows gradient descent to satisfy several error functions "+
           "simultaneously.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      u.emitLine();
      for (int i=0;i<errFun.length;i++) {
        u.emitUnparseWithClassName(errFun[i],lang,false);
      }
    u.unindent();
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    Vector v=p.parseTypeList("sim.errFun.ErrFun",lang,true);
    errFun=new ErrFun[v.size()];
    v.copyInto(errFun);
    p.parseChar('}',true);
    return this;
  }//end parse

  // the following are called by the gradient descent algorithm.
  // f(x) is the mean of the errors from errSum over batchSize
  // calls to it.

  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    try {
      double err=0;
      gradient.mult(0);
      for (int i=0;i<errFun.length;i++) {
        weights[i].replace(weights[0]); //all weight vectors must be same size.
        err+=errFun[i].evaluate(rnd,true,false,false);
        errFun[i].findGradient();
        gradient.addMult(1./errFun.length,errFunGradient[i]);
      }
      return err/errFun.length;
    } catch (MatrixException e) {
      e.print();
    }
    return 0;
  }//end evaluate

  /** update the gradient vector based on the current fInput vector.
    * Assumes that evaluate() was already called on this vector.
    */
  public void findGradient() {  //gradient is updated in evaluate()
  } //end findGradient

  /** The gradient of f(x) with respect to x (a column vector)*/
  public MatrixD getGradient() {
    errFunGradient=new MatrixD[errFun.length];
    weights       =new MatrixD[errFun.length];
    for (int i=0;i<errFun.length;i++) {
      errFunGradient[i]=errFun[i].getGradient();
      weights       [i]=errFun[i].getInput();
    }
    gradient=errFunGradient[0].duplicate();
    return gradient;
  }

  /** The input x sent to the function f(x) (a column vector)*/
  public MatrixD getInput() {
    if (weights==null)
      getGradient(); //calling my own getGradient will calculate the weights
    return weights[0]; //pass my weights on through to the first error function
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    for (int i=0;i<errFun.length;i++)
      errFun[i].initialize(level);
  }
}//end class ErrAvg

