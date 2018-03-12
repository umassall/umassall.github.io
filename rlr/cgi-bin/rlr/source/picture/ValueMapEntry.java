package picture;
import parse.*;
import expression.*;

/**
  * This parses a value map entry of the form [.5 1.5] which means
  * the piecewise linear function of the value map should map an
  * input of .5 to an output of 1.5;
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 20 April 96
  * @author Leemon Baird
  */
public class ValueMapEntry implements Parsable {
  /** The input number that will be mapped to another number */
  public NumExp x;
  /** The output number to which the input number was mapped*/
  public NumExp y;

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'[' NumExp [','] NumExp ']'//One line of a value map."+
           "The first number maps to the second.  Numbers not on the list "+
           "are linearly interpolated.";
  }
  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit('[');
    u.emitUnparse(x,lang);
    u.emit(',');
    u.emitUnparse(y,lang);
    u.emit(']');
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('[',true);
    x=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(',',false);
    y=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(']',true);
    return this;
  } //end method parse

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} //end class ValueMapEntry
