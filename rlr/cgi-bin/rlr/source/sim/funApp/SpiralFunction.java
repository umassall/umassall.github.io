package sim.funApp;
import parse.*;
import matrix.*;
import pointer.*;
import watch.*;
import java.util.*;
import expression.*;

 /* A function approximator with one weight, one input, and two outputs. The
  * inputs in the range [0,1] are mapped to a spiral within the [-1,1] square.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0 20 May 97
  * @author Leemon Baird
  */
public class SpiralFunction extends FunApp {
  final double pi=3.1415926;

  /** Define the MatrixD objects that will be used by evaluate()
    * and findGradients().  All 6 should be column vectors (n by 1 matrices).
    * All the MatrixD objects are copied, but the pointers still point
    * to the same data arrays.
    * @exception MatrixException if vector shapes don't match
    */
  public void setIO(MatrixD inVect,   MatrixD outVect,    MatrixD weights,
                    MatrixD dEdIn,    MatrixD dEdOut,     MatrixD dEdWeights,
                    MatrixD dEdIndIn, MatrixD dEdOutdOut, MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    super.setIO(inVect, outVect, weights, dEdIn, dEdOut, dEdWeights,  dEdIndIn, dEdOutdOut, dEdWeightsdWeights);

    if ((dEdIn     !=null && inVect.size !=dEdIn.size) ||
        (dEdOut    !=null && outVect.size!=dEdOut.size) ||
        (dEdWeights!=null && weights.size!=dEdWeights.size))
      throw new MatrixException ("LookupTable.java: vector sizes don't match");

  }//end setIO

  /** calculate the output for the given input */
  public void evaluate() {
    try {
      double x=inVect.val(0);
      double w=weights.val(0);
      double theta=(1-x)*w*2*pi;
      outVect.set(0,x*Math.sin(theta));
      outVect.set(1,x*Math.cos(theta));
    } catch (MatrixException e) {
      e.print();
    }
  }

  /** Calculate the output and gradient for a given input.
    * This does everything evaluate() does, plus it calculates
    * the gradient of the error with respect to the inputs and
    * weights, dEdx and dEdw,
    * User must set dEdOut before calling.
    */
  public void findGradients() {
    try {
      double x=inVect.val(0);
      double w=weights.val(0);
      double theta=(1-x)*w*2*pi;
      dEdWeights.set(0,dEdOut.val(0)*x*( Math.cos(theta))*(1-x)*2*pi+
                       dEdOut.val(1)*x*(-Math.sin(theta))*(1-x)*2*pi);
    } catch (MatrixException e) {
      e.print();
    }
  }

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw.
    */
  public void findHessian(){
    //this method not yet implemented
  }

  /** Return # elements (weights) in the lookup table.
    */
  public int nWeights(int nIn,int nOut) {
    return 1;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//spiral function. "+
           "Maps a single input in the range [0,1] into 2D vectors in the "+
           "[-1,1], [-1,1] square lying on "+
           "a spiral.  The weight W makes the spiral circle W times within "+
           "the square.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  } //parse

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public Object clone() {
    return cloneVars(new SpiralFunction());
  }

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    return super.cloneVars(copy);
  }
} //end SpiralFunction
