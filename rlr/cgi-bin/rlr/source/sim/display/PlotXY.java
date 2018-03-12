package sim.display;
import Display;
import WebSim;
import Random;
import watch.*;
import pointer.*;
import java.awt.*;
import java.util.Vector;
import parse.*;
import expression.*;
import fix.*;
import matrix.*;
import sim.data.*;
import sim.funApp.*;

/** This Plot records (x,y) pairs and plots then, with or without symbols,
  * either connected by lines or not, with one color for lines and another
  * for filling the symbols.
  *    <p>This code is (c) 1996,1997 Ansgar Laubsch and Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 2.0, 15 May 97
  * @author Ansgar Laubsch
  * @author Leemon Baird
  */
public class PlotXY extends Plot {
  //Version 2.0  15 May 97 completely reorganized to be a Plot - Leemon Baird
  //Version 1.04 22 Apr 97 added noMerge parameter - Leemon Baird
  //Version 1.03 15 April 97 Made corrections to the unparse routine - Mance Harmon
  //Version 1.02 16 Sep 96

  private Random rnd=new Random(); //a different random # generator from the one in the Experiment, so the Display won't interfere

  private PBoolean ringBuffer  =new PBoolean(false); //forget the oldest point when the buffer's full?
  private PBoolean noMerge     =new PBoolean(false); //forget every other point when the buffer's full?
  private PBoolean drawLines   =new PBoolean(true);  //should the dots be connected with lines?
  private PBoolean screenFixed =new PBoolean(false); //freeze region graphed on the screen?
  private IntExp symbolType    =new IntExp(0);       //0=none (a single-pixel dot), 1=circle,2=square,3=triangle,4=+,5=X,6=*
  private IntExp symbolSize    =new IntExp(6);       // diameter of the symbols (should be even)
  private IntExp maxNRects     =new IntExp(1000);    //max. number rects to be stored
  private IntExp frequency     =new IntExp(1);       //add a data point after every freqency updates

  private PString   xName      =new PString(null);   //name of the variable being watched for x coordinates
  private PString   yName      =new PString(null);   //name of the variable being watched for y coordinates
  private PString   triggerName=new PString(null);   //name of the variable that triggers redraws
  private Pointer   xVar       =null;  //the variable watched for x coordinate
  private Pointer   yVar       =null;  //the variable watched for y coordinate
  private Pointer   triggerVar =null;  //the variable that triggers redraws

  private int nRects         = 0; //number of used (=filled) rectangles. If ringBuffer, then position for next rectangle
  private int nRingRects     = 0; //if ringBuffer, number of data elements in buffer
  private int nPointsPerRect = 1; //number of points to be merged into one rectangle
  private int nPointsInLast  = 0; //number of points already stored in actual-rect(= nRects+1)
  private double sMinX,sMaxX,sMinY,sMaxY; //region being graphed, either autoscaled or from user
  private boolean reOrgFlag = true;   //set at each reorg of data arrays, set back at addRect
  private double[] minX,maxX,minY,maxY,firstX,lastX,firstY,lastY; //Data characterizing the rectangles
  private int lastRestartNumber=0; //last value of WebSim.restartNumber.  Restart whenever it changes

  private double[] zeros    ={0,0,0};
  private double[] minusOnes={-1,-1,-1};
  private PMatrixD colorM    =new PMatrixD(new MatrixD(zeros    ).transpose()); //color for lines/rects/symbol outlines.  null means black.
  private PMatrixD fillColorM=new PMatrixD(new MatrixD(minusOnes).transpose()); //color to fill symbols with. null means clear
  private Color    color    =Color.black;
  private Color    fillColor=null;
  private int[] xs=new int[4]; //temporary variable for drawing triangle symbols
  private int[] ys=new int[4]; //temporary variable for drawing triangle symbols

