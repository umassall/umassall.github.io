package sim.data;
import parse.*;
import java.util.*;
import matrix.*;
import Random;
import matrix.*;
import expression.*;
import watch.*;

/** Generates data around a spiral within the square [-1,1]^2.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 20 May 97
  * @author Leemon Baird
  */
public class SpiralData extends Data {
  final static double pi=3.1415926;
  private NumExp numSpirals=new NumExp(3);

  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    double r=rnd.nextDouble();
    double theta=(1-r)*numSpirals.val*2*pi;
    out[0]=r*Math.sin(theta);
    out[1]=r*Math.cos(theta);
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
    return "'{' 'numSpirals' NumExp'}'"+
           "//Random points around a spiral within the [-1,1] square. ";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{ numSpirals ");
    u.emitUnparse(numSpirals,lang);
    u.emit('}');
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    p.parseID("numSpirals",true);
    numSpirals=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar('}',true);
    inSize=0;
    outSize=2;
    return this;
  }//end parse

  /** Remember the WatchManager for this object and create the window */
  public void setWatchManager(WatchManager wm,String name) {
    wm.registerVar(name+"numSpirals", numSpirals, this);
    super.setWatchManager(wm,name);
  }
}//end SpiralData
