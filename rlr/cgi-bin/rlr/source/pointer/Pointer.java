package pointer;
import parse.*;
import watch.Watcher;

/** A Pointer is an object that contains just a single public variable, val.
  * Since that variable can be changed, it acts like a pointer to the
  * variable in languages such as C.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.2, 23 May 97
  * @author Leemon Baird
  */
public abstract class Pointer implements Parsable {
  //v. 1.2 28 May 97 converted from interface to class
  //v. 1.1,28 Mar 97 added toDouble()
  //v. 1.0,4 June 96
  /** Create a new object of the same type with the same value */
  public abstract Object clone();

  /** Copy the value of this object into another of the same type */
  public abstract void copyInto(Pointer obj);

  /** If a pointer to a numeric type, convert the .val field to a double,
    * else just return 0.
    */
  public abstract double toDouble();

  /** Does this pointer and the other have the same value?
    * Different types , such as (int)3 and (long)3
    * are defined to have different values.
    */
  public abstract boolean equalVal(Pointer obj);

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "";
  }

  /** Output a description of this object that can be parsed with parse().*/
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
}
