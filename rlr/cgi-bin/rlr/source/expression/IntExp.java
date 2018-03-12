package expression;
import parse.*;
import pointer.*;

/** A constant integer expression.  This is used to allow expressions
  * such as 2*(-floor(3.1)+17/2) to appear in WebSim HTML files.
  * This class extends PInt, so it can be used as a pointer, too.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 2.0, 10 May 97
  * @author Leemon Baird
  */
public class IntExp extends PInt {
  private NumVarExp numVarExp=null; //the general expression that is then checked for being a proper IntExp
  private int origVal=0; //if numVarExp==null, is the original value calculated during parsing

  /** Constructor inits to zero */
  public IntExp() {
    origVal=val=0;
  }

  /** Constructor gives initial value */
  public IntExp(int v) {
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
           "//A constant integer expression. "+
           "The expression must not have a noninteger value or contain variables. "+
           "Expressions like 3/floor(2.7) are integer expressions because "+
           "floor() and ceil() always return integers, and division of two "+
           "integers returns an integer.";
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
    if (!numVarExp.isInt())
      p.error("Constant integer (not double) expression ");
    else if (!numVarExp.isConstant())
      p.error("Constant (not variable) integer expression ");
    origVal=val=numVarExp.intVal;
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
    return true;
  }
}//end IntExp
