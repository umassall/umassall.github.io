package expression;
import parse.*;
import pointer.*;
import java.util.Vector;

/** A numeric expression.  This is used to allow expressions
  * such as 2.3*(sin(3.0)/5-'time') to appear in WebSim HTML files,
  * where 'time' is the name of a watchable variable.
  * This class extends PDouble, so it can be used as a pointer, too.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 3 Nov 96
  * @author Leemon Baird
  */
public class NumVarExp extends PDouble implements Expression, Parsable {
  /** the original starting value is stored here */
  public double   origVal =0;      //original value calculated during parsing
  /** if this is an integer expression, the original starting value is stored here too */
  public  int     intVal  =0;      //current value if an integer expresssion
  private boolean isInt   =false;  //expression is of type int rather than double?
  private boolean isConst =false;  //expression is constant with no variables?

  private final int ADD=0, //addition    +
                    SUB=1; //subtraction -
  private int        ops    []=null; //the kind of operation being done (ADD/SUB)
  private NumVarTerm factors[]=null; //the factors being multiplied or divided

  /** Constructor inits to zero */
  public NumVarExp() {
    origVal=val=0;
  }

  /** Constructor gives initial value */
  public NumVarExp(double v) {
    origVal=val=v;
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "NumVarTerm (('+' | '-') NumVarTerm )* "+
           "//A numeric expression. Can contain variables and combinations "+
           "of the operators +-*/% and 14 predefined functions, "+
           "So it might be 3.0/sin(1+'time') if 'time' is a "+
           "watchable variable name.  The functions sin(x), cos(x), tan(x), "+
           "asin(x), acos(x), atan(x) are all in radians. "+
           "The functions floor(x), ceil(x), mod(n,m), and n%m return ints. "+
           "The functions sqrt(x), ln(x), log10(x), exp(x), and "+
           "power(x,y) return doubles.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(val);
    u.emit(" ");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    NumVarTerm fact=null;
    Vector     vFact=new Vector();
    Vector     vOp  =new Vector();

    fact=(NumVarTerm)p.parseClass("expression.NumVarTerm",lang,true);
    vFact.addElement(fact);
    val    =fact.val;
    intVal =fact.intVal;
    isInt  =fact.isInt();
    isConst=fact.isConstant();
    while (true) {
      if (p.parseChar('+',false)) {
        vOp.addElement(new Integer(ADD));
        fact=(NumVarTerm)p.parseClass("expression.NumVarTerm",lang,true);
        vFact.addElement(fact);
        isInt  =isInt   && fact.isInt();
        isConst=isConst && fact.isConstant();
        if (isInt)
          val=intVal=intVal + fact.intVal;
        else
          val+=fact.val;
      } else if (p.parseChar('-',false)) {
        vOp.addElement(new Integer(SUB));
        fact=(NumVarTerm)p.parseClass("expression.NumVarTerm",lang,true);
        vFact.addElement(fact);
        isInt  =isInt   && fact.isInt();
        isConst=isConst && fact.isConstant();
        if (isInt)
          val=intVal=intVal - fact.intVal;
        else
          val-=fact.val;
      } else
        break;
    }

    if (isInt)
      origVal=val;
    else
      origVal=intVal;

    ops    =new int       [vOp.size()];
    factors=new NumVarTerm[vFact.size()];

    vFact.copyInto(factors);
    for (int i=0;i<ops.length;i++)
      ops[i]=((Integer)vOp.elementAt(i)).intValue();

    return this;
  } //end method parse

  /** Is this a simple constant like 3, 3.1, -.5 rather than an expression? */
  public final boolean isSimple() {
    return true;
  }

  /** Is this a constant with no variables? */
  public final boolean isConstant() {
    return true;
  }

  /** Is this an expression of type integer rather than double */
  public final boolean isInt() {
    return isInt;
  }
}//end class NumVarExp
