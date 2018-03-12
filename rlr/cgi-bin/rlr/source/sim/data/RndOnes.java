package sim.data;
import parse.*;
import expression.*;
import Random;

/** This Data generates random vectors choosing an integer N uniformly,
  * filling in the first N elements of the vector with ones, and filling
  * in the rest of the vector with uniform real numbers in [0,1].
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 17 Mar 97
  * @author Leemon Baird
  */
public class RndOnes extends Data {
  /** The expression parsed to get the dimensionality of the vectors */
  public IntExp outSizeExp=null;

  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    int i,n=rnd.nextInt(0,outSize);
    for (i=0;i<n;i++)
      out[i]=1;
    for (;i<outSize;i++)
      out[i]=rnd.nextDouble();
  }

  /** Put the nth input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * An exception is raised if n<0 or n>=nPairs.
    * If number of pairs is infinite, then exception is always thrown.
    * @exception ArrayIndexOutOfBoundsException arrays were too small or there is no "nth" data item
    */
  public void getData(int n,double[] in,double[] out) throws ArrayIndexOutOfBoundsException {
    throw new ArrayIndexOutOfBoundsException();
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "IntExp//Create random vectors with this many elements. "+
           "Uniformly chooses N, then fills in first N elements with "+
           "ones, the rest with uniform real numbers in [0,1].";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emitUnparse(outSizeExp,lang);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    outSizeExp = (IntExp)p.parseClass("IntExp",lang,true);
    outSize = outSizeExp.val;
    inSize =0;  //these are just random outputs, not input/output pairs
    nPairs =-1;
    return this;
  }
}//end class RndOnes
