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

/** Display a scalar function of 2 variables as a 3D plot
  * of the cube appear at desired locations on the screen.
  *    <p>This code is (c) 1996 Ansgar Laubsch and Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.5 15 Apr 97
  * @author Ansgar Laubsch
  * @author Leemon Baird
  */
public class Graph3D extends Display {
  //1.5 29 May 97:   made corrections to the unparse method - Scott Weaver
  //1.4 15 April 97: made corrections to the unparse method - Mance Harmon
  //1.3 30 Mar 97: most parameters are now watchable, editable at runtime
  //1.2 6 Nov 96: Fixed bug in "fill cache data" code.  The increments to x and y
  //were (xmax-xmin)/xSamples.  Changed to (xmax-xmin)/(xSamples-1). -Mance Harmon
  //1.1 3 Nov 96 snapshots of function approximators, converted to IntExp/NumExp
  //1.0 1 Jul 96

  //
  //- data is passed in twodimensional
  //array [x][y] = z
  //
  //- initial pos. of cube on screen is
  //passed in array
  //[x][y][z][0] = x coord of corner on screen
  //[x][y][z][1] = y coord of corner on screen
  //[x][y][z][2] = depth of corner into screen
  //
  //
  //------------
  //action:
  //
  //- O N   S T A R T U P (init)
  //(done once to prepare data for interact. plotting)
  //  - rescaling dataset
  //    - find min/maX in z-coords.
  //    - rescaledataset to be between 0 .... 1
  //      => heights in dataset correspond to
  //         virtual space coord.sys size (all
  //         axis between 0 .... 1
  //
  //- P A I N T
  //(automatically done by OS every time it paints
  // is needed)
  //  - get act. size of disp. win.
  //    (width / height)
  //  - calc data for part of screen to plot in
  //    with it (viewScreen, cx, cy[center])
  // (- clear screen)
  //  - rescale screen pos. of corners to fit in
  //    calculated part of screen (viewScreen)
  //    (calling rescale for this)
  //  - call drawAll to do plotting
  //
  //- R E S C A L E
  //(makes cube in given array fit in view screen)
  //  - find extrema in x/y coord. of corners of
  //    cube on screen (=> get overall pixel
  //    width & height of flat 2D cube view on
  //    screen)
  //  - compare it to width & height (in pixels)
  //    of viewScreen (part to plot in) and calc.
  //    factor
  //  - rescale cube by appliing the factor to all
  //    3 coord. of every corner.
  //
  //- G R A P H  3-D
  //(does the plot)
  //  - calc number of datasets in x&y dir.
  //  - find lowest corner of floor of cube
  //    (=> start drawing at this point later to
  //     be able to go from back to front, only
  //     hiding what is supposed to be hidden by
  //     things that are closer to the viwer than
  //     other things)
  //  - draw the backpart of the cube (lines
  //    spreading from lowest dorner)
  //  -

  private final double dtr = Math.PI/180; //to convert degrees to radians, multiply by this

  /* global Variables */
  protected double[] floorC   ={0,0,0};                               //just used to initialize colorM
  protected double[] topC     ={.5,.5,1};                             //just used to initialize colorM
  protected double[] bottomC  ={.25,.25,.5};                          //just used to initialize colorM
  protected PMatrixD floorColorM =new PMatrixD(new MatrixD(floorC ).transpose()); //[red,green,blue] for color
  protected PMatrixD topColorM   =new PMatrixD(new MatrixD(topC   ).transpose()); //[red,green,blue] for color
  protected PMatrixD bottomColorM=new PMatrixD(new MatrixD(bottomC).transpose()); //[red,green,blue] for color
  protected Color    floorColor  =new Color((float)0,  (float)0,  (float)0 ); //color of the floor when plotted
  protected Color    topColor    =new Color((float).5, (float).5, (float)1 ); //color of the floor when plotted
  protected Color    bottomColor =new Color((float).25,(float).25,(float).5); //color of the floor when plotted
  protected PBoolean plotFloor   =new PBoolean(false);                //is the floor being plotted on now?
  protected PBoolean plotTop     =new PBoolean(false);                //is the top of the cube being plotted on now?
  protected Parsable plot[][]    ={new Plot[0]};                      //every Plot for the floor, in order, top one last
  protected int[]    xs          =new int[5];                         //temporary varaible used in erasing floor/top
  protected int[]    ys          =new int[5];                         //temporary varaible used in erasing floor/top

  /** Function approximator to plot (a duplicate of the original)*/
  protected FunApp function=null;
  /** The origianl function approximator whose duplicate will be plotted */
  protected FunApp[] origFunction={null};
  /** Vector of inputs, only 2 of which are overridden */
  protected PMatrixD inputs=new PMatrixD(null);

  /** How many samples to take in the x dimension */
  protected IntExp xSamples=new IntExp(10);
  /** Which element of the input vector is plotted as x */
  protected IntExp xElement=new IntExp(0);
  /** Min value of x to plot */
  protected NumExp xMin=new NumExp(0);
  /** Max value of x to plot */
  protected NumExp xMax=new NumExp(1);

  /** Input vector */
  protected MatrixD inputM=null;
  /** Output vector */
  protected MatrixD outputM=null;
  /** snapshot of the weights used by the learning system */
  protected MatrixD weightM=null;
  /** pointer to the weights used by the learning system */
  protected PMatrixD weightP=null;


  /** How many samples to take in the y dimension */
  protected IntExp ySamples=new IntExp(10);
  /** Which element of the input vector is plotted as y */
  protected IntExp yElement=new IntExp(1);
  /** Min value of y to plot */
  protected NumExp yMin=new NumExp(0);
  /** Max value of y to plot */
  protected NumExp yMax=new NumExp(1);

  /** Which element of the output vector is plotted as z */
  protected IntExp zElement=new IntExp(0);
  /** Min value of z to plot (min>max for autoscaling) */
  protected NumExp zMin=new NumExp(1);
  /** Max value of z to plot (min>max for autoscaling) */
  protected NumExp zMax=new NumExp(0);

  /** Variable whose changes trigger updates of the graph */
  protected PString trigger=new PString("");
  /** How many times the trigger variable must change to cause an update */
  protected IntExp triggerFreq=new IntExp(1);

  /** Array of heights of points */
  public double[][] data=null;

