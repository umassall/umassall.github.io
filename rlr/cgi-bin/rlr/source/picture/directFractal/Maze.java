package picture.directFractal;
import picture.*;

/** This DirectFractal is the maze on the USAFA CS110 94-95 textbook cover
  * This is an infinite maze that never repeats (quasiperiodic), with
  * exactly one path between any two points (no loops, no unreachable
  * regions).  Each wall of the maze is also such a maze.  The entire scene
  * is one wall of yet another such maze.
  * x and y are both in range [-1,1] to show the main maze.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Maze extends DirectFractal {
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//a fractal maze (CS110 cover)";
  }
  void f() {
    double xf=floor(x/2+.5f);
    double yf=floor(y/2+.5f);
    y=frac(y/2+.5f)*2-1
      +(1+sgn(.5f-abs(.5f-frac(x/2+.5f))-abs(.5f-frac(y/2+.5f))))
      *(4+(2-4*mod(xf+yf,2))
         *(1-2*floor(2*rnd(xf-floor(.4f*mod(0.5f+abs(xf)+abs(yf+.5f),4)),
                           yf-floor(.4f*mod(2.5f+abs(yf)+abs(xf+.5f),4)))))
         *(1.f-sgn(.5f-(abs(yf)+floor(.7f*mod(xf+3,4)))
                      *(abs(xf)+floor(.7f*mod(yf+1,4))))));
    x=(2*frac(x/2+.5f)-1) * (1-2*mod(xf+yf,2));
  }
  void g() {
    double d;
    d=abs(x)+abs(y);
    if (d<=1 && d>0) {
      d=(1+10*d*(1-d))/d/d;
      x=x*d;
      y=y*d;
    }
  }  
  void c(Colors color) {   //for black maze walls: negative=white,  nonnegative=black
    color.value=.5f-abs(x-y)/2;
  }
} //end class Maze
