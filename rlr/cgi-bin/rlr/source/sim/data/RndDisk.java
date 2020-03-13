package sim.data;
import parse.*;
import Random;

/** This Data generates random 2D vectors uniformly inside a small circle.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 16 Sep 96
  * @author Leemon Baird
  */
public class RndDisk extends Data {
  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    while (true) {
      out[0]=rnd.nextDouble()*2-1;
      out[1]=rnd.nextDouble()*2-1;
      if (out[0]*out[0]+out[1]*out[1]<=1)
        break;
    }
    out[0]=out[0]/40+.5;
    out[1]=out[1]/40+.5;
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
    return "//Points in a circle. "+
           "Random 2D vectors inside of a .5-radius circle at (.5,.5)";
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
    nPairs =-1;
    return this;
  }
}//end class RndDisk
