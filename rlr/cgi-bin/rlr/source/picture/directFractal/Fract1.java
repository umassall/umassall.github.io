package picture.directFractal;
import picture.*;

/** A simple circular fractal with many parts shaped like quarter circles.
  * Many of the numerical constants have a suffix f because the compiler,
  * in direct violation of the spec, assumes they are double by default.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Fract1 extends DirectFractal {
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//fractal circles";
  }

  void f() { //many-to-one map of (x,y) into a bounded region
    x=frac(x-t);
    y=frac(y-t);
  };
  void g() { //one-to-one map of (x,y) from bounded region to whole plane
    double d=x*x+y*y;
    x/=d;
    y/=d;
  }
  void c(Colors color) { //map (x,y) from bounded region to a double
    color.value=x*x+y*y;
  }
} //end class Fract1
