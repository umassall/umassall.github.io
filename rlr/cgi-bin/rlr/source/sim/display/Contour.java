package sim.display;
import Display;
import watch.*;
import pointer.*;
import java.awt.*;
import java.util.Vector;
import parse.*;
import sim.funApp.*;
import matrix.*;
import expression.*;
import fix.*;

/** Draw a contour plot with any number of contours.
  *    <p>This code is (c) 1996,1997 Ansgar Laubsch and Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 2.0 15 May 97
  * @author Ansgar Laubsch
  * @author Leemon Baird
  */
public class Contour extends Plot {
  //v  2.0 15 May 97 completely reorganized how plotting is done. -Leemon Baird
  //v. 1.2 15 April 97: Made corrections to the unparse method - Mance Harmon
  //v. 1.1 26 Mar 97
  //v. 1.0 22 Aug 96

  protected IntExp   numContours =new IntExp(10);// number of contour levels to draw
  protected FunApp   function    =null;          // Function approximator to plot
  protected FunApp[] origFunction=new FunApp[1]; // The origianl function approximator whose duplicate will be plotted
  protected MatrixD  inputs      =null;     //Vector of inputs, only 2 of which are overridden
  protected IntExp xSamples=new IntExp(10); // How many samples to take in the x dimension
  protected IntExp xElement=new IntExp(0);  // Which element of the input vector is plotted as x
  protected IntExp ySamples=new IntExp(10); // How many samples to take in the y dimension
  protected IntExp yElement=new IntExp(0);  // Which element of the input vector is plotted as y
  protected IntExp zElement=new IntExp(0);  // Which element of the output vector is plotted as z
  protected NumExp zMin    =new NumExp(1);  // Min value of z to plot (min>max for autoscaling)
  protected NumExp zMax    =new NumExp(0);  // Max value of z to plot (min>max for autoscaling)
  protected double data[][]=null; // data for plot, passed in by calling program
  protected double min, max;      // min/max height in dataset
  protected MatrixD inputM =null; // Input vector
  protected MatrixD outputM=null; // Output vector
  protected MatrixD weightM=null; // snapshot of the weights used by the learning system
  protected final double[] zeros={0,0,0};   //used in the definition of colorM
  protected Color    color   =Color.black;  //the color of the lines if not spectrum
  protected PMatrixD colorM  =new PMatrixD(new MatrixD(zeros).transpose()); //the RGB color of the lines, or [-1,-1,-1] for spectrum
  protected PBoolean spectrum=new PBoolean(false); //colors the lines proportional to height?

