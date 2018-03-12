package picture.directFractal;
import parse.*;
import picture.*;

/** This PicPipe returns the double at a point on a direct fractal.
  * There are a number of math routines defined that child classes can use.
  * The f(), g(), and c() functions and the BNF() method are abstract.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public abstract class DirectFractal extends PicPipe {
  protected static final double seed=1.1234f; //seed for direct 2D random number generator
  protected double origX,origY,origZ,origT;   //the original (x,y,z,t) coordinates
  protected double x,y,z,t;                   //the current (x,y,z,t) coordinates

  static final double floor(double x) {return (double)Math.floor((double)x);}
  static final double abs  (double x) {return x<0?-x:x;}
  static final double sgn  (double x) {return x<0?-1:x>0?1:0;}
  static final double frac (double x) {return x-(double)Math.floor((double)x);}
  static final double sqr  (double x) {return x*x;}
  static final double rnd  (double x,double y) {return frac(seed*(x+.25f)*(x+.25f)*(y+.75f)*(y+1.75f));}
  static final double mod  (double x,double n) {return n*frac(x/n);}
  static final double max  (double a,double b) {return a<b?b:a;}
//#define  big ((unsigned long)1<<31)
//#define  _and(x,y) ((long double) (((unsigned long)((x)*big))&((unsigned long)((y)*big)))/big)
//#define  _xor(x,y) ((long double) (((unsigned long)((x)*big))^((unsigned long)((y)*big)))/big)
//#define  rotate(x,y,a) {double _x=x,_y=y,_a=(a); x=_x*cosl(_a)+_y*sinl(_a); y=-_x*sinl(_a)+_y*cosl(_a);}
//#define  log(x) logl(((x)>1e-5)&&((x)<1e5) ? (x) : 1)
//#define  sqrtl(x) sqrtl((x)>=0 ? (x) : 0)

  abstract void f();             //many-to-one map of point into a bounded region
  abstract void g();             //one-to-one map of point from bounded region to whole plane
  abstract void c(Colors color); //map point from bounded region to a double

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  } //end method parse

  /** Return the color of a point on the direct fractal. */
  public void get(PicPipeList source, Colors color,
                  double x_,double y_,double z_,double t_,
                  double dx,double dy,double dz,double dt) {
    x=origX=x_;
    y=origY=y_;
    z=origZ=z_;
    t=origT=t_;
    //f(); //add this one in to ensure a pattern covers the plane.
    g();
    f();
    g();
    f();
    c(color);
  }//end get
} //end class DirectFractal
