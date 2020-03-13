package sim.data;
import parse.*;
import Random;

/** This Data generates random uniform 2D vectors inside the unit square.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 16 Sep 96
  * @author Leemon Baird
  */
public class RndUniformSquare extends Data {
  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    out[0]=rnd.nextDouble()-.5;  //double in range [-.5,.5]
    out[1]=rnd.nextDouble()-.5;
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
    return "//Points in a square. "+
           "Random 2D vectors uniformly in the unit square (0,0)-(1,1).";
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
    inSize =0;  //these are just random outputs, not input/output pairs
    outSize=2;
    nPairs =-1; //infinite number of possible outputs
    return this;
  }
}//end class RndUniformSquare
