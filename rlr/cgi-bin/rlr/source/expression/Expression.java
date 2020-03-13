package expression;
import parse.*;

/** An Expression is an object representing a numerical expression.
  * It is parsable, and can be asked whether its value is integer
  * or double, whether it is simple (a single number) or complicated,
  * and whether it is constant or contains variables.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 10 May 97
  * @author Leemon Baird
  */
 interface Expression {

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang);

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang);

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException;

  /** Is this a simple constant like 3, 3.1, -.5 rather than an expression? */
  public boolean isSimple();

  /** Is this a constant with no variables? */
  public boolean isConstant();

  /** Is this an expression of type integer rather than double */
  public boolean isInt();
}