  /** Array of coordinates of corners [x] [y] [z] [p]
    * where p=0 is x screen coordinate,
    *       p=1 is y screen coordinate and
    *       p=2 is depth into the screen of corner
    */
  double [][][][] corner    = new double [2][2][2][3];
  double [][][][] newCorner = new double [2][2][2][3];

  /** Position of mouseCursor on mouseDown-Event */
  public int xMouseDown, yMouseDown;

  /** Position of center of screen */
  public int cx, cy;

  /** Initial rotation of the cube about x axis (degrees) */
  public NumExp initXAngle=new NumExp(10);
  /** Initial rotation of the cube about y axis (degrees) */
  public NumExp initYAngle=new NumExp(30);
  /** Initial rotation of the cube about z axis (degrees) */
  public NumExp initZAngle=new NumExp(0);

  /** highest & lowest height Dataset in Array */
  public double maxHeight, minHeight;

  //don't put numeric labels on an axis whose projected length
  //on the screen (in pixels) is less than sqrt(axisLabelLen)
  private final static int axisLabelLen=100;

  /** Rectangle to fit cube for rescaling after rotation
    * [0][0..1] = upper-left corner x/y
    * [1][0..1] = lower-right corner x/y
    */
  public int [][] viewScreen = new int [2][2];

  /** coordinates of corner farthest away (corner[minX][minY][minZ])
    * for drawCubeOutlines
    */
  int minX;
  int minY;
  int minZ;

  /** is the mouse down right now, rotating the cube? */
  boolean mouseDownNow=false;

  private Object[][] parameters=
    {{"3D surface plot.  Given a function approximator, plot z vs. x and y every freq "+
      "steps on a 3D graph with the given rotation."+
      "DEFAULTS: rotate 10,30,0, z autoscales, no flicker "},
     {"flicker",     flicker,      "true=no double buffering, so it's faster, uses less memory, but flickers",
      "rotateX",     initXAngle,   "rotate viewing angle about x axis (in degrees)",
      "rotateY",     initYAngle,   "rotate about x, then y, then z",
      "rotateZ",     initZAngle,   "rotate about x, then y, then z",
      "trigger",     trigger,      "redraw graph every freq time that trigger signal changes",
      "freq",        triggerFreq,  "redraw frequency",
      "xSamples",    xSamples,     "# samples along x axis (2 or more)",
      "ySamples",    ySamples,     "# samples along y axis (2 or more)",
      "xMin",        xMin,         "min x in region to plot",
      "xMax",        xMax,         "max x in region to plot",
      "yMin",        yMin,         "min y in region to plot",
      "yMax",        yMax,         "max y in region to plot",
      "zMin",        zMin,         "min z to plot (or automatically scale if zMin>zMax)",
      "zMax",        zMax,         "max z to plot",
      "xElement",    xElement,     "which element of input vector is x (0 or more)",
      "yElement",    yElement,     "which element of input vector is y (0 or more)",
      "zElement",    zElement,     "which element of output vector is z (0 or more)",
      "function",    origFunction, "function to plot z vs. x and y",
      "plotFloor",   plotFloor,    "true=draw the plots on the floor of the cube",
      "plotTop",     plotTop,      "true=draw the plots on the top of the cube",
      "floorColor",  floorColorM,  "[red,green,blue] of floor of the cube",
      "topColor",    topColorM,    "[red,green,blue] of surface top",
      "bottomColor", bottomColorM, "[red,green,blue] of surface underside",
      "plots",       plot,         "list of PlotXY to draw on sube bottom/top",
      "inputs",      inputs,       "values for all input elements, since only 2 change with x and y"
      },
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** add menu items to the window containing this GWin canvas. */
  public void addMenus(MenuBar mb) {
    Menu menu=new Menu("View");
    menu.add("Redraw");
    menu.add("Front");
    menu.add("Back");
    menu.add("Y vs X");
    menu.add("Z vs X");
    menu.add("Z vs Y");
    mb.add(menu);
  } //end addMenus

  /** respond to the menu choices */
  public boolean action(Event e,Object w) {
    if      (((String)w).equals("Front" )) setRotation(     10,      30,  0);
    else if (((String)w).equals("Back"  )) setRotation(     10,     210,  0);
    else if (((String)w).equals("Y vs X")) setRotation(89.9999,  -.0001,-90);
    else if (((String)w).equals("Z vs X")) setRotation(  .0001,-89.9999,  0);
    else if (((String)w).equals("Z vs Y")) setRotation(      0,       0,  0);
    else if (((String)w).equals("Redraw")) ; //the update() below does the redraw
    else return super.action(e,w);

    update(null,null,null); //redraw everything
    return super.action(e,w);
  }

  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    for (int i=0;i<plot[0].length;i++)
      ((Plot)plot[0][i]).setWatchManager(wm,name+"plot"+i+"/");
  }//emd setWatchManager

  //set the cube to the desired orientation
  private void setRotation(double xAngle,double yAngle,double zAngle) {
    xAngle+=1e-10;
    yAngle-=1e-10;
    zAngle+=1e-10;
    for (int xi=0; xi<=1; xi++)  {
      for (int yi=0; yi<=1; yi++)  {
        for (int zi=0; zi<=1; zi++)  {
          corner[xi][yi][zi][0] = xi;
          corner[xi][yi][zi][1] = yi;
          corner[xi][yi][zi][2] = zi-(double).5;
          rotate3(corner[xi][yi][zi],0,90,-90);
          rotate3(corner[xi][yi][zi],xAngle,yAngle,zAngle);
        }
      }
    }
  } //end setRotation

  /* rescale all data so heights lie between 0 and 1 */
  private void rescale() {
    minHeight=maxHeight=data[0][0];

    // Find min. and max. height in Dataset & store it in min/maxHeight
    if (zMin.val<=zMax.val) { //if not autoscaling
      minHeight=zMin.val;
      maxHeight=zMax.val;
    } else {
      for (int testx = 0; testx < (data.length); testx++) {
        for (int testy = 0; testy < (data[0].length); testy++) {
          if (data[testx][testy] > maxHeight) {
            maxHeight = data[testx][testy];
          }
          if (data[testx][testy] < minHeight) {
            minHeight = data[testx][testy];
          }
        }
      }//end for testx
    } //end if autoscaling

    if (maxHeight<=minHeight) {
      maxHeight+=.0000001;
      minHeight-=.0000001;
    }

    // Rescale dataset to be between 0 .... 1
    for (int scalex = 0; scalex < (data.length); scalex++) {
      for (int scaley = 0; scaley < (data[0].length); scaley++) {
        data[scalex][scaley] = (data[scalex][scaley] - minHeight) / (maxHeight - minHeight);
        if (data[scalex][scaley]<0) data[scalex][scaley]=0; //clip to cube
        if (data[scalex][scaley]>1) data[scalex][scaley]=1;
      }
    }
  }//end rescale

