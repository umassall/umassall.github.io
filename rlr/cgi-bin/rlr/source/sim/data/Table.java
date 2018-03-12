package sim.data;
import parse.*;
import java.util.*;
import matrix.*;
import Random;
import matrix.*;

/** this allows the user to enter a table of input/output vectors
  * to be used in training.  These numbers are entered directly in
  * the string that is parsed.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1b, 15 April 97
  * @author Leemon Baird
  */
public class Table extends Data {
  //version 1.1b 15 April 96 Made minor corrections to unparse routine - Mance Harmon
  //version 1.1 3 Nov 96 Leemon Baird, converted to parseClass("MatrixD"...)
  //version 1.0 26 Jun 96 Leemon Baird
  MatrixD[] inData;
  MatrixD[] outData;

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
    if (in!=null)
      java.lang.System.arraycopy(inData [n].data,0,in ,0, inData[0].size);
    java.lang.System.arraycopy(outData[n].data,0,out,0,outData[0].size);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ( MatrixD MatrixD )* '}'"+
           "//table of input/output vectors for training. "+
           "Each MatrixD is a row vector representing one input or output.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{ ");
    u.indent();
      for (int i=0;i<nPairs;i++) {
        u.emitLine();
        u.emitUnparse(inData[i],lang);
        u.emit(" ");
        u.emitUnparse(outData[i],lang);
      }
    u.emitLine();
    u.emit('}');
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector in =new Vector();
    Vector out=new Vector();
    MatrixD m;
    p.parseChar('{',true);
    inSize=-1;
    outSize=-1;

    while (null!=(m=(MatrixD)p.parseClass("MatrixD",lang,false))) {
      if (inSize<0) //first time through, remember length
        inSize=m.size;
      else if (inSize!=m.size) //ensure each vector same length as first
        p.error("Vector of size "+inSize+", not "+m.size);

      in.addElement(m);
      out.addElement(m=(MatrixD)p.parseClass("MatrixD",lang,true));

      if (outSize<0) //first time through, remember length
        outSize=m.size;
      else if (outSize!=m.size) //ensure each vector same length as first
        p.error("Vector of size "+outSize+", not "+m.size);
    }

    nPairs=out.size();
    inData =new MatrixD[nPairs];
    outData=new MatrixD[nPairs];
    in.copyInto(inData);
    out.copyInto(outData);
    p.parseChar('}',true);
    return this;
  }//end parse
}//end Table