  private Object[][] parameters=
    {{"Contour plot. Given a function approximator, plot z vs. x and y"},
     {"function", origFunction, "the function to plot",
      "contours", numContours,  "# contour levels",
      "spectrum", spectrum, "use rainbow colors for curves?",
      "color",    colorM,   "color for curves if not spectrum",
      "xElement", xElement, "which element of input vector is X",
      "xSamples", xSamples, "# samples along x axis",
      "yElement", yElement, "which element of input vector is Y",
      "ySamples", ySamples, "# samples along y axis",
      "zElement", zElement, "which element of output vector to plot",
      "zMin",     zMin,     "Z value of lowest contour level, if zMin<zMax",
      "zMax",     zMax,     "(autoscales if zMax<zMin)"},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** Draw the contour lines.
    * The region of mathematical space to draw is (xMin,yMin)-(xMax,yMax), where
    * (xMin,yMin) is plotted at screen coordinates (startX,startY), and
    * (xMax,yMin) is plotted at screen coordinates (xAxisX,xAxisY), and
    * (xMin,yMax) is plotted at screen coordinates (yAxisX,yAxisY).
    */
  public void drawAll(Graphics g, double xMin,   double xMax,
                                  double yMin,   double yMax,
                                  int    startX, int    startY,
                                  int    xAxisX, int    xAxisY,
                                  int    yAxisX, int    yAxisY) {
    if (g==null)
      return;
    try { //ignore MatrixException
      if (!spectrum.val)
        g.setColor(color);
      if (data==null) { //first time update() called, get the data
        inputM =(inputs==null ? origFunction[0].getInput() : inputs).duplicate();
        outputM=origFunction[0].getOutput().duplicate();
        weightM=origFunction[0].getWeights().duplicate();
        function.setIO(inputM,outputM,weightM,null,null,null,null,null,null);
        data=new double[xSamples.val][ySamples.val];
      } else { //after first update, just take snapshot of current weights
        synchronized (parentDisplay.repaintInProgress) { //don't change weights in the middle of a paint()
          weightM.replace(origFunction[0].getWeights());
        }
      }

      if (data.length!=xSamples.val || data[0].length!=ySamples.val) //if size changed
        data=new double[xSamples.val][ySamples.val]; //recreate data cache with new size

      for (int x=0; x<data.length; x++) //fill cache data[][]
        for (int y=0; y<data[0].length; y++) {
          inputM.set(xElement.val,(double)x*(xMax-xMin)/(xSamples.val-1)+xMin);
          inputM.set(yElement.val,(double)y*(yMax-yMin)/(ySamples.val-1)+yMin);
          function.evaluate();
          data[x][y]=outputM.val(zElement.val);
        }
    } catch (MatrixException e) { e.print();
    }

    //get the min and max elements in the array
    if (zMin.val <= zMax.val) { //don't autoscale, use given bounds
      min=zMin.val;
      max=zMax.val;
    } else { //find bounds for autoscaling
      min=max=data[0][0];
      for (int x=0; x<xSamples.val; x++) {
        for (int y=0; y<ySamples.val; y++) {
          if (data[x][y] < min)
            min = data[x][y];
          if (data[x][y] > max)
            max = data[x][y];
        }
      }
    }

    int s1x,s1y,s2x,s2y,t,m;
    double[][] h={{0.,0.},{0.,0.}}; //heights scaled so there's a contour at all integers
    int [][] scx={{0,0},{0,0}};     //x coordinate on screen of the 4 corners
    int [][] scy={{0,0},{0,0}};     //y coordinate on screen of the 4 corners
    int fx,fy,tx,ty,x,y,i,j;
    double v;

    for (x=0; x<xSamples.val-1; x++)  {
      for (y=0; y<ySamples.val-1; y++)  {
          h[0][0]=(data[x  ][y  ]-min)/(max-min)*numContours.val;
          h[0][1]=(data[x  ][y+1]-min)/(max-min)*numContours.val;
          h[1][0]=(data[x+1][y  ]-min)/(max-min)*numContours.val;
          h[1][1]=(data[x+1][y+1]-min)/(max-min)*numContours.val;
          scx[0][0] = startX+(int)( ((double)(x  )/(xSamples.val-1))*(xAxisX-startX) +
                                    ((double)(y  )/(ySamples.val-1))*(yAxisX-startX));
          scy[0][0] = startY+(int)( ((double)(x  )/(xSamples.val-1))*(xAxisY-startY) +
                                    ((double)(y  )/(ySamples.val-1))*(yAxisY-startY));
          scx[1][0] = startX+(int)( ((double)(x+1)/(xSamples.val-1))*(xAxisX-startX) +
                                    ((double)(y  )/(ySamples.val-1))*(yAxisX-startX));
          scy[1][0] = startY+(int)( ((double)(x+1)/(xSamples.val-1))*(xAxisY-startY) +
                                    ((double)(y  )/(ySamples.val-1))*(yAxisY-startY));
          scx[0][1] = startX+(int)( ((double)(x  )/(xSamples.val-1))*(xAxisX-startX) +
                                    ((double)(y+1)/(ySamples.val-1))*(yAxisX-startX));
          scy[0][1] = startY+(int)( ((double)(x  )/(xSamples.val-1))*(xAxisY-startY) +
                                    ((double)(y+1)/(ySamples.val-1))*(yAxisY-startY));
          scx[1][1] = startX+(int)( ((double)(x+1)/(xSamples.val-1))*(xAxisX-startX) +
                                    ((double)(y+1)/(ySamples.val-1))*(yAxisX-startX));
          scy[1][1] = startY+(int)( ((double)(x+1)/(xSamples.val-1))*(xAxisY-startY) +
                                    ((double)(y+1)/(ySamples.val-1))*(yAxisY-startY));
          j=0;
          i=1;//i=0 if saddlepoint is interpreted as NW-SE lines, i=1 for NE-SW lines
          if (2<data.length) //can't find bias if data array is only 2 by 2 (1 square)
            if (  (x >0 && (data[x+1][y]-data[x  ][y])*(data[x  ][y]-data[x-1][y])>0)
               || (x==0 && (data[x+2][y]-data[x+1][y])*(data[x+1][y]-data[x  ][y])<0))
              i=0; //change interpretation of a square with 2 diagonal corners high

          t=(int)Math.ceil (Math.min(h[0][0],h[1][0]));
          m=(int)Math.floor(Math.max(h[0][0],h[1][0]));
          for (;t<=m;t++) { //for all contour levels crossing bottom of grid square
            double r=(t-h[0][0])/(h[1][0]-h[0][0]); //cont line is this fraction of way across
            if (h[1][0]==h[0][0]) r=0; //this will never happen, but prevent inf just in case
            int cx=(int)((1-r)*scx[0][0]+r*scx[1][0]); //screen coord of start of contour line
            int cy=(int)((1-r)*scy[0][0]+r*scy[1][0]);
            if (!connect(g,cx,cy,(double)t,  //draw line to first of 3 sides that match
                         scx[i][0],scy[i][0],h[i][0],
                         scx[i][1],scy[i][1],h[i][1]))
              if (!connect(g,cx,cy,(double)t,
                           scx[  i][1],scy[  i][1],h[  i][1],
                           scx[1-i][1],scy[1-i][1],h[1-i][1]))
                connect(g,cx,cy,(double)t,
                            scx[1-i][1],scy[1-i][1],h[1-i][1],
                            scx[1-i][0],scy[1-i][0],h[1-i][0]);
          }

          t=(int)Math.ceil (Math.min(h[i][0],h[i][1]));
          m=(int)Math.floor(Math.max(h[i][0],h[i][1]));
          for (;t<=m;t++) { //for all contour levels crossing left side of grid square (right when i=1)
            double r=(t-h[i][0])/(h[i][1]-h[i][0]); //cont line is this fraction of way across
            if (h[i][1]==h[i][0]) r=0; //this will never happen, but prevent inf just in case
            int cx=(int)((1-r)*scx[i][0]+r*scx[i][1]); //screen coord of start of contour line
            int cy=(int)((1-r)*scy[i][0]+r*scy[i][1]);
            if (!connect(g,cx,cy,(double)t,  //draw line to first of 3 sides that match
                         scx[  i][0],scy[  i][0],h[  i][0],
                         scx[1-i][0],scy[1-i][0],h[1-i][0]))
              if (!connect(g,cx,cy,(double)t,
                           scx[1-i][0],scy[1-i][0],h[1-i][0],
                           scx[1-i][1],scy[1-i][1],h[1-i][1]))
                connect(g,cx,cy,(double)t,
                            scx[1-i][1],scy[1-i][1],h[1-i][1],
                            scx[  i][1],scy[  i][1],h[  i][1]);
          }

          t=(int)Math.ceil (Math.min(h[0][1],h[1][1]));
          m=(int)Math.floor(Math.max(h[0][1],h[1][1]));
          for (;t<=m;t++) { //for all contour levels crossing top of grid square
            double r=(t-h[0][1])/(h[1][1]-h[0][1]); //cont line is this fraction of way across
            if (h[1][1]==h[0][1]) r=0; //this will never happen, but prevent inf just in case
            int cx=(int)((1-r)*scx[0][1]+r*scx[1][1]); //screen coord of start of contour line
            int cy=(int)((1-r)*scy[0][1]+r*scy[1][1]);
            connect(g,cx,cy,(double)t,
                        scx[1-i][1],scy[1-i][1],h[1-i][1],
                        scx[1-i][0],scy[1-i][0],h[1-i][0]);
          }
      }//end for y
    }//end for x
  }//end drawAll

  //Draw a line from point (x1,y1,h1) to the point with the same height
  //on the line (x2,y2,h2)-(x3,y3,h3).  It returns true
  //if there is such a point between them that has the same height
  //as h1 (assuming linear interpolation), and false otherwise.
  //This is called by drawAll
  private final boolean connect(Graphics g, int x1, int y1, double h1,
                                            int x2, int y2, double h2,
                                            int x3, int y3, double h3) {
    double r=(h3==h2) ? 2 : (h1-h2)/(h3-h2); //fraction of the way from 2 to 3
    if (r<0 || r>1)
      return false; //no location between 2 and 3 has height h1
    if (spectrum.val) //if spectrum
      g.setColor(Color.getHSBColor((float)(numContours.val-h1)/(float)(1.3*numContours.val),(float)1,(float)1));
    g.drawLine(x1,y1,(int)(x2+r*(x3-x2)),(int)(y2+r*(y3-y2)));
    return true;
  }

  /** ensure the function approximator has its destroy() called too */
  public void destroy() {
    function.destroy();
  }

  /** One of the watched variables has changed, so look at it and others.
    * It should call checkMoved() to
    * make sure the window is a legal size.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (changedVar==colorM)
      try {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
      } catch (MatrixException e) {
        e.print();
      }
    if (!parentDisplay.disableDisplays)
        parentDisplay.repaint();
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      function=(FunApp)origFunction[0].clone();
      if (colorM.val==null || colorM.val.size!=3)
        System.out.println("color vector is not 3 elements");
      try {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
      } catch (MatrixException e) {
      }
    }
  }
}  // end of class Contour
