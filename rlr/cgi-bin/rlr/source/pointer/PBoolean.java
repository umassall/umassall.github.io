package pointer;
import parse.*;

/**
  * This is a pointer to a boolean, used for pass-by-reference calls.
  * Passing the pointer to a method allows that method to change
  * the value.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 9 May 96
  * @author Leemon Baird
  */
public class PBoolean extends Pointer {
  /** The value which is pointed to. */
  public boolean val;

  /** The value can be initialized by the constructor */
  public PBoolean(boolean initVal) {
    val=initVal;
  }

  /** Create a new object of the same type with the same value */
  public final Object clone() {
    return new PBoolean(val);
  }

  /** Copy the value of this object into another of the same type */
  public final void copyInto(Pointer obj) {
    ((PBoolean)obj).val=val;
  }

  /** pointers convert to strings just as their values convert */
  public String toString() {
    return ""+val;
  }

  /** If a pointer to a numeric type, convert the .val field to a double,
    * else just return 0.
    */
  public double toDouble() {
    return val ? 1 : 0;
  }

  /** Does this pointer and the other have the same value?
    * Different types , such as (int)3 and (long)3
    * are defined to have different values.
    */
  public boolean equalVal(Pointer obj) {
    return (obj instanceof PBoolean) &&
           (val==((PBoolean)obj).val);
  }
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'true' | 'false' //a boolean ('true' or 'false')";
  }

  /** Output a description of this object that can be parsed with parse().*/
  public void unparse(Unparser u, int lang) {
    u.emit(""+val);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    val=p.parseBoolean(true);
    return this;
  }
}

