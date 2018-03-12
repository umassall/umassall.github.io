package expression;
import parse.*;
import pointer.*;

/** A numeric expression.  This is used to allow expressions
  * such as 2.3*(sin(3.0)/5-'time') to appear in WebSim HTML files,
  * where 'time' is the name of a watchable variable.
  * This class extends PDouble, so it can be used as a pointer, too.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0 10 May 96
  * @author Leemon Baird
  */
public class NumVarFact extends PDouble implements Expression, Parsable {
  /** if this is an integer expression, the original starting value is stored here too */
  public  int     intVal =0;     //current value if an integer expresssion
  private double  origVal=0;     //original value calculated during parsing
  private boolean isInt  =false; //expression is of type int rather than double
  private boolean isConst=false; //is expression constant (no variables)?
  private NumVarExp  arg1=null,arg2=null;  //arguments to a function, if any
  private NumVarFact fact=null;            //arguments to the negation operator
  private double LOG_OF_10=Math.log(10.0); //useful constant for creating log10() function

  private final int INT     =0,  //an integer
                    DOUBLE  =1,  //a double
                    NEGATION=2,  //a minus sign followed by a factor
                    PAREN   =3,  //an expression with parentheses around it

                    FLOOR   =4,  //floor(-3.5)=-4
                    CEIL    =5,  //ceil (-3.5)=-3
                    MOD     =6,  //mod  (-3,10)=7

                    POWER   =7,  //pow(x,y)= x to the y
                    SQRT    =8,  //square root
                    LN      =9,  //natural log
                    LOG10   =10, //log base 10
                    EXP     =11, //exp(x)= e to the x

                    SIN     =12, //sine in radians
                    COS     =13, //cosine in radians
                    TAN     =14, //tangent in radians
                    ASIN    =15, //arc sine in radians
                    ACOS    =16, //arc cosine in radians
                    ATAN    =17; //arc tangent in radians
  private int kind=INT;          //the kind of expression this is.  One of the above



  /** Constructor inits to zero */
  public NumVarFact() {
    val=0;
  }

  /** Constructor gives initial value */
  public NumVarFact(double v) {
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
    return "<integer> | <double> | '-' NumVarFact | "+
           "('(' NumVarExp ')') | "+
           "('floor' '(' NumVarExp ')') | "+
           "('ceil' '(' NumVarExp ')') | "+
           "('mod' '(' NumVarExp ')') | "+
           "('power' '(' NumVarExp ')') | "+
           "('sqrt' '(' NumVarExp ')') | "+
           "('ln' '(' NumVarExp ')') | "+
           "('log10' '(' NumVarExp ')') | "+
           "('exp' '(' NumVarExp ')') | "+
           "('sin' '(' NumVarExp ')') | "+
           "('cos' '(' NumVarExp ')') | "+
           "('tan' '(' NumVarExp ')') | "+
           "('asin' '(' NumVarExp ')') | "+
           "('acos' '(' NumVarExp ')') | "+
           "('atan' '(' NumVarExp ')') | "+
           "//A factor used by NumVarExp.";
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
    if (p.isInt) {
      kind   =INT;
      val    =intVal=p.parseInt___(true);
      isInt  =true;
      isConst=true;
    } else if (p.isDouble) {
      kind   =DOUBLE;
      val    =p.parseDouble___(true);
      isInt  =false;
      isConst=true;
    } else if (p.parseChar('-',false)) {
      kind   =NEGATION;
      fact   =(NumVarFact)p.parseClass("NumVarFact",lang,true);
      val    =-fact.val;
      intVal =-fact.intVal;
      isInt  = fact.isInt();
      isConst= fact.isConstant();
    } else if (p.parseChar('(',false)) {
      kind   =PAREN;
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = arg1.val;
      intVal = arg1.intVal;
      isInt  = arg1.isInt();
      isConst= arg1.isConstant();
    } else if (p.parseID("floor",false)) {
      kind   =FLOOR;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      intVal = (int)Math.floor(arg1.val);
      val    = intVal;
      isInt  = true;
      isConst= arg1.isConstant();
    } else if (p.parseID("ceil",false)) {
      kind   =CEIL;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      intVal = (int)Math.ceil(arg1.val);
      val    = intVal;
      isInt  = true;
      isConst= arg1.isConstant();
    } else if (p.parseID("mod",false)) {
      kind   =CEIL;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      if (!arg1.isInt())
        p.error("Integer expression");
      p.parseChar(',',true);
      arg2   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      if (!arg2.isInt())
        p.error("Integer expression");
      p.parseChar(')',true);
      intVal = ((arg1.intVal % arg2.intVal) + arg2.intVal) % arg2.intVal;
      val    = intVal;
      isInt  = true;
      isConst= arg1.isConstant() && arg2.isConstant();
    } else if (p.parseID("power",false)) {
      kind   =POWER;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(',',true);
      arg2   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.pow(arg1.val,arg2.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant() && arg2.isConstant();
    } else if (p.parseID("sqrt",false)) {
      kind   =SQRT;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.sqrt(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("ln",false)) {
      kind   =LN;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.log(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("log10",false)) {
      kind   =LOG10;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.log(arg1.val)/LOG_OF_10;
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("exp",false)) {
      kind   =EXP;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.exp(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("sin",false)) {
      kind   =SIN;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.sin(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("cos",false)) {
      kind   =COS;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.cos(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("tan",false)) {
      kind   =TAN;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.tan(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("asin",false)) {
      kind   =ASIN;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.asin(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("acos",false)) {
      kind   =ACOS;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.acos(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else if (p.parseID("atan",false)) {
      kind   =ATAN;
      p.parseChar('(',true);
      arg1   =(NumVarExp)p.parseClass("NumVarExp",lang,true);
      p.parseChar(')',true);
      val    = Math.atan(arg1.val);
      intVal = (int)val;
      isInt  = false;
      isConst= arg1.isConstant();
    } else
      p.error("Numeric expression");
    origVal=val;
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
