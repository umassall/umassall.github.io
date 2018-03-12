package sim.data;
import parse.*;
import Random;

/** This is the 2-input XOR function
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 24 June 96
  * @author Leemon Baird
  */
public class XOR extends Data {
  double[][] inData={{1,0,0},
                    {1,0,1},
                    {1,1,0},
                    {1,1,1}};
  double[][] outData={{0},{1},{1},{0}};

  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    getData(rnd.nextInt(0,nPairs-1),in,out);
  }

  /** Put the nth input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * An exception is raised if n<0 or n>=nPairs.
    * If number of pairs is infinite, then exception is always thrown.
    * @exception ArrayIndexOutOfBoundsException arrays were too small or there is no "nth" data item
    */
  public void getData(int n,double[] in,double[] out) throws ArrayIndexOutOfBoundsException {
    java.lang.System.arraycopy(inData [n],0,in ,0, inData[0].length);
    java.lang.System.arraycopy(outData[n],0,out,0,outData[0].length);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//2 input 1 output XOR data with bias.";
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
    inSize =inData[0].length;
    outSize=outData[0].length;
    nPairs =outData.length;
    return this;
  }
}