  private Data[]   snapshotData    =new Data[1];   //data object whose data is plotted by snapshot (=null if no snapshots)
  private FunApp[] snapshotFunApp1 =new FunApp[1]; //function that the output of data is passed through (=null if none)
  private FunApp   snapshotFunApp  =null;          //a copy of snapshotFunApp1
  private IntExp   snapshotXElement=new IntExp(0); //which element of dataObject is X (if data!=null)
  private IntExp   snapshotYElement=new IntExp(1); //which element of dataObject is Y (if data!=null)
  private IntExp   snapshotSamples =new IntExp(1); //# samples for each snapshot (if data!=null)
  private PMatrixD snapshotInput   =new PMatrixD(null); //input  vector for data object (if data!=null)
  private PMatrixD snapshotOutput  =new PMatrixD(null); //output vector for data object (if data!=null)
  private PMatrixD snapshotOutput2 =new PMatrixD(null); //output vector for the funApp that modifies the output of the data object (if snapshotData!=null && snapshotFunApp!=null)
  private PMatrixD snapshotWeights =new PMatrixD(null); //the weight matrix in the function approximation (if snapshotFunApp!=null)
  private PMatrixD snapshotOrigWeights =new PMatrixD(null); //weights in the original function approximator to copy into snapshotWeights (if snapshotFunApp!=null)

  private Object[][] parameters=
    {{"//Autoscaling 2D plot. Plots one variable vs. another"},
          {"freq",             frequency,        "how often to capture new data",
           "size",             maxNRects,        "# points in buffer",
           "symbolType",       symbolType,       "0=symbol on the data points, 1=circle, 2=square, 3=triangle, 4=+, 5=X, 6=*",
           "symbolSize",       symbolSize,       "in pixels",
           "ring",             ringBuffer,       "use ring buffer (forget points in order seen)",
           "drawLines",        drawLines,        "don't connect dots?",
           "noMerge",          noMerge,          "don't draw rectangles around forgotten points?",
           "lineColor",        colorM,           "RGB (each 0.0-1.0) for lines",
           "symbolColor",      fillColorM,       "RGB (each 0.0-1.0) for filling in symbols (default is clear)",
           "x",                xName,            "variable to use for x coord",
           "y",                yName,            "variable to use for y coord",
           "trigger",          triggerName,      "take snapshot when this variable changes",
           "snapshotData",     snapshotData,     "Data output goes through FunApp",
           "snapshotFunApp",   snapshotFunApp1,  "applied to output of Data",
           "snapshotXElement", snapshotXElement, "which FunApp output for X axis",
           "snapshotYElement", snapshotYElement, "which FunApp output for Y axis",
           "snapshotSamples",  snapshotSamples,  "how many samples in one snapshot",},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerWatch(triggerName.val, frequency,this); //get data when triggerName.val changes frequency times
  }//end setWatchManager

  /** One of the watched variables has been unregistered.
    */
  public void unregister(String watchedVar) {
    if (watchedVar.equals(triggerName.val))
      triggerVar=null;
    update(null,null,null); //redraw the new screen
  } //end unregister

