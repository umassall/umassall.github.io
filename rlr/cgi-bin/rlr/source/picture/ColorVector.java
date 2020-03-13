package picture;
import parse.*;
import expression.*;

/** This parses a color vector of the form <1,1,1,1,1> with 3-5 elements,
  * each of which is a real number from 0 to 1, with optional commas
  * between them. The elements are red, green, blue, filter, transparency.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class ColorVector implements Parsable {
  /** The color represented by the vector; missing elements are zero. */
  public Colors color;

  private NumExp red,green,blue,filter,transparency; //the values read in

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'<' NumExp [','] NumExp [','] NumExp" +
           " [ [','] NumExp [','] NumExp ] '>'"+
           "//A single color. "+
           "Elements are red, green, blue, filter, transparency, "+
           "each of which range from 0 to 1.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("<");
    u.emitUnparse(red,lang);
    u.emit(",");
    u.emitUnparse(green,lang);
    u.emit(",");
    u.emitUnparse(blue,lang);
    if (filter!=null) {
      u.emit(",");
      u.emitUnparse(filter,lang);
      u.emit(",");
      u.emitUnparse(transparency,lang);
    }
    u.emit(">");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    color=new Colors();
    color.filter=color.trans=0;
    p.parseChar('<',true);
    red=(NumExp)p.parseClass("NumExp",lang,true);
    color.red=(byte)(255*red.val-128);
    p.parseChar(',',false);
    green=(NumExp)p.parseClass("NumExp",lang,true);
    color.green=(byte)(255*green.val-128);
    p.parseChar(',',false);
    blue=(NumExp)p.parseClass("NumExp",lang,true);
    color.blue=(byte)(255*blue.val-128);
    filter=transparency=null;
    if (p.tChar!='>') {
      p.parseChar(',',false);
      filter=(NumExp)p.parseClass("NumExp",lang,true);
      color.filter=(byte)(255*filter.val-128);
      p.parseChar(',',false);
      transparency=(NumExp)p.parseClass("NumExp",lang,true);
      color.trans=(byte)(255*transparency.val-128);
    }
    p.parseChar('>',true);
    return this;
  } //end method parse

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} //end class ColorVector
