package pointer;
/**
  * This is a pointer to an array of floats, used for pass-by-reference calls.
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
public class PFloatArray1D extends Pointer {
  /** The value which is pointed to. */
  public float[] val;

  /** The value can be initialized by the constructor */
  public PFloatArray1D(float[] initVal) {
    val=initVal;
  }

  /** Create a new object of the same type with the same value */
  public final Object clone() {
    return new PFloatArray1D(val);
  }

  /** Copy the value of this object into another of the same type */
  public final void copyInto(Pointer obj) {
    ((PFloatArray1D)obj).val=val;
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
    if (!(obj instanceof PFloatArray1D) ||
        obj==null)
      return false;
    float[] v=((PFloatArray1D)obj).val;
    if (v==null && val==null)
      return true;
    if ((v==null && val!=null) ||
        (v!=null && val==null) ||
        (v.length!=val.length))
      return false;
    for (int i=0;i<v.length;i++)
      if (v[i]!=val[i])
        return false;
    return true;
  }
}