  /** One of the watched variables has changed, so look at it and others.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (lastRestartNumber!=WebSim.restartNumber) {
      lastRestartNumber=WebSim.restartNumber;
      nRects         = 0; //number of used (=filled) rectangles. If ringBuffer, then position for next rectangle
      nRingRects     = 0; //if ringBuffer, number of data elements in buffer
      nPointsPerRect = 1; //number of points to be merged into one rectangle
      nPointsInLast  = 0; //number of points already stored in actual-rect(= nRects+1)
    }

    try {
      if (changedVar==colorM) {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
      }
      else if (changedVar==fillColorM)
        if (fillColorM.val.val(0)>=0)
          fillColor=new Color((float)fillColorM.val.val(0),
                              (float)fillColorM.val.val(1),
                              (float)fillColorM.val.val(2));
        else
          fillColor=null; //negative color means no fillColor, so it's clear
      } catch (MatrixException e) {
      }

    if (changedVar!=triggerVar)
      return;
    if (changedVar==triggerVar) {   //if the trigger variable changed, then add a point and plot
      if (xVar!=null && yVar!=null)
        addDataPoint(xVar.toDouble(),yVar.toDouble());
      if (snapshotData[0]!=null) //if taking snapshots, then do it here
        if (snapshotFunApp==null) //if just a Data object, no FunApp on its output
          for (int i=0;i<snapshotSamples.val;i++) {
            snapshotData[0].getData(snapshotInput.val.data,
                                 snapshotOutput.val.data,rnd);
            addDataPoint(snapshotOutput.val.data[snapshotXElement.val],
                         snapshotOutput.val.data[snapshotYElement.val]);
          }
        else //the Data object's output passes through the FunApp
          for (int i=0;i<snapshotSamples.val;i++) {
            snapshotData[0].getData(snapshotInput.val.data,
                                 snapshotOutput.val.data,rnd);
            try {
              snapshotWeights.val.replace(snapshotOrigWeights.val);
            } catch(MatrixException e) {
              e.print();
            }
            snapshotFunApp.evaluate();
            addDataPoint(snapshotOutput2.val.data[snapshotXElement.val],
                         snapshotOutput2.val.data[snapshotYElement.val]);
          }
    }
  } //end update

  /** function addDataPoint
    * add a new data point and redraw the whole graph */
  public void addDataPoint(double newX, double newY) {
    if (ringBuffer.val) { //ring buffer stores/draws only last maxNRects.val points
      nPointsInLast=1;
      addRect(newX,newY); //store new data point at current position
      nRingRects++;       //increment # data points, until its full
      if (nRingRects==maxNRects.val)
        nRingRects--;
      nRects++;           //increment current position
      if (nRects==maxNRects.val) {
        nRects=0;
      }
      return;
    }

    // test whether array is full and reOrg is needed
    if (nRects == maxNRects.val -1 & nPointsInLast == nPointsPerRect)
      reOrg();

    // step to next new position for storing next point
    if (nPointsInLast == nPointsPerRect)  {
      nRects++;
      nPointsInLast = 1;
    }
    else  {
      nPointsInLast++;
    }

    // add data (newX|newY)
    // ro rect. at:    'position': [nRects],     point: nPointsInLast
    addRect (newX, newY);
  }  // end of function addDataPoint


  /** function addRect
    * adds rect. (represented by one point) to end of (global) arrays */
  public void addRect (double newX, double newY)  {
    // check whether there is already data in akt. rect.
    // if not, set values to coord. of new point
    if (nPointsInLast == 1)  {
      minX[nRects] = newX;
      maxX[nRects] = newX;
      minY[nRects] = newY;
      maxY[nRects] = newY;
      firstX[nRects] = newX;
      lastX [nRects] = newX;
      firstY[nRects] = newY;
      lastY [nRects] = newY;
    }
    // if there is already data for akr. rect., merge new point into it
    else {
      minX[nRects] = minX[nRects] < newX ? minX[nRects] : newX;
      maxX[nRects] = maxX[nRects] > newX ? maxX[nRects] : newX;
      minY[nRects] = minY[nRects] < newY ? minY[nRects] : newY;
      maxY[nRects] = maxY[nRects] > newY ? maxY[nRects] : newY;
      firstX[nRects] = firstX[nRects];
      lastX[nRects] = newX;
      firstY[nRects] = firstY[nRects];
      lastY[nRects] = newY;
    }

    // check whether new rect. fits into screen dimensions (no redraw needed)
    // if x- and y-values fit into screensize...
    if (((minX[nRects]>sMinX)&&(maxX[nRects]<sMaxX))&&((minY[nRects]>sMinY)&&(maxY[nRects]<sMaxY)))  {
      // if no reOrg has been done before (reOrgFlag==flase)
      if (reOrgFlag!=false)  {
        reOrgFlag = false;
      }
    }
  }  // end of function addRect


