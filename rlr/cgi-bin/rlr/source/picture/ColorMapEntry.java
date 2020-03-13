package picture;
import parse.*;
import expression.*;

/** This parses a color map entry of the form [.5 <1 0 0>] which means
  * the color map should map value .5 to the color red.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class ColorMapEntry implements Parsable {
  /** The number that maps to this color */
  public NumExp level;
  /** The color (red,green,blue,filter,transparency) to which the number level maps */
  public double red,green,blue,filter,trans;
  protected ColorVector cols; //the color vector

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'[' NumExp [','] picture.ColorVector ']'"+
           "//one line of a color map. "+
           "In the linear map, the given number maps to the given color. "+
           "values in between given colors map to linearly-interpolated colors.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit('[');
    u.emitUnparse(level,lang);
    u.emit(',');
    u.emitUnparse(cols,lang);
    u.emit(']');
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('[',true);
    level = (NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(',',false);
    cols  = (ColorVector)p.parseClass("ColorVector",lang,true);
    p.parseChar(']',true);
    red    = ((double)cols.color.red   +128)/255;
    green  = ((double)cols.color.green +128)/255;
    blue   = ((double)cols.color.blue  +128)/255;
    filter = ((double)cols.color.filter+128)/255;
    trans  = ((double)cols.color.trans +128)/255;
    return this;
  } //end method parse

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} //end class ColorMapEntry
