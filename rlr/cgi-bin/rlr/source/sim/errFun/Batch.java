package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import Random;

/** Combine N calls to an error function into a single error number.  This allows
  * something like epoch-wise batch learning on infinite training sets.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.02, 23 July 97
  * @author Leemon Baird
  */
public class Batch extends ErrFun {
  //Version 1.02 Changed interface to match ErrFun - MH
  //Version 1.01 Added evaluateAgain() - Mance Harmon
  //Version 1.0 15 Apr 97 simple batch implementation

  /** the error function to query multiple times */
  protected ErrFun errFun=null;

  /** the number of times to call the error function */
  protected IntExp batchSize=new IntExp(1);

  /** the gradient from a single call to errFun */
  protected MatrixD errFunGradient=null;

  /** the average of the gradients from all batchSize calls to errFun */
  protected MatrixD gradient=null;

  /** which of the batch elements is currently being processed.  Counts
    * repeatedly from 0 to N-1 than back to 0 for batches of N elements.
    */
  protected PInt batchIndex=new PInt(0);

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    errFun.setWatchManager(wm,name+"err/");
    wm.registerVar(name+"batch counter",batchIndex,this);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' 'batchSize' IntExp <sim.errFun.ErrFun> '}'" +
           "//Call the ErrFun this many times and averages the errors into "+
           "a single error.  This allows approximate epoch-wise batch "+
           "learning on an infinite data set.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      u.emitLine();
      u.emit("batchSize ");
      u.emitUnparse(batchSize,lang);
      u.emitLine();
      u.emitUnparseWithClassName(errFun,lang,false);
    u.unindent();
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    p.parseID("batchSize",true);
    batchSize=(IntExp)p.parseClass("IntExp",lang,true);
    errFun   =(ErrFun)p.parseType("sim.errFun.ErrFun",lang,true);
    p.parseChar('}',true);

    return this;
  }//end parse

  // the following are called by the gradient descent algorithm.
  // f(x) is the mean of the errors from errFun over batchSize
  // calls to it.

  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    try {
      double err=0;
      gradient.mult(0);
      long updateCount=0;
      for (batchIndex.val=0;batchIndex.val<batchSize.val;batchIndex.val++) {
        err+=errFun.evaluate(rnd,true,false,false);
        if (willFindDeriv) {
            errFun.findGradient();
            gradient.addMult(1./batchSize.val,errFunGradient);
        }
        if (willFindHess) {} //this code needs to be implemented /**/

        if (updateCount++>10/**/) { //update every 10 steps, just to be polite and yield to other threads
          updateCount=0;
          watchManager.update();
        }
      }
      return err/batchSize.val;
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
    errFunGradient=errFun.getGradient();
    gradient=errFunGradient.duplicate();
    return gradient;
  }

  /** The input x sent to the function f(x) (a column vector)*/
  public MatrixD getInput() {
    return errFun.getInput(); //pass my weights on through to the error function
  }


  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    errFun.initialize(level);
  }
}//end class Batch