  /** function reOrg
    * Reorganizes the arrays when nRects has reached maxNRects.val
    * by merging pairs of rects. into one rect.
    * => new array-size = nRects / 2 */
  public void reOrg ()  {
    // Variables:
    // number of array-element to store new rect. in
    int newRect;
    // array-element-numbers of rects to merge
    int rect1, rect2;

    //for each new rect do:
    for (newRect=0; newRect<(maxNRects.val/2); newRect++)  {
      // calc array-pos. of two rects to merge
      rect1 = newRect * 2;
      rect2 = rect1 + 1;
      // set data for new rect. (newRect)
      mergeRect (rect1, rect2, newRect);
    }

    // lower newRect by 1 because var. is reinitialized again after loop
    newRect -= 1;

    // reset/adjust global Variables
    // increase number of points to be sored in one rect. by multipl. with 2
    nPointsPerRect *= 2;
    // set number of points stored in last rect. to max points per rect.
    nPointsInLast = nPointsPerRect;
    // number of used rect. = newRect (because array starts at 0)
    nRects = newRect;
    // set Flag that signs that reOrg has been done
    reOrgFlag = true;
  }  // end of function reOrg


  /** function mergeRect
    * Merges the two given rects with array indices rect1 and rect2
    * into new array element newRect
    */
  public void mergeRect (int rect1, int rect2, int newRect)  {
    minX[newRect] = minX[rect1] < minX[rect2] ? minX[rect1] : minX[rect2];
    maxX[newRect] = maxX[rect1] > maxX[rect2] ? maxX[rect1] : maxX[rect2];
    minY[newRect] = minY[rect1] < minY[rect2] ? minY[rect1] : minY[rect2];
    maxY[newRect] = maxY[rect1] > maxY[rect2] ? maxY[rect1] : maxY[rect2];
    firstX[newRect] = firstX[rect1];
    lastX [newRect] = lastX [rect2];
    firstY[newRect] = firstY[rect1];
    lastY [newRect] = lastY [rect2];
  }  // end of function mergeRect

