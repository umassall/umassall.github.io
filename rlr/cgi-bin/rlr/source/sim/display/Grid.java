package sim.display;
import pointer.*;
import java.awt.*;
import parse.*;
import expression.*;
import watch.*;

/** This Plot draws a regular grid with a given feature size and type of grid.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.00, 1 June 97
  * @author Leemon Baird
  */
public class Grid extends Plot {
  NumExp size=new NumExp(1.0); //size of each grid cell
  IntExp type=new IntExp(0);   //0=squares, 1=triangles, 2=Penrose, 3=Half Penrose

  static int BOTH    =0; //type for both horizontal and vertical lines
  static int HORIZ   =1; //type for horizontal grid lines
  static int VERT    =2; //type for vertical grid lines
  static int HEX     =3; //type for grid of hexagons
  static int TRIANGLE=4; //type for grid of equalateral triangles
  static int PENROSE =5; //type for Penrose tiling with fat/skinny rhombus
  static int PENTRI  =6; //type for Penrose tiling with triangles that are half fat/skinny rhombus
  static int PENDOT  =7; //type for dots at intersections of Penrose tiling

  final double phi=(Math.sqrt(5)-1)/2; //golden mean=0.6180339

  private Object[][] parameters=
    {{"Regular grid. Useful as a background for other plots"},
     {"size",size,"size of one line segment (in graphing units, not pixels)",
      "type",type,"0=squares, 1=horiztal lines, 2=vertical lines, "+
                  "3=hexagons, 4=triangles, 5=Penrose, 6=Penrose triangles, "+
                  "7=Penrose dots"},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  //for faster drawing, make all these global rather than
  //constantly putting them on the stack
  double _xMin,_xMax,_yMin,_yMax;
  int _startX,_startY,_xAxisX,_xAxisY,_yAxisX,_yAxisY;
  Graphics _g;

  /** draws the tiling */
  public void drawAll(Graphics g, double xMin,   double xMax,
                                  double yMin,   double yMax,
                                  int    startX, int    startY,
                                  int    xAxisX, int    xAxisY,
                                  int    yAxisX, int    yAxisY) {
    _g     =g;
    _xMin  =xMin;
    _xMax  =xMax;
    _yMin  =yMin;
    _yMax  =yMax;
    _startX=startX;
    _startY=startY;
    _xAxisX=xAxisX;
    _xAxisY=xAxisY;
    _yAxisX=yAxisX;
    _yAxisY=yAxisY;
    _g.setColor(Color.black);

    if (type.val==HORIZ ||
        type.val==VERT  ||
        type.val==BOTH) {
      if (type.val==VERT || type.val==BOTH) {
        double lastX =Math.floor(xMax/size.val)*size.val;
        double firstX=Math.ceil (xMin/size.val)*size.val;
        for (double x=firstX;x<=lastX;x+=size.val)
          drawLine(x,yMin,x,yMax);
      }
      if (type.val==HORIZ || type.val==BOTH) {
        double lastY =Math.floor(yMax/size.val)*size.val;
        double firstY=Math.ceil (yMin/size.val)*size.val;
        for (double y=firstY;y<=lastY;y+=size.val)
          drawLine(xMin,y,xMax,y);
      }
    } else if (type.val==TRIANGLE) {
      double s=size.val;
      double h=s*Math.sqrt(3);
      for   (double x=Math.floor(xMin/s)*s; x<=xMax; x+=s)
        for (double y=Math.floor(yMin/h)*h; y<=yMax; y+=h) {
          drawLine(x,y+h/2,x+s,y+h/2);
          drawLine(x,y,    x+s,y);
          drawLine(x,y,    x+s,y+h);
          drawLine(x,y+h,  x+s,y);
        }
    } else if (type.val==HEX) {
      double w=size.val;        //width of a triangle
      double ww=3*w;            //width of entire rectangle
      double w2=w/2;            //half the width of a triangle
      double hh=w*Math.sqrt(3); //height of entire rectangle
      double h=hh/2;            //height of a triangle
      for   (double x=Math.floor(xMin/ww)*ww; x<=xMax; x+=ww)
        for (double y=Math.floor(yMin/hh)*hh; y<=yMax; y+=hh) {
          drawLine(x+w2,y+h,x,y);
          drawLine(x+w2,y+h,x,y+hh);
          drawLine(x+w2,y+h,x+w+w2,y+h);
          drawLine(x+w+w2,y+h,x+w+w,y);
          drawLine(x+w+w2,y+h,x+w+w,y+hh);
          drawLine(x+w+w,y,x+ww,y);

        }
    } else if (type.val==PENROSE ||   //Penrose tiles
               type.val==PENTRI  ||   //half of each Penrose tile
               type.val==PENDOT) {    //dots at corners of Penrose tiles
      int levels=0;
      double ax=0;//fat tringle has sides of length s, s, and s/phi where s=size.val
      double ay=0;
      double bx=0;
      double by=Math.cos(3.1415926/5)*size.val*2;
      double cx=Math.sin(3.1415926/5)*size.val;
      double cy=by/2;
      double r=0, tx=0,ty=0;

      for (int i=0;i<50;i++) { //repeatedly expand the triangular region to be filled
        //This should be modified to loop until triangle abc covers all of
        //xMin,yMin,xMax,yMax, then break.
        //But 50 iterations covers a region hundreds of millions of tiles wide,
        //so 50 iterations should generally be good enough.
        levels++;
        r=1+Math.sqrt(((ax-bx)*(ax-bx)+(ay-by)*(ay-by))/
                      ((ax-cx)*(ax-cx)+(ay-cy)*(ay-cy)));
        tx=ax+r*(cx-ax);
        ty=ay+r*(cy-ay);
        cx=bx; cy=by;
        bx=ax; by=ay;
        ax=tx; ay=ty;
      }
      drawPenrose(ax,ay,bx,by,cx,cy,levels,true); //fill in the large fat triangle
    }
  } //end drawAll

  //draw a line from x1,y1 to x2,y2
  private final void drawLine(double x1,double y1,double x2,double y2) {
    _g.drawLine((int)(_startX+(x1-_xMin)/(_xMax-_xMin)*(_xAxisX-_startX)),
                (int)(_startY+(y1-_yMin)/(_yMax-_yMin)*(_yAxisY-_startY)),
                (int)(_startX+(x2-_xMin)/(_xMax-_xMin)*(_xAxisX-_startX)),
                (int)(_startY+(y2-_yMin)/(_yMax-_yMin)*(_yAxisY-_startY)));
  }//end drawLine

  //draw a dot at (x,y)
  private final void drawDot(double x,double y) {
    _g.drawLine((int)(_startX+(x-_xMin)/(_xMax-_xMin)*(_xAxisX-_startX)),
                (int)(_startY+(y-_yMin)/(_yMax-_yMin)*(_yAxisY-_startY)),
                (int)(_startX+(x-_xMin)/(_xMax-_xMin)*(_xAxisX-_startX)),
                (int)(_startY+(y-_yMin)/(_yMax-_yMin)*(_yAxisY-_startY)));
  }//end drawLine

  //draw the tiling within a triangle that's half of a Penrose rhombus.
  //if fat, then fill half of a "fat" rhombus, else fill half of a "skinny" rhombus.
  //if triangles, then it draws lines through every Penrose rhombus
  //tile, cutting it into two equal triangles.
  //recursion ends when level=0
  //for fat,     corners a and b are 36 degrees, and c is 108 degrees.
  //for not fat, corners b and c are 72 degrees and a is 36 degrees
  //in both cases, the one side of the triangle to not draw is the
  //one connecting the two corners with equal angles.
  //In both cases, point d is on the side ab, such that lengths ad=bc.
  private void drawPenrose(double ax, double ay,
                           double bx, double by,
                           double cx, double cy,
                           int level, boolean fat) {
    if (ax<_xMin && bx<_xMin && cx<_xMin) return; //these 4 ifs speed up extreme zooms
    if (ax>_xMax && bx>_xMax && cx>_xMax) return;
    if (ay<_yMin && by<_yMin && cy<_yMin) return;
    if (ay>_yMax && by>_yMax && cy>_yMax) return;

    if (level<=0 && type.val==PENDOT) {
      drawDot(ax,ay);
      drawDot(bx,by);
      drawDot(cx,cy);
      return;
    }

    if (level<=0) {
      drawLine(ax,ay,cx,cy);
      if (!fat || type.val==PENTRI) drawLine(ax,ay,bx,by);
      if ( fat || type.val==PENTRI) drawLine(bx,by,cx,cy);
      return;
    }

    double r=Math.sqrt(((bx-cx)*(bx-cx)+(by-cy)*(by-cy))/
                       ((bx-ax)*(bx-ax)+(by-ay)*(by-ay)));
    double dx=ax+r*(bx-ax);
    double dy=ay+r*(by-ay);
    if (fat) {
      drawPenrose(ax,ay,cx,cy,dx,dy,level  ,false);
      drawPenrose(bx,by,cx,cy,dx,dy,level-1,true );
    } else {
      drawPenrose(cx,cy,dx,dy,bx,by,level-1,false);
      drawPenrose(cx,cy,ax,ay,dx,dy,level-1,true );
    }
  }//end drawPenrose
}//end class Grid
