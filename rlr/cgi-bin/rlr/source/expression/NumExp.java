package expression;
import parse.*;
import pointer.*;

/** An numeric expression.  This is used to allow expressions
  * such as 2.3*(-3+sin(17)) to appear in WebSim HTML files.
  * This class extends PDouble, so it can be used as a pointer, too.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 2.0, 10 May 97
  * @author Leemon Baird
  */
public class NumExp extends PDouble implements Parsable {
  private NumVarExp numVarExp=null; //the general expression that is then checked for being constant
  private double origVal=0; //if numVarExp==null, is the original value calculated during parsing
  private boolean isInt=false; //is the expression of type int rather than double

  /** Constructor inits to zero */
  public NumExp() {
    origVal=val=0;
  }

  /** Constructor gives initial value */
  public NumExp(double v) {
    origVal=val=v;
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "NumVarExp"+
           "//A constant numeric expression. "+
           "The expression must not contain any variables. "+
           "Division is as in C, so 5.0/2 is 2.5 but 5/2 is 2";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (numVarExp==null) {
      u.emit(val);
      u.emit(" ");
    } else
      u.emitUnparse(numVarExp,lang);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    numVarExp=(NumVarExp)p.parseClass("expression.NumVarExp",lang,true);
    isInt=numVarExp.isInt();
    if (!numVarExp.isConstant())
      p.error("Constant numeric expression ");
    origVal=val=numVarExp.val;
    if (numVarExp.isSimple())
      numVarExp=null; //no need to keep around a whole parse tree for a single number
    return this;
  } //end parse

  /** Is this a simple constant like -5 rather than an expression? */
  public final boolean isSimple() {
    return (numVarExp==null);
  }

  /** Is this a constant with no variables? */
  public final boolean isConstant() {
    return true;
  }

  /** Is this an expression of type integer rather than double */
  public final boolean isInt() {
    return isInt;
  }
}//end NumExp