  /** draws 2D-Plot of data in global arrays (min/max, first/last X/Y) */
  public void drawAll(Graphics g, double xMin,   double xMax,
                                  double yMin,   double yMax,
                                  int    startX, int    startY,
                                  int    xAxisX, int    xAxisY,
                                  int    yAxisX, int    yAxisY) {
    int x1,x2,y1,y2; //holds endpoints of lines to plot, corners of rects and symbols
    if (g==null) return;
    double factor = (double)4;

    int len=10; // length of tic marks
    // x/y position of marks
    // [0][0/1] = x/y of mark starting point
    // [1][0/1] = x/y of end of mark
    int mark[][] = {{0,0},{0,0}};
    double number; // number to draw on scale
    int nX, nY; // position of number to draw onto scale

    // calc. data for screen-coord. system
    // 1 get min/max of X-data and Y-data in dataset
    // 1.1 set vars. to values of first point in dataset
    //find region to view on screen
    if (xMin<=xMax) { //if X bounds given, use them
      sMinX=xMin;
      sMaxX=xMax;
    } else { //if X bounds not given, then autoscale to fit the data
      sMinX = minX[0];
      sMaxX = maxX[0];
      for (int test=1; test<=(ringBuffer.val ? nRingRects : nRects); test++)  {
        if (minX[test] < sMinX) sMinX = minX[test];
        if (maxX[test] > sMaxX) sMaxX = maxX[test];
      }
      double scale,log10=(double)Math.log(10);
      scale=(double)Math.exp(log10*Math.floor(Math.log(
               sMaxX==sMinX ? 1 : sMaxX-sMinX)/log10))/2;
      sMinX=(double)Math.floor(sMinX/scale)*scale;
      sMaxX=(double)Math.ceil (sMaxX/scale)*scale;
    } //end find region to view on screen

    if (sMinX==sMaxX) //avoid divide by zero
      sMaxX+=1e-10;

    if (yMin<=yMax) { //if Y bounds given, use them
      sMinY=yMin;
      sMaxY=yMax;
    } else { //if Y bounds not given, then autoscale to fit the data
      sMinY = minY[0];
      sMaxY = maxY[0];
      for (int test=1; test<=(ringBuffer.val ? nRingRects : nRects); test++)  {
        if (minY[test] < sMinY) sMinY = minY[test];
        if (maxY[test] > sMaxY) sMaxY = maxY[test];
      }
      double scale,log10=(double)Math.log(10);
      scale=(double)Math.exp(log10*Math.floor(Math.log(
             sMaxY==sMinY ? 1 : sMaxY-sMinY)/log10))/2;
      sMinY=(double)Math.floor(sMinY/scale)*scale;
      sMaxY=(double)Math.ceil (sMaxY/scale)*scale;
    } //end find region to view on screen

    if (sMinY==sMaxY)  //avoid divide by zero: never let region have zero height
      sMaxY+=1e-10;

    g.setPaintMode();
    g.setColor (color);

    //draw all of the rectangles, etc. onto the screen
    for (int rect=0; rect<=(ringBuffer.val ? nRingRects : nRects); rect++) {
      if (noMerge.val) { //plot just the last point if not drawing merge rectangles
        x1=x2=(int)(startX + (((xAxisX-startX)*(lastX[rect]-sMinX))/(sMaxX-sMinX)));
        y1=y2=(int)(startY - (((startY-yAxisY)*(lastY[rect]-sMinY))/(sMaxY-sMinY)));
      } else { //plot the entire rectangle
        x1=(int)(startX + (((xAxisX-startX)*(minX[rect]-sMinX))/(sMaxX-sMinX)));
        x2=(int)(startX + (((xAxisX-startX)*(maxX[rect]-sMinX))/(sMaxX-sMinX)));
        y1=(int)(startY - (((startY-yAxisY)*(minY[rect]-sMinY))/(sMaxY-sMinY)));
        y2=(int)(startY - (((startY-yAxisY)*(maxY[rect]-sMinY))/(sMaxY-sMinY)));
      }

      int rectWidth=x2-x1;  //size of rectangle bounding a group of merged points
      int rectHeight=-(y2-y1);

      if (rectWidth==0 || rectHeight==0) //Java bug: can't draw 1-pixel-tall or 1-pixel-wide rects
        g.drawLine (x1,y1,x2,y2);
      else
        g.fillRect (x1, y1-rectHeight, rectWidth+1, rectHeight+1);

      if (noMerge.val && rect>0 && drawLines.val)  { //draw a line between last points if no rectangles being drawn
        x1=(int)(startX + (((xAxisX-startX)*(lastX [rect-1]-sMinX))/(sMaxX-sMinX)));
        y1=(int)(startY - (((startY-yAxisY)*(lastY [rect-1]-sMinY))/(sMaxY-sMinY)));
        x2=(int)(startX + (((xAxisX-startX)*(lastX [rect]  -sMinX))/(sMaxX-sMinX)));
        y2=(int)(startY - (((startY-yAxisY)*(lastY [rect]  -sMinY))/(sMaxY-sMinY)));
        g.drawLine (x1, y1, x2, y2); //draw a line between two adjacent rectangles
      }
      if (!noMerge.val && rect>0 && drawLines.val)  { //connect to previous rectangle (if there is one, and rectangles are being drawn)
        x1=(int)(startX + (((xAxisX-startX)*(lastX [rect-1]-sMinX))/(sMaxX-sMinX)));
        y1=(int)(startY - (((startY-yAxisY)*(lastY [rect-1]-sMinY))/(sMaxY-sMinY)));
        x2=(int)(startX + (((xAxisX-startX)*(firstX[rect]  -sMinX))/(sMaxX-sMinX)));
        y2=(int)(startY - (((startY-yAxisY)*(firstY[rect]  -sMinY))/(sMaxY-sMinY)));
        g.drawLine (x1, y1, x2, y2); //draw a line between two adjacent rectangles
      }
    }//end for rect=0 to nRects
    if (symbolType.val!=0) { //draw symbol around last point in rectangle
      for (int rect=0; rect<=(ringBuffer.val ? nRingRects : nRects); rect++) { //draw symbols
        x1=(int)(startX + (((xAxisX-startX)*(lastX[rect]-sMinX))/(sMaxX-sMinX)))-(symbolSize.val/2);
        y1=(int)(startY - (((startY-yAxisY)*(lastY[rect]-sMinY))/(sMaxY-sMinY)))-(symbolSize.val/2);
        int w=symbolSize.val;
        if (fillColor!=null) {
          g.setColor(fillColor);
          switch (symbolType.val) {
            case 1: g.fillOval (x1,y1,w,w);break; //circle
            case 2: g.fillRect (x1,y1,w,w);break; //square
            case 3: xs[0]=x1;      ys[0]=y1+w;    //triangle
                    xs[1]=x1+w/2;  ys[1]=y1;
                    xs[2]=x1+w;    ys[2]=y1+w;
                    xs[3]=xs[0];   ys[3]=ys[0];
                    g.fillPolygon(xs,ys,4);break;
            default: break;
          }
          g.setColor(color);
        }
        switch (symbolType.val) {
            case 1: g.drawOval    (x1,y1,w,w);               break; //circle
            case 2: g.drawRect    (x1,y1,w,w);               break; //square
            case 3: g.drawPolygon (xs,ys,4);                 break; //triangle
            case 4: g.drawLine (x1,    y1+w/2,x1+w,  y1+w/2);       //+
                    g.drawLine (x1+w/2,y1,    x1+w/2,y1+w);  break;
            case 5: g.drawLine (x1,y1,x1+w,y1+w);                   //X
                    g.drawLine (x1,y1+w,x1+w,y1);            break;
            case 6: g.drawLine (x1,    y1+w/2,x1+w,  y1+w/2);       //*
                    g.drawLine (x1+w/2,y1,    x1+w/2,y1+w);
                    g.drawLine (x1,y1,x1+w,y1+w);
                    g.drawLine (x1,y1+w,x1+w,y1);            break;
            default: break;
        }
      }//end for rect
    }//end if symbol
  } //end drawAll

