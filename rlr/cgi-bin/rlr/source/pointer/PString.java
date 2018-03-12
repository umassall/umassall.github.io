package pointer;
import parse.*;

/**
  * This is a pointer to a String, used for pass-by-reference calls.
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
public class PString extends Pointer {
  public PString() {super();} //CAFE bug: this shouldn't be needed

  /** The value which is pointed to. */
  public String val;

  /** The value can be initialized by the constructor */
  public PString(String initVal) {
    val=initVal;
  }

  /** Create a new object of the same type with the same value */
  public final Object clone() {
    return new PString(val);
  }

  /** Copy the value of this object into another of the same type */
  public final void copyInto(Pointer obj) {
    ((PString)obj).val=val;
  }

  /** pointers convert to strings just as their values convert */
  public String toString() {
    return ""+val;
  }

  /** If a pointer to a numeric type, convert the .val field to a double,
    * else just return 0.
    */
  public double toDouble() {
    return 0;
  }

  /** Does this pointer and the other have the same value?
    * Different types , such as (int)3 and (long)3
    * are defined to have different values.
    */
  public boolean equalVal(Pointer obj) {
    if (!(obj instanceof PString))
      return false;
    String p=((PString)obj).val;
    if (p==null && val==null)
      return true;
    if (p==null || val==null)
      return false;
    return val.equals(p);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "<string>//a string in single quotes";
  }

  /** Output a description of this object that can be parsed with parse().*/
  public void unparse(Unparser u, int lang) {
    if (val!=null) {
      u.emit("'");
      u.emit(val);
      u.emit("' ");
    } else
      u.emit("null");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    if (p.parseID("null",false))
      val=null;
    else
      val=p.parseString(true);
    return this;
  }
}