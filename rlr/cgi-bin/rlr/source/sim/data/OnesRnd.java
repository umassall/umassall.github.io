package sim.data;
import parse.*;
import java.util.*;
import matrix.*;
import Random;
import matrix.*;
import expression.*;
import watch.*;

/** Generate an N-element vector, where the first K elements are 1.0,
  * and the rest of the elements are uniform, random in the range [0.0,1.0].
  * K is chosen uniformly from [0,N].
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 21 May 97
  * @author Leemon Baird
  */
public class OnesRnd extends Data {
  private IntExp numOutputs=new IntExp(20);

  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    int k=rnd.nextInt(0,numOutputs.val);
    for (int i=0;i<k;i++)
      out[i]=1.0;
    for (int i=k;i<numOutputs.val;i++)
      out[i]=rnd.nextDouble();
  }

  /** Put the nth input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * An exception is raised if n<0 or n>=nPairs.
    * If number of pairs is infinite, then exception is always thrown.
    * @exception ArrayIndexOutOfBoundsException arrays were too small or there is no "nth" data item
    */
  public void getData(int n,double[] in,double[] out) /*throws ArrayIndexOutOfBoundsException*/ {
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' 'numOutputs' IntExp'}'"+
           "//Random vector of ones and random numbers.  The output "+
           "vector has the given number of elements, the first K of which "+
           "are 1.0, and the rest are random in the range [0,1].  K is random "+
           "in the range [0,N], where N is the number of elements in the "+
           "output vector.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{ numOutputs ");
    u.emitUnparse(numOutputs,lang);
    u.emit('}');
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    p.parseID("numOutputs",true);
    numOutputs=(IntExp)p.parseClass("IntExp",lang,true);
    p.parseChar('}',true);
    inSize=0;
    outSize=numOutputs.val;
    return this;
  }//end parse

  /** Remember the WatchManager for this object and create the window */
  public void setWatchManager(WatchManager wm,String name) {
    wm.registerVar(name+"numOutputs", numOutputs, this);
    super.setWatchManager(wm,name);
  }
}//end OnesRnd