  /** Put preferred autoscaling bounds into the variables pointed to by these
    * four pointers.  Change the variables only if the bounds should be
    * expanded to be larger than what the variables already say.
    */
  public void autoscaleBounds(PDouble xMin, PDouble xMax,
                              PDouble yMin, PDouble yMax) {
    for (int rect=0; rect<=(ringBuffer.val ? nRingRects : nRects); rect++) {
      if (xMin.val>minX[rect]) xMin.val=minX[rect];
      if (xMax.val<maxX[rect]) xMax.val=maxX[rect];
      if (yMin.val>minY[rect]) yMin.val=minY[rect];
      if (yMax.val<maxY[rect]) yMax.val=maxY[rect];
    }
  }//end autoscaleBounds

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      triggerVar=watchManager.findVar(triggerName.val);
      xVar=watchManager.findVar(xName.val);
      yVar=watchManager.findVar(yName.val);
      try {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
        if (fillColorM.val.val(0)>=0)
          fillColor=new Color((float)fillColorM.val.val(0),
                              (float)fillColorM.val.val(1),
                              (float)fillColorM.val.val(2));
        else
          fillColor=null; //negative color means no fillColor, so it's clear
      } catch (MatrixException e) {
        e.print();
      }
      firstX = new double [maxNRects.val];
      firstY = new double [maxNRects.val];
      lastX  = new double [maxNRects.val];
      lastY  = new double [maxNRects.val];
      minX   = new double [maxNRects.val];
      minY   = new double [maxNRects.val];
      maxX   = new double [maxNRects.val];
      maxY   = new double [maxNRects.val];

      if (snapshotData[0]!=null) {
        snapshotInput.val =new MatrixD(snapshotData[0].inSize ());
        snapshotOutput.val=new MatrixD(snapshotData[0].outSize());
        if (snapshotFunApp1[0]!=null) {
          snapshotFunApp         =(FunApp)snapshotFunApp1[0].clone();
          snapshotOrigWeights.val=snapshotFunApp1[0].getWeights();
          snapshotWeights        =(PMatrixD)snapshotOrigWeights.clone();
          snapshotOutput2.val    =(MatrixD)(snapshotFunApp1[0].getOutput().clone());
          try {
            snapshotFunApp.setIO(snapshotOutput.val,
                                 snapshotOutput2.val,
                                 snapshotWeights.val,
                                 null,null,null,null,null,null);
          } catch(MatrixException e) {
            e.print();
          }
        }
      }//end if snapshotData[0]!=null
    }//end if level==0
  }//end initialize
}  // end of class Graph2D
