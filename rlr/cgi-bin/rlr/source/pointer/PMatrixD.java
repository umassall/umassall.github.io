package pointer;
import matrix.*;
import parse.*;

/**
  * This is a pointer to a MatrixD, used for pass-by-reference calls.
  * Passing the pointer to a method allows that method to change
  * the value.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 22 Oct 96
  * @author Leemon Baird
  */
public class PMatrixD extends Pointer {
  /** The value which is pointed to. */
  public MatrixD val;

  /** The value can be initialized by the constructor */
  public PMatrixD(MatrixD initVal) {
    val=initVal;
  }

  /** Create a new object of the same type with the same value */
  public final Object clone() {
    return new PMatrixD(val);
  }

  /** Copy the value of this object into another of the same type */
  public final void copyInto(Pointer obj) {
    if (val!=null)
      ((PMatrixD)obj).val=val.duplicate();
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
    if (!(obj instanceof PMatrixD))
      return false;
    PMatrixD p=(PMatrixD)obj;
    if (val==null && p.val==null)
      return true;
    if (val==null || p.val==null)
      return false;
    return val.equalEls(p.val);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return new MatrixD(0).BNF(lang);
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (val!=null)
      u.emitUnparse(val,lang);
    else
      u.emit("null");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    if (p.parseID("null",false))
      val=null;
    else
      val=(MatrixD)p.parseClass("matrix.MatrixD",lang,true);
    return this;
  } //end parse

}