  // given a point {x,y,z}, rotate that point about the given
  // axis (0=x, 1=y, 2=z), by the given angle angle (in degrees).
  private final void rotate (double[] point,int axis, double angle) {
    int a=(axis+1)%3, b=(axis+2)%3;
    double temp;
    temp    =Math.cos(angle*dtr)*point[a]-Math.sin(angle*dtr)*point[b];
    point[b]=Math.sin(angle*dtr)*point[a]+Math.cos(angle*dtr)*point[b];
    point[a]=temp;
  }

  // given a point {x,y,z}, rotate that point about the
  // y axis, then the x axis, then the z axis by given angles (in degrees)
  private final void rotate3 (double[] point, double angleX,
                              double angleY, double angleZ) {
    rotate(point,1,angleY);
    rotate(point,0,angleX);
    rotate(point,2,angleZ);
  }

  // given a point {x,y,z}

  /** One of the watched variables has been unregistered.
    */
  public void unregister(String watchedVar) {
  } //end unregister

  /** The variable changed, so redraw */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    try { //ignore MatrixException
      if (data==null) { //first time update() called, get the data
        inputM =(inputs.val==null ? origFunction[0].getInput() : inputs.val).duplicate();
        outputM=origFunction[0].getOutput().duplicate();
        weightM=origFunction[0].getWeights().duplicate();
        function.setIO(inputM,outputM,weightM,null,null,null,null,null,null);
        data=new double[xSamples.val][ySamples.val];
      } else { //after first update, just take snapshot of current weights
        synchronized (repaintInProgress) { //don't change weights in the middle of a paint()
          weightM.replace(origFunction[0].getWeights());
        }
      }

      if (changedVar==xSamples || changedVar==ySamples) {
        data=new double[xSamples.val][ySamples.val]; //recreate data cache with new size
      }

      if (changedVar==topColorM)
        topColor=new Color((float)topColorM.val.val(0),
                           (float)topColorM.val.val(1),
                           (float)topColorM.val.val(2));
      if (changedVar==bottomColorM)
        bottomColor=new Color((float)bottomColorM.val.val(0),
                              (float)bottomColorM.val.val(1),
                              (float)bottomColorM.val.val(2));
      if (changedVar==floorColorM)
        floorColor=new Color((float)floorColorM.val.val(0),
                             (float)floorColorM.val.val(1),
                             (float)floorColorM.val.val(2));

    } catch (NullPointerException e) { //update was called before the data was ready
    } catch (MatrixException e) { e.print();
    }
    super.update(changedName,changedVar,obj); //repaints screen if appropriate
  } //end update

  /** function drawAll
    * Show a 3D surface plot of the heights in data[][],
    * with the origin at (ax,ay), the X axis corner at (bx,by),
    * the Z axis corner at (cx,cy), and the Y axis corner at (dx,dy).
    */
  public void drawAll(Graphics g)  {
    if (data==null || inputM==null || outputM==null || function==null )
      return; //if the data structures aren't set up yet, don't draw
    try { //ignore MatrixException
      for (int x=0; x<(data.length); x++) //fill cache data[][]
        for (int y=0; y<(data[0].length); y++) {
          inputM.set(xElement.val,(double)x*(xMax.val-xMin.val)/(xSamples.val-1)+xMin.val);
          inputM.set(yElement.val,(double)y*(yMax.val-yMin.val)/(ySamples.val-1)+yMin.val);
          function.evaluate();
          data[x][y]=outputM.val(zElement.val);
        }
    } catch (MatrixException e) {
      e.print(); //this should never happen
    }

    if (data!=null)
      rescale();

    try {
      // Get size of screen
      int width =bounds().width;
      int height=bounds().height;
      if (data==null)
        return;

      // ... and init viewScreen with it
      viewScreen [0][0] = width/5; // x of upper-left corner
      viewScreen [0][1] = height/5; // y of upper-left corner
      viewScreen [1][0] = width - (width/5); // x of lower-right corner
      viewScreen [1][1] = height - (height/5); // y of lower-right corner

      // init center of screen as center of viewScreen
      cx = (viewScreen[1][0] - viewScreen[0][0]) / 2 + viewScreen[0][0];
      cy = (viewScreen[1][1] - viewScreen[0][1]) / 2 + viewScreen[0][1];

      g.clearRect(0,0,10000,10000);//clear screen
      g.setColor(Color.black);

      rescale (corner); //rescale data

      /** Variabels */

      /** Number of Datasets in Array data */
      double nx, ny;

      /** Define Arrays for triangle drawing
        * [0][0..2] = x/y coord of 1st triangle
        * [1][0..2] = x/y coord of 2nd triangle */
      int[][] xcoord = new int[2][3];
      int[][] ycoord = new int[2][3];
      int corners = 3;

      /** Drawing surface plot: */
      // 1. calculate Number of Datasets (nx, ny)
      nx = data.length;
      ny = data[0].length;

      // 2. Find lowest corner and set operator for drawing
      int minX = 0;
      int minY = 0;

      if (g==null)
        return;

      for (int x=0; x<=1; x++)  {
        for (int y=0; y<=1; y++)  {
          if (corner[x][y][0][2] > corner[minX][minY][0][2])  {
            minX = x;
            minY = y;
          }
        }
      }
      int xOp = (minX == 1) ? 1 : 0;
      int yOp = (minY == 1) ? 1 : 0;

      // Draw (not dir. visible) back part of cube outlines
      if (!mouseDownNow) //don't draw outline twice if screen updates while user is dragging
        drawCubeOutlines (g, 1, 1, false, corner);

      // 3. Plot surface
      int yInit = (yOp==0) ? 0 : (int)ny-2;
      int yReinit = (yOp==0)? 1 : -1;
      int xInit = (xOp==0) ? 0 : (int)nx-2;
      int xReinit = (xOp==0)? 1 : -1;

      for (int y = yInit; ((yOp==0)?(y<ny-1):(y>-1)); y += yReinit)  {     // For every 'line' do....
        for (int x = xInit; ((xOp==0)?(x<nx-1):(x>-1)); x += xReinit)  {   // For every 'colum' do...

          // 3.1.1 calc x/y data (on screen!) for 1st triangle
          xcoord[0][0] = xs ( (x/(nx-1)), (y/(ny-1)), data [x][y], corner);
          ycoord[0][0] = ys ( (x/(nx-1)), (y/(ny-1)), data [x][y], corner);
          xcoord[0][1] = xs ( (x/(nx-1)), ((y+1)/(ny-1)), data [x][y+1], corner);
          ycoord[0][1] = ys ( (x/(nx-1)), ((y+1)/(ny-1)), data [x][y+1], corner);
          xcoord[0][2] = xs ( ((x+1)/(nx-1)), ((y+1)/(ny-1)), data [x+1][y+1], corner);
          ycoord[0][2] = ys ( ((x+1)/(nx-1)), ((y+1)/(ny-1)), data [x+1][y+1], corner);

          // 3.1.2 calc x/y data (on screen!) for 2nd triangle
          // x/ycoord[0] & x/ycoord[2] are identical with the 1st triangle!
          xcoord[1][0] = xcoord[0][0];
          ycoord[1][0] = ycoord[0][0];
          xcoord[1][1] = xs ( ((x+1)/(nx-1)), (y/(ny-1)), data [x+1][y], corner);
          ycoord[1][1] = ys ( ((x+1)/(nx-1)), (y/(ny-1)), data [x+1][y], corner);
          xcoord[1][2] = xcoord[0][2];
          ycoord[1][2] = ycoord[0][2];

          // 3.2.0 Find out which triangle to draw first
          // var. first/second = no. of triangle in x/ycoord[] to draw 1st/2nd
          int first;
          int second;
          if (corner[0][1][0][2] > corner[1][0][0][2])  {
            first = 0;
            second = 1;
          }
          else  {
            first = 1;
            second = 0;
          }

          // 3.2.1 Draw 1st triangle
          // 3.2.1.1 Do we see top or underside of triangle?
          if ( ((xcoord[first][1]-xcoord[first][0]) * (ycoord[first][2]-ycoord[first][0]) - (xcoord[first][2]-xcoord[first][0]) * (ycoord[first][1]-ycoord[first][0])) > 0)
            g.setColor(first==0 ? topColor : bottomColor);
          else
            g.setColor(first==0 ? bottomColor : topColor);
          g.fillPolygon (xcoord[first], ycoord[first], corners);
          g.setColor(Color.black);
          g.drawPolygon (xcoord[first], ycoord[first], corners);

          // 3.2.2 Draw 2nd triangle
          // 3.2.2.1 Do we see top or underside of triangle?
          if ( ((xcoord[second][1]-xcoord[second][0]) * (ycoord[second][2]-ycoord[second][0]) - (xcoord[second][2]-xcoord[second][0]) * (ycoord[second][1]-ycoord[second][0])) > 0)
            g.setColor(first==0 ? bottomColor : topColor);
          else
            g.setColor(first==0 ? topColor : bottomColor);
          g.fillPolygon (xcoord[second], ycoord[second], corners);
          g.setColor(Color.black);
          g.drawPolygon (xcoord[second], ycoord[second], corners);
        }
      }

      // Draw (visible) front part of cube outlines
      if (!mouseDownNow) //don't draw outline twice if screen updates while user is dragging
        drawCubeOutlines (g, 2, 1, true, corner);
    } catch (ArrayIndexOutOfBoundsException e) {
    } //in case user changes # samples in the middle of a paint operation

    if (mouseDownNow) { //make sure outline doesn't flicker when user rotates cube during redraws
      drawCubeOutlines (g, 1, 2, false, newCorner);
      drawCubeOutlines (g, 2, 2, false, newCorner);
    }
  }  //end of function drawAll


  /** fucntion xs
    * calculates screen-x-coordinate from space coordinates of a point */
  public int xs(double x, double y, double height, double[][][][] corner) {

    int xscreen = (int) (corner[0][0][0][0] + (corner[1][0][0][0]-corner[0][0][0][0]) * x + (corner[0][1][0][0]-corner[0][0][0][0]) * y + (corner[0][0][1][0]-corner[0][0][0][0]) * height);
    return (xscreen);

  }  // end of function xs


  /** function ys
    * calculates screen-y-coordinate from space coordinates of a point */
  public int ys(double x, double y, double height, double[][][][] corner) {

    int yscreen = (int) (corner[0][0][0][1] + (corner[1][0][0][1]-corner[0][0][0][1]) * x + (corner[0][1][0][1]-corner[0][0][0][1]) * y + (corner[0][0][1][1]-corner[0][0][0][1]) * height);
    return (yscreen);

  }  // end of function ys


  /** function mouseDown
    * Event Handling for pressing mouseButton */
  public boolean mouseDown (Event e, int x, int y)  {
    mouseDownNow=true;

    // Save coordinates of starting point of dragging
    xMouseDown = x;
    yMouseDown = y;

    // copy 'corner' array to 'newCorner'
    for (int xc=0; xc<=1; xc++)  {
      for (int yc=0; yc<=1; yc++)  {
        for (int zc=0; zc<=1; zc++)  {
          for (int ic=0; ic<=2; ic++)  {
            newCorner [xc][yc][zc][ic] = corner [xc][yc][zc][ic];
          }
        }
      }
    }

    return true;

  }  // end of function MouseDown


  /** function mouseDrag
    * Event Handling for dragging Mouse
    */
  public boolean mouseDrag (Event e, int x, int y)  {
      // Set moving speed
      double speed = (double).01;

      // Find lowest corner of 4 and set operator for drawing
      int minX = 0;
      int minY = 0;
      for (int xf=0; xf<=1; xf++)  {
        for (int yf=0; yf<=1; yf++)  {
          if (newCorner[xf][yf][0][2] > newCorner[minX][minY][0][2])  {
            minX = xf;
            minY = yf;
          }
        }
      }//end for xf

      // Find lowest corner of 8
      int minXX=0,minYY=0,minZZ=0;
      for (int xf=0; xf<=1; xf++)  {
        for (int yf=0; yf<=1; yf++)  {
          for (int zf=0; zf<=1; zf++)  {
            if (corner[xf][yf][zf][2] > corner[minXX][minYY][minZZ][2])  {
              minXX=xf;
              minYY=yf;
              minZZ=zf;
            }
          }
        }
      }//end for xf

      int xOp = (minX == 1) ? 1 : 0;
      int yOp = (minY == 1) ? 1 : 0;

      // routines for recalc of points:
      // Calc distance detween grabStartingPoint and actual position
      int dx = x - xMouseDown;
      int dy = y - yMouseDown;

      // Calc some constants for moving
      double a = (double)Math.cos(speed * dx);
      double b = (double)Math.sin(speed * dx);
      double c = (double)Math.cos(speed * dy);
      double d = (double)Math.sin(speed * dy);
      double d2;
      //s is positive or negative depending on whether the perimeter
      //of the cube will be traced clockwise or counterclockwise.
      double s=(corner[1-minXX][1-minYY][minZZ][1]-
               corner[1-minXX][  minYY][minZZ][1])*
              (corner[  minXX][1-minYY][minZZ][0]-
               corner[1-minXX][  minYY][minZZ][0]) -
              (corner[1-minXX][1-minYY][minZZ][0]-
               corner[1-minXX][  minYY][minZZ][0])*
              (corner[  minXX][1-minYY][minZZ][1]-
               corner[1-minXX][  minYY][minZZ][1]);

      // Calc new positions of corners acccording to x and y
      if (s*(xMouseDown-corner[1-minXX][  minYY][  minZZ][0])*(corner[1-minXX][1-minYY][  minZZ][1]-corner[1-minXX][  minYY][  minZZ][1]) -
          s*(yMouseDown-corner[1-minXX][  minYY][  minZZ][1])*(corner[1-minXX][1-minYY][  minZZ][0]-corner[1-minXX][  minYY][  minZZ][0]) < 0 ||
          s*(xMouseDown-corner[1-minXX][1-minYY][  minZZ][0])*(corner[  minXX][1-minYY][  minZZ][1]-corner[1-minXX][1-minYY][  minZZ][1]) -
          s*(yMouseDown-corner[1-minXX][1-minYY][  minZZ][1])*(corner[  minXX][1-minYY][  minZZ][0]-corner[1-minXX][1-minYY][  minZZ][0]) < 0 ||
          s*(xMouseDown-corner[  minXX][1-minYY][  minZZ][0])*(corner[  minXX][1-minYY][1-minZZ][1]-corner[  minXX][1-minYY][  minZZ][1]) -
          s*(yMouseDown-corner[  minXX][1-minYY][  minZZ][1])*(corner[  minXX][1-minYY][1-minZZ][0]-corner[  minXX][1-minYY][  minZZ][0]) < 0 ||
          s*(xMouseDown-corner[  minXX][1-minYY][1-minZZ][0])*(corner[  minXX][  minYY][1-minZZ][1]-corner[  minXX][1-minYY][1-minZZ][1]) -
          s*(yMouseDown-corner[  minXX][1-minYY][1-minZZ][1])*(corner[  minXX][  minYY][1-minZZ][0]-corner[  minXX][1-minYY][1-minZZ][0]) < 0 ||
          s*(xMouseDown-corner[  minXX][  minYY][1-minZZ][0])*(corner[1-minXX][  minYY][1-minZZ][1]-corner[  minXX][  minYY][1-minZZ][1]) -
          s*(yMouseDown-corner[  minXX][  minYY][1-minZZ][1])*(corner[1-minXX][  minYY][1-minZZ][0]-corner[  minXX][  minYY][1-minZZ][0]) < 0 ||
          s*(xMouseDown-corner[1-minXX][  minYY][1-minZZ][0])*(corner[1-minXX][  minYY][  minZZ][1]-corner[1-minXX][  minYY][1-minZZ][1]) -
          s*(yMouseDown-corner[1-minXX][  minYY][1-minZZ][1])*(corner[1-minXX][  minYY][  minZZ][0]-corner[1-minXX][  minYY][1-minZZ][0]) < 0) {
        // Calc new positions of corners to rotate image
        double theta=(double)Math.acos(((x-cx)*(xMouseDown-cx)+(y-cy)*(yMouseDown-cy)) /
                                     (Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy)) *
                                      Math.sqrt((xMouseDown-cx)*(xMouseDown-cx)+(yMouseDown-cy)*(yMouseDown-cy)))) *
                                      ((x-cx)*(yMouseDown-cy)-(xMouseDown-cx)*(y-cy)>0 ? -1 : 1);
        for (int xn=0; xn<=1; xn++)  {
          for (int yn=0; yn<=1; yn++)  {
            for (int zn=0; zn<=1; zn++)  {
               newCorner [xn][yn][zn][0] = (double)Math.cos(theta)*(corner[xn][yn][zn][0]-cx)-
                                           (double)Math.sin(theta)*(corner[xn][yn][zn][1]-cy)+cx;
               newCorner [xn][yn][zn][1] = (double)Math.sin(theta)*(corner[xn][yn][zn][0]-cx)+
                                           (double)Math.cos(theta)*(corner[xn][yn][zn][1]-cy)+cy;
               newCorner [xn][yn][zn][2] = corner[xn][yn][zn][2];
            }
          }
        }//end for xn
      } //end if s* ...
      else { //rotate cube rather than image
        // Calc new positions to rotate cube
        for (int xn=0; xn<=1; xn++)  {
          for (int yn=0; yn<=1; yn++)  {
           for (int zn=0; zn<=1; zn++)  {
              d2 = b * (corner[xn][yn][zn][0] - cx) + a *  corner[xn][yn][zn][2];
              newCorner [xn][yn][zn][0] = a * (corner[xn][yn][zn][0] - cx) - b *  corner[xn][yn][zn][2] + cx;
              newCorner [xn][yn][zn][1] = c * (corner[xn][yn][zn][1] - cy) - d * d2 + cy;
              newCorner [xn][yn][zn][2] = d * (corner[xn][yn][zn][1] - cy) + c * d2;
            }
          }
        }
      }//end else rotate cube

      // rescale newCorner Array
      rescale (newCorner);

      // Find lowest corner and set operator for drawing
      minX = 0;
      minY = 0;
      for (int xf=0; xf<=1; xf++)  {
        for (int yf=0; yf<=1; yf++)  {
          if (newCorner[xf][yf][0][2] > newCorner[minX][minY][0][2])  {
            minX = xf;
            minY = yf;
          }
        }
      }
      xOp = (minX == 1) ? 1 : 0;
      yOp = (minY == 1) ? 1 : 0;

      repaint();
      return true;
  }  // end of function MouseDrag


  /** funtion mouseUp
    * Event Handling for releasing mouseButton */
  public boolean mouseUp (Event e, int x, int y)  {
    mouseDownNow=false;

    // copy 'newCorner' array to 'corner'
    for (int xc=0; xc<=1; xc++)  {
      for (int yc=0; yc<=1; yc++)  {
        for (int zc=0; zc<=1; zc++)  {
          for (int ic=0; ic<=2; ic++)  {
            corner [xc][yc][zc][ic] = newCorner [xc][yc][zc][ic];
          }
        }
      }
    }

    // paint again...
    update (null,null,null);

    return true;

  }  // end of function MouseUp


  /** function drawCubeOutlines
    * Draw outlines of Cube
    * part = 1 draws backpart
    * part = 2 draws frontpart
    * mode = 1 draws outlines normally
    * mode = 2 erases outlines
    * numbers = true prints numbers & names of axis
    * numbers = false does not print any numbers and names of axis */
  public void drawCubeOutlines (Graphics g, int part, int mode, boolean numbers, double[][][][] corner)  {

    /** Variabels for drawing scale on outlines */
    double x0, y0, xa, ya, xb, yb, ln;

    // set normal PaintMode
    g.setPaintMode();

    // select which part to draw
    switch (part)  {

      // Draw (not directly visible) back part of cube in light Gray
      case 1:  {

        // Find out, which corner is farthest away <- corner[minX][minY][minZ]
        minX = 0;
        minY = 0;
        minZ = 0;
        for (int x=0; x<=1; x++)  {
          for (int y=0; y<=1; y++)  {
            for (int z=0; z<=1; z++)  {
              if (corner[x][y][z][2] > corner[minX][minY][minZ][2])  {
                minX = x;
                minY = y;
                minZ = z;
              }
            }
          }
        }

        // draw lines coming out of farthest point (corner[minX][minY][minZ])

        g.setColor(Color.gray); //the 3 more distant edges are gray (near are black)
        // if mode = 2 => set XORMode for erasing
        if (mode == 2)  {
          g.setXORMode(Color.white);
        }

        // draw outlines
        g.drawLine ((int)corner[minX][minY][minZ][0], (int)corner[minX][minY][minZ][1], (int)corner[minX][minY][1-minZ][0], (int)corner[minX][minY][1-minZ][1]);
        g.drawLine ((int)corner[minX][minY][minZ][0], (int)corner[minX][minY][minZ][1], (int)corner[minX][1-minY][minZ][0], (int)corner[minX][1-minY][minZ][1]);
        g.drawLine ((int)corner[minX][minY][minZ][0], (int)corner[minX][minY][minZ][1], (int)corner[1-minX][minY][minZ][0], (int)corner[1-minX][minY][minZ][1]);

        for (int i=0;i<plot[0].length;i++) { //draw all the plots on the near part of the cube
          if (!mouseDownNow &&
              ((minZ==1 && plotTop.val) ||
               (minZ==0 && plotFloor.val))) {
            xs[0]=xs[4]=(int)corner[0][0][minZ][0];
            ys[0]=ys[4]=(int)corner[0][0][minZ][1];
            xs[1]=      (int)corner[1][0][minZ][0];
            ys[1]=      (int)corner[1][0][minZ][1];
            xs[2]=      (int)corner[1][1][minZ][0];
            ys[2]=      (int)corner[1][1][minZ][1];
            xs[3]=      (int)corner[0][1][minZ][0];
            ys[3]=      (int)corner[0][1][minZ][1];
            g.setColor(floorColor);
            g.fillPolygon(xs,ys,5);
            ((Plot)plot[0][i]).drawAll(g,xMin.val,  xMax.val, yMin.val,  yMax.val,
                              xs[0],ys[0],xs[1],ys[1],xs[3],ys[3]);
          }
        }

        // end of case1 (draw partly visible part of outlines)
        break;
      }

      // Draw (visible) front part of cube
      case 2:  {
        // set front draw color to black
        g.setColor(Color.black);
        // if mode = 2 => set XORMode for erasing
        if (mode == 2)  {
          g.setXORMode(Color.white);
        }

        // draw 3 * 3 outlines, spreading from

        // - corner [1-minX][1-minY][minZ][0/1]
        g.drawLine ((int)corner[1-minX][1-minY][minZ][0], (int)corner[1-minX][1-(minY)][minZ][1], (int)corner[1-(minX)][1-(minY)][1-(minZ)][0], (int)corner[1-(minX)][1-(minY)][1-(minZ)][1] );
        g.drawLine ((int)corner[1-(minX)][1-(minY)][minZ][0], (int)corner[1-(minX)][1-(minY)][minZ][1], (int)corner[1-(minX)][minY][minZ][0], (int)corner[1-(minX)][minY][minZ][1] );
        g.drawLine ((int)corner[1-(minX)][1-(minY)][minZ][0], (int)corner[1-(minX)][1-(minY)][minZ][1], (int)corner[minX][1-(minY)][minZ][0], (int)corner[minX][1-(minY)][minZ][1] );

        // - corner [1-(minX)][minY][1-(minZ)][0/1]
        g.drawLine ((int)corner[1-(minX)][minY][1-(minZ)][0], (int)corner[1-(minX)][minY][1-(minZ)][1], (int)corner[1-(minX)][minY][minZ][0], (int)corner[1-(minX)][minY][minZ][1] );
        g.drawLine ((int)corner[1-(minX)][minY][1-(minZ)][0], (int)corner[1-(minX)][minY][1-(minZ)][1], (int)corner[1-(minX)][1-(minY)][1-(minZ)][0], (int)corner[1-(minX)][1-(minY)][1-(minZ)][1] );
        g.drawLine ((int)corner[1-(minX)][minY][1-(minZ)][0], (int)corner[1-(minX)][minY][1-(minZ)][1], (int)corner[minX][minY][1-(minZ)][0], (int)corner[minX][minY][1-(minZ)][1] );

        // - corner [minX][1-(minY)][1-(minZ)][0/1]
        g.drawLine ((int)corner[minX][1-(minY)][1-(minZ)][0], (int)corner[minX][1-(minY)][1-(minZ)][1], (int)corner[minX][1-(minY)][minZ][0], (int)corner[minX][1-(minY)][minZ][1] );
        g.drawLine ((int)corner[minX][1-(minY)][1-(minZ)][0], (int)corner[minX][1-(minY)][1-(minZ)][1], (int)corner[minX][minY][1-(minZ)][0], (int)corner[minX][minY][1-(minZ)][1] );
        g.drawLine ((int)corner[minX][1-(minY)][1-(minZ)][0], (int)corner[minX][1-(minY)][1-(minZ)][1], (int)corner[1-(minX)][1-(minY)][1-(minZ)][0], (int)corner[1-(minX)][1-(minY)][1-(minZ)][1] );

        for (int i=0;i<plot[0].length;i++) { //draw all the plots on the near part of the cube
          if (!mouseDownNow &&
              ((minZ==0 && plotTop.val) ||
               (minZ==1 && plotFloor.val))) {
            xs[0]=xs[4]=(int)corner[0][0][1-minZ][0];
            ys[0]=ys[4]=(int)corner[0][0][1-minZ][1];
            xs[1]=      (int)corner[1][0][1-minZ][0];
            ys[1]=      (int)corner[1][0][1-minZ][1];
            xs[2]=      (int)corner[1][1][1-minZ][0];
            ys[2]=      (int)corner[1][1][1-minZ][1];
            xs[3]=      (int)corner[0][1][1-minZ][0];
            ys[3]=      (int)corner[0][1][1-minZ][1];
            g.setColor(floorColor);
            g.fillPolygon(xs,ys,5);
            ((Plot)plot[0][i]).drawAll(g,xMin.val,  xMax.val, yMin.val,  yMax.val,
                              xs[0],ys[0],xs[1],ys[1],xs[3],ys[3]);
          }
        }

        // Draw scale onto cube

        // 0.1 lenght of marks (constant)
        ln = 10;
        // 0.2 constant for drawing marks
        double k = (corner [1-(minX)][1-(minY)][minZ][0]-corner [minX][1-(minY)][minZ][0]) * (corner [1-(minX)][minY][minZ][1]-corner [minX][1-(minY)][minZ][1]) - (corner [1-(minX)][1-(minY)][minZ][1]-corner [minX][1-(minY)][minZ][1]) * (corner [1-(minX)][minY][minZ][0]-corner [minX][1-(minY)][minZ][0]);

        // 1.1 Calculate screenpoints of x-axis
        xa = corner [minX][1-(minY)][minZ][0];
        ya = corner [minX][1-(minY)][minZ][1];
        xb = corner [1-(minX)][1-(minY)][minZ][0];
        yb = corner [1-(minX)][1-(minY)][minZ][1];
        if (k > 0)  {
          double xtemp = xa;
          double ytemp = ya;
          xa = xb;
          ya = yb;
          xb = xtemp;
          yb = ytemp;
        }
        // 1.2 Calculate and draw marks
        for (double j=0; j <= 1; j+=(double).2) {
          x0 = xs ( j, 1-(minY), minZ, corner);
          y0 = ys ( j, 1-(minY), minZ, corner);
          mark (g, x0, y0, xa, ya, xb, yb, ln);
        }

        String ns=null; //temporarily holds number converted to a string

        // 1.3 Calculate and draw numbers
        g.setColor(Color.black); //the 5 near edges are black (back 3 are gray)
        if (numbers && (xa-xb)*(xa-xb)+(ya-yb)*(ya-yb)>axisLabelLen) {
          // xMin.val
          x0 = xs ( 0, 1-(minY), minZ, corner);
          y0 = ys ( 0, 1-(minY), minZ, corner);
          ns=Util.toString(xMin.val,5,3); //to 5 decimal places, scientific not. after 2 zeros
          double stringWidth = g.getFontMetrics().stringWidth(ns);
          double stringHeight = g.getFontMetrics().getHeight();
          int xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/(double)2));
          int yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
          // xMax.val
          x0 = xs ( 1, 1-(minY), minZ, corner);
          y0 = ys ( 1, 1-(minY), minZ, corner);
          ns=Util.toString(xMax.val,5,3); //to 5 decimal places, scientific not. after 2 zeros
          stringWidth = g.getFontMetrics().stringWidth(ns);
          stringHeight = g.getFontMetrics().getHeight();
          xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/2));
          yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
        }

        // 2.1 Calculate screenpoints of y-axis
        xa = corner [1-(minX)][1-(minY)][minZ][0];
        ya = corner [1-(minX)][1-(minY)][minZ][1];
        xb = corner [1-(minX)][minY][minZ][0];
        yb = corner [1-(minX)][minY][minZ][1];
        if (k > 0)  {
          double xtemp = xa;
          double ytemp = ya;
          xa = xb;
          ya = yb;
          xb = xtemp;
          yb = ytemp;
        }
        // 2.2 Calculate and draw marks
        for (double j=0; j <= 1; j+=(double).2) {
          x0 = xs (1-(minX), j, minZ, corner);
          y0 = ys (1-(minX), j, minZ, corner);
          mark (g, x0, y0, xa, ya, xb, yb, ln);
        }
        // 2.3 Calculate and draw numbers
        if (numbers && (xa-xb)*(xa-xb)+(ya-yb)*(ya-yb)>axisLabelLen) {
          // yMin.val
          x0 = xs (1-(minX), 0, minZ, corner);
          y0 = ys (1-(minX), 0, minZ, corner);
          ns=Util.toString(yMin.val,5,3); //to 5 decimal places, scientific not. after 2 zeros
          double stringWidth = g.getFontMetrics().stringWidth(ns);
          double stringHeight = g.getFontMetrics().getHeight();
          int xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/(double)2));
          int yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
          // yMax.val
          x0 = xs (1-(minX), 1, minZ, corner);
          y0 = ys (1-(minX), 1, minZ, corner);
          ns=Util.toString(yMax.val,5,3); //to 5 decimal places, scientific not. after 2 zeros
          stringWidth = g.getFontMetrics().stringWidth(ns);
          stringHeight = g.getFontMetrics().getHeight();
          xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/2));
          yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
        }

        // 3.1 Calculate screenpoints of z-axis
        xa = corner [1-(minX)][minY][minZ][0];
        ya = corner [1-(minX)][minY][minZ][1];
        xb = corner [1-(minX)][minY][1-(minZ)][0];
        yb = corner [1-(minX)][minY][1-(minZ)][1];
        if (k > 0)  {
          double xtemp = xa;
          double ytemp = ya;
          xa = xb;
          ya = yb;
          xb = xtemp;
          yb = ytemp;
        }
        // 3.2 Calculate and draw marks
        for (double j=0; j <= 1; j+=(double).2) {
          x0 = xs (1-(minX), minY, j, corner);
          y0 = ys (1-(minX), minY, j, corner);
          mark (g, x0, y0, xa, ya, xb, yb, ln);
        }
        // 3.3 Calculate and draw numbers
        if (numbers && (xa-xb)*(xa-xb)+(ya-yb)*(ya-yb)>axisLabelLen) {
          // minHeight
          x0 = xs (1-(minX), minY, 0, corner);
          y0 = ys (1-(minX), minY, 0, corner);
          ns=Util.toString(minHeight,5,3); //to 5 decimal places, scientific not. after 2 zeros
          double stringWidth = g.getFontMetrics().stringWidth(ns);
          double stringHeight = g.getFontMetrics().getHeight();
          int xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/(double)2));
          int yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
          // maxHeight
          x0 = xs (1-(minX), minY, 1, corner);
          y0 = ys (1-(minX), minY, 1, corner);
          ns=Util.toString(maxHeight,5,3); //to 5 decimal places, scientific not. after 2 zeros
          stringWidth = g.getFontMetrics().stringWidth(ns);
          stringHeight = g.getFontMetrics().getHeight();
          xString = (int)((x0 - (yb-ya) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) - (stringWidth/2));
          yString = (int)((y0 + (xb-xa) * ( (2*ln) / Math.sqrt( (xb-xa)*(xb-xa) + (yb-ya)*(yb-ya) ) ) ) + stringHeight);
          g.drawString (ns, xString, yString);
        }



        // end of case 2 (draw visible part of outlines)
        break;
      }
    }

    // set back to normal PaintMode
    g.setPaintMode();

  }  // end of function drawCubeOutlines


  /** function mark
    * Draws one mark onto the screen */
  public void mark(Graphics g,
                   double x0, double y0,
                   double xa, double ya,
                   double xb, double yb,
                   double ln) {

    double len=(xb-xa)*(xb-xa) + (yb-ya)*(yb-ya);
    if (len!=0)
      len=ln/(double)Math.sqrt(len);
    g.drawLine((int)x0,(int)y0,(int)(x0-(yb-ya)*len),(int)(y0+(xb-xa)*len));

  }  // end of function mark


  /** function rescale
    * Rescales given cube in 'array' to fit in viewScreen rect */
  public void rescale (double [][][][] array)  {

    // biggest and lowest x bzw. y value on screen
    //    [0][0..1] = lowest x / highest x
    //    [1][0..1] = lowest y / highest y
    double maxSize [][] = { {100000, -100000},
                           {100000, -100000} };

    // search extrema
    for (int xs=0; xs<=1; xs++)  {
      for (int ys=0; ys<=1; ys++)  {
        for (int zs=0; zs<=1; zs++)  {
          for (int is=0; is<=1; is++)  {
            if (array [xs][ys][zs][is] < maxSize[is][0])  {
              maxSize[is][0] = array [xs][ys][zs][is];
            }
            if (array [xs][ys][zs][is] > maxSize[is][1])  {
              maxSize[is][1] = array [xs][ys][zs][is];
            }
          }
        }
      }
    }

    // calc factor for scaling
    double xFactor = (viewScreen[1][0] - viewScreen[0][0]) / (double)(maxSize[0][1] - maxSize[0][0]);
    double yFactor = (viewScreen[1][1] - viewScreen[0][1]) / (double)(maxSize[1][1] - maxSize[1][0]);
    double factor = xFactor < yFactor ? xFactor : yFactor;

    // recalc coordinates of corners in array
    for (int xn=0; xn<=1; xn++)  {
      for (int yn=0; yn<=1; yn++)  {
        for (int zn=0; zn<=1; zn++)  {
          array [xn][yn][zn][0] = (array[xn][yn][zn][0] - (maxSize[0][0]+maxSize[0][1])/2) * factor + (viewScreen[0][0]+viewScreen[1][0])/2;
          array [xn][yn][zn][1] = (array[xn][yn][zn][1] - (maxSize[1][0]+maxSize[1][1])/2) * factor + (viewScreen[0][1]+viewScreen[1][1])/2;
          array [xn][yn][zn][2] = array [xn][yn][zn][2] * factor;
        }
      }
    }

  }  // end of function rescale

  /** ensure the function approximator has its destroy() called too */
  public void destroy() {
    super.destroy();
    function.destroy();
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    origFunction[0].initialize(level);
    for (int i=0;i<plot[0].length;i++) {
      ((Plot)plot[0][i]).parentDisplay=this; //so they can call this.repaint() if necessary
      ((Plot)plot[0][i]).initialize(level);
    }
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      if (inputs.val!=null)
        inputs.val.transpose();
      function=(FunApp)origFunction[0].clone();
      setRotation(initXAngle.val,initYAngle.val,initZAngle.val);
      watchManager.registerWatch(trigger.val,triggerFreq,this);
      try {
        topColor   =new Color((float)topColorM.val.val(0),
                              (float)topColorM.val.val(1),
                              (float)topColorM.val.val(2));
        bottomColor=new Color((float)bottomColorM.val.val(0),
                              (float)bottomColorM.val.val(1),
                              (float)bottomColorM.val.val(2));
        floorColor =new Color((float)floorColorM.val.val(0),
                              (float)floorColorM.val.val(1),
                              (float)floorColorM.val.val(2));
      } catch (MatrixException e) {
        e.print();
      }
    }
  }//end initialize
}  // end of class Graph3D
