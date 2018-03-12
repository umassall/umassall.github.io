package sim.funApp;
import sim.funApp.FunApp;
import parse.*;
import matrix.*;
import expression.*;
import java.util.*;
import pointer.*;

public class InterferenceFunction extends FunApp {
private MatrixD input;
private MatrixD output;
protected FunApp function=null;
protected FunApp origFunction=null;
/** other gradient (x) */
MatrixD dEdW_x=null;
/** other gradient (x') */
MatrixD dEdW_xp=null;

  /** Define the MatrixD objects that will be used by evaluate()
    * and findGradients().  All 9 should be column vectors (n by 1 matrices).
    * All the MatrixD objects are copied, but the pointers still point
    * to the same data arrays.
    * @exception MatrixException if vector shapes don't match
    */

  public void setIO(MatrixD inVect, MatrixD outVect, MatrixD weights,
                    MatrixD dEdIn,  MatrixD dEdOut,  MatrixD dEdWeights,
                    MatrixD dEdIndIn, MatrixD dEdOutdOut, MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    super.setIO(inVect, outVect, weights, dEdIn, dEdOut, dEdWeights, null, null, null);
    input     = (MatrixD)inVect.clone();
    output    = (MatrixD)outVect.clone();
    dEdW_x    = new MatrixD(weights.size);
    dEdW_xp   = new MatrixD(weights.size);

  }//end setIO

  /** calculate the output for the given input.  Also calculates Ap and App because
  * it is appropriate to do so eventhough they are not used until backward pass */
  public void evaluate() {
    try {
        function.inVect.replace(input.submatrix(0,1,1));   // fill correct input value into Net's inVect
        function.dEdOut.mult(0);       // want gradient and hessian with respect to
        function.dEdOut.add(1);        // output not some cost function (cf).  Therefore
        function.findGradients();
        dEdW_x.replace(function.dEdWeights);

        function.inVect.replace(input.submatrix(1,1,1));   // fill correct input value into Net's inVect
        function.dEdOut.mult(0);       // want gradient and hessian with respect to
        function.dEdOut.add(1);        // output not some cost function (cf).  Therefore
        function.findGradients();
        dEdW_xp.replace(function.dEdWeights);
        double thing=(dEdW_x.dot(dEdW_xp))/(dEdW_x.dot(dEdW_x));
//        System.out.println("  dEdW_x               "+dEdW_x);
//        System.out.println("  (dEdW_x.dot(dEdW_x)) "+(dEdW_x.dot(dEdW_x)));
//    System.out.println("I("+inVect.val(0)+","+inVect.val(1)+") = "+thing);
        output.set(0,thing);
    }  // end try
    catch (MatrixException e) {
        e.print();
    }
  }  //end evaluate

  /** Calculate the output and gradient for a given input.
    * This does everything evaluate() does, plus it calculates
    * the gradient of the error with respect to the inputs and
    * weights, dEdx and dEdw, User must set dEdOut before calling.
    * User must set dEdOut before calling.
    */
  public void findGradients() {
//  try {
  evaluate();
//      } catch (MatrixException e) {
//          e.print();
//      }
  }

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw.
    * User must set dEdOut and dEdOutdOut before calling.
    */
  public void findHessian() {
//  try {
  evaluate();
//      } catch (MatrixException e) {
//          e.print();
//      }
  }

  public int nWeights(int nIn,int nOut) {return function.nWeights(nIn,nOut);};

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ";
    }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {

    p.parseChar('{',true);
    while (true) { //parse whatever parameters are there
      if (p.parseID("funApp",false)) {
        origFunction=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
        function=(FunApp)origFunction.clone();
      } else break;
    }
    p.parseChar('}',true);
    try {
        setIO(new MatrixD(2), new MatrixD(1), origFunction.getWeights(),
//              new MatrixD(2), new MatrixD(1), new MatrixD(origFunction.nWeights()),
              null,null,null,
              null,null,null);
      //setIO(holds x and x', holds I(x,x') ,  the weights             , null,null,null, null,null,null);
    }
    catch (MatrixException e) {
        e.print();
    }
    return this;
  } //end parse

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public Object clone() {
    return cloneVars(new InterferenceFunction());
  }

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    InterferenceFunction c=(InterferenceFunction)copy;
    c.function=(FunApp)function.clone();
    try {
        c.function.setIO(origFunction.getInput(),origFunction.getOutput(),origFunction.getWeights(),
                         new MatrixD(origFunction.inVect .size),
                         new MatrixD(origFunction.outVect.size),
                         new MatrixD(origFunction.weights.size),
                         null,null,null);
        // communicate input, outputs, and weights for funApp that is internal to this.
    }  // end try
    catch (MatrixException e) {
        e.print();
    }
    return super.cloneVars(copy);
  }
} //end class Net

