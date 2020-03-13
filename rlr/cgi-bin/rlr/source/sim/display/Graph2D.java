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

/** This Display maintains a square window which contains a Plot or
  * multiple Plots.  The user can zoom in and reset with the mouse and
  * menu.  The refresh rate of redrawing can be different from the
  * rate at which the Plot samples and stores data.  The entire Display
  * can be forced to redraw by clicking on it.
  *    <p>This code is (c) 1996,1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01 2 June 97
  * @author Leemon Baird
  */
public class Graph2D extends Display {
  //v. 1.01 2 June 97 added showNumbers, squareView, propZoom
  //v. 1.0  15 May 97
  PBoolean propZoom   =new PBoolean(false); //constrain zoom regions to be proportional to window shape?
  PBoolean squareView =new PBoolean(true);  //ensure plot region of window is square rather than using all available space?
  PBoolean showNumbers=new PBoolean(true);  //put numbers and tick marks on the axes?

  /** will erase screen, then draw plot[0][0], then plot[0][1], etc. */
  Parsable plot[][]={new Plot[0]};

  /** zooming out multiplies the region size by this in each direction*/
  static final double zoomRatio=2;

  /** Min value of x to plot originally (menu item "top level" returns to this)*/
  protected NumExp origMinX=new NumExp(0);
  /** Max value of x to plot originally (menu item "top level" returns to this)*/
  protected NumExp origMaxX=new NumExp(-1);
  /** Min value of y to plot originally (menu item "top level" returns to this)*/
  protected NumExp origMinY=new NumExp(0);
  /** Max value of y to plot originally (menu item "top level" returns to this)*/
  protected NumExp origMaxY=new NumExp(-1);

  /** Variable whose changes trigger updates of the graph */
  protected PString trigger=new PString(null);
  /** How many times the trigger variable must change to cause an update */
  protected IntExp triggerFreq=new IntExp(1);
  protected Pointer  triggerVar; //the variable which causes the screen to redraw


  PDouble currMinX =new PDouble(0); //the region currently being zoomed in on
  PDouble currMaxX =new PDouble(0);
  PDouble currMinY =new PDouble(0);
  PDouble currMaxY =new PDouble(0);
  int    xAxisX, xAxisY;  //screen coord of point (maxX,  minY)
  int    yAxisX, yAxisY;  //screen coord of point (minX,  maxY)
  int    startX, startY;  //screen coord of point (startX,startY)
  double screenRectRatio; //ratio of height to width in pixels of plot area on the screen
  int    zoomStartX, zoomStartY;   //where the mouse clicked during a click and drag
  int    zoomCornerX, zoomCornerY; //where the mouse is now  during a click and drag
  int    zoomWidth, zoomHeight;    //size of area selected with mouse, in pixels
  boolean mouseDownNow=false;   //is the mouse currently dragging to select a zoom?
  boolean zoomed =false;        //currently zoomed in or out, thus disabling autoscaling?
  double[] ones  ={1,1,1};
  PMatrixD colorM=new PMatrixD(new MatrixD(ones).transpose());  //[red,green,blue] of background color
  Color    color =Color.white;                                  //background color

  private Object[][] parameters=
    {{"2D plot. Plot each of the plots on top of each other (first one on the bottom)."},
     {"trigger",    trigger,    "redraw when this variable changes",
      "freq",       triggerFreq,"redraw every freq times trigger changes",
      "xMin",       origMinX,   "min x value to plot (automatically scales if xMin>xMax)",
      "xMax",       origMaxX,   "max x",
      "yMin",       origMinY,   "min y",
      "yMax",       origMaxY,   "max y",
      "color",      colorM,     "[red,green,blue] for background",
      "flicker",    flicker,    "true=no double buffering, so it's faster, uses less memory, but flickers",
      "showNumbers",showNumbers,"true=show numbers on X and Y axes",
      "propZoom",   propZoom,   "true=the user can only zoom in on proportional regions (no distortions)",
      "squareView", squareView, "true=always show a square (not rectangular) region",
      "plots",      plot,       "list of PlotXY, last one plotted on top"},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** add menu items to the window containing this GWin canvas. */
  public void addMenus(MenuBar mb) {
    Menu menu=new Menu("Zoom");
    menu.add("Redraw");
    menu.add("In");
    menu.add("Out");
    menu.add("Reset");
    mb.add(menu);
  } //end addMenus

  /** respond to the menu choices */
  public boolean action(Event e,Object w) {
    if (((String)w).equals("Reset" )) {
      zoomed=false;
      zoom(origMinX.val,origMaxX.val,origMinY.val,origMaxY.val);
      update(null,null,null); //load in the data for the new region
    } else if (((String)w).equals("Redraw" )) {
      update(null,null,null); //load in the data for the new region
    } else if (((String)w).equals("Out" )) {
      zoomed=true;
      zoom(currMinX.val-(currMaxX.val-currMinX.val)*(zoomRatio-1)/2,
           currMaxX.val+(currMaxX.val-currMinX.val)*(zoomRatio-1)/2,
           currMinY.val-(currMaxY.val-currMinY.val)*(zoomRatio-1)/2,
           currMaxY.val+(currMaxY.val-currMinY.val)*(zoomRatio-1)/2);
      update(null,null,null); //load in the data for the new region
    } else if (((String)w).equals("In" )) {
      zoomed=true;
      zoom(currMinX.val+(currMaxX.val-currMinX.val)*(1-1./zoomRatio)/2,
           currMaxX.val-(currMaxX.val-currMinX.val)*(1-1./zoomRatio)/2,
           currMinY.val+(currMaxY.val-currMinY.val)*(1-1./zoomRatio)/2,
           currMaxY.val-(currMaxY.val-currMinY.val)*(1-1./zoomRatio)/2);
      update(null,null,null); //load in the data for the new region
    }
    return super.action(e,w);
  } //end action

  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"minX(now)",  currMinX,   this);
    wm.registerVar(name+"maxX(now)",  currMaxX,   this);
    wm.registerVar(name+"minY(now)",  currMinY,   this);
    wm.registerVar(name+"maxY(now)",  currMaxY,   this);
    for (int i=0;i<plot[0].length;i++)
      ((Plot)plot[0][i]).setWatchManager(wm,name+"plot"+i+"/");
  } //end setWatchManager

  /** The variable changed, so redraw */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (changedVar==squareView || changedVar==showNumbers) //if plot region might change size, then...
      buffBounds=null; //force the double buffering buffer to be recreated so it's clipped properly
    if (changedVar==colorM)
      try {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
      } catch(MatrixException e) {
        e.print();
      }
    super.update(changedName,changedVar,obj); //repaints screen if appropriate
    if (changedVar!=triggerVar) //force repaint when user changes a parameter
      repaint();
  } //end update

  /** function mouseDown
    * Event Handling for pressing mouseButton */
  public boolean mouseDown (Event e, int x, int y)  {
    mouseDownNow=true;
    zoomStartX = x;
    zoomStartY = y;
    zoomCornerX = zoomStartX;
    zoomCornerY = zoomStartY;
    zoomWidth = 0;
    zoomHeight = 0;
    return true;
  }  // end of MouseDown


  /** mouseDrag
    * Event Handling for dragging Mouse
    */
  public boolean mouseDrag (Event e, int x, int y)  {
    if (propZoom.val && y!=zoomStartY) {  //ensure a region is selected with same ratio as whole plot
      double r=(double)Math.abs(x-zoomStartX) /
                       Math.abs(y-zoomStartY);
      r*=screenRectRatio;
      if (r>1)
        x=(int)((x-zoomStartX)/r+zoomStartX);
      else
        y=(int)((y-zoomStartY)*r+zoomStartY);
    }

    // calc. width & height of new rect.
    zoomWidth  = Math.abs(x - zoomStartX);
    zoomHeight = Math.abs(y - zoomStartY);

    // calc. upper left corner of new rect.
    zoomCornerX = (x >= zoomStartX) ? zoomStartX : x;
    zoomCornerY = (y >= zoomStartY) ? zoomStartY : y;
    repaint();
    return true;
  }  // end of MouseDrag


  /** funtion mouseUp
    * Event Handling for releasing mouseButton
    */
  public boolean mouseUp (Event e, int x, int y)  {
    mouseDownNow=false;
    mouseDrag(null,x,y); //calculate zoomCornerX and zoomCornerY
    if (zoomWidth==0 && zoomHeight==0) {
        zoomed=false; //back to original region, so reenable autoscaling
        zoom(origMinX.val,origMaxX.val,origMinY.val,origMaxY.val); //click without drag resets to whole picture
        update(null,null,null); //load in the data for the new region
        return true;
    }
    zoomed=true; //zoomed in or out, so disable autoscaling

    if (zoomWidth==0) //don't zoom in on zero-sized region
      zoomWidth++;
    if (zoomHeight==0)
      zoomHeight++;

    // reset new min&max of displayed data according to zoom-rect.
    { double w=currMaxX.val-currMinX.val,h=currMaxY.val-currMinY.val;
      currMinX.val = currMinX.val + (zoomCornerX-startX) / (double)(xAxisX-startX) * w;
      currMaxX.val = currMinX.val + (zoomWidth)          / (double)(xAxisX-startX) * w;
      currMaxY.val = currMinY.val + (startY-zoomCornerY) / (double)(startY-yAxisY) * h;
      currMinY.val = currMaxY.val - (zoomHeight)         / (double)(startY-yAxisY) * h;
    }

    zoom(currMinX.val,currMaxX.val,currMinY.val,currMaxY.val);
    update(null,null,null); //load in the data for the new region
    return true;

  }  // end of MouseUp

  // zoom in on a region and redraw it
  private void zoom(double minX,double maxX,double minY,double maxY) {
    currMinX.val=minX;
    currMaxX.val=maxX;
    currMinY.val=minY;
    currMaxY.val=maxY;
    update(null,null,null); //redraw so user can see the click and drag
  } //end zoom

  /** ensure the function approximator has its destroy() called too */
  public void destroy() {
    super.destroy();
  }

  //draw axes, erase the region, call all Plot objects to draw in the region
  public void drawAll(Graphics g) {
    if (g==null)
      return;
    int    lMarks      =4;   //length of the tick marks
    int    decPlaces   =3;    //how many decimal places to show for axis labels
    int    maxZeros    =3;    //axis labels go to scientific notation if it would have this many zeros
    String bigNum      =fix.Util.toString(-80.e-288D/9,decPlaces,maxZeros);  //assumed biggest number ever in a label
    int    maxNumWidth =g.getFontMetrics().stringWidth(bigNum);             //width in pixels of bigNum
    int    maxNumHeight=g.getFontMetrics().getHeight();                     //height in pixels of bigNum
    int    leftIndent  =maxNumWidth+lMarks+3;  //left   margin of plot area in pixels
    int    bottomIndent=maxNumHeight+lMarks+6; //bottom margin of plot area in pixels
    int    topIndent   =maxNumHeight/2+3;      //top    margin of plot area in pixels
    int    rightIndent =maxNumWidth/2+3;       //right  margin of plot area in pixels

    if (!showNumbers.val)
      leftIndent=rightIndent=bottomIndent=topIndent=1;

    g.setPaintMode();
    g.setColor(Color.black);
    double number;              //number at end of tick mark
    int nX, nY;                 //where to draw tick mark
    int width =bounds().width;  //height of the entire region this Display can draw to
    int height=bounds().height; //width of the entire region this Display can draw to

    if (squareView.val) { //plot on largest square that fits in the window
      int w=Math.min(width-leftIndent-rightIndent, //plot region is w by w square
                     height-bottomIndent-topIndent);
      screenRectRatio =1;
      xAxisX          = w+leftIndent + (int)((width -w-leftIndent-rightIndent )/2.);
      xAxisY = startY = w+topIndent  + (int)((height-w-topIndent -bottomIndent)/2.);
      yAxisX = startX = leftIndent   + (int)((width -w-leftIndent-rightIndent )/2.);
      yAxisY          = topIndent    + (int)((height-w-topIndent -bottomIndent)/2.);
    } else { //plot in the largest rectangle that fits in the window
      int w=width-leftIndent-rightIndent;  //width of plot region
      int h=height-bottomIndent-topIndent; //height of plot region
      screenRectRatio= (w!=0 && h!=0) ? (double)h/w : 1;
      xAxisX          = w+leftIndent + (int)((width -w-leftIndent-rightIndent )/2.);
      xAxisY = startY = h+topIndent  + (int)((height-h-topIndent -bottomIndent)/2.);
      yAxisX = startX = leftIndent   + (int)((width -w-leftIndent-rightIndent )/2.);
      yAxisY          = topIndent    + (int)((height-h-topIndent -bottomIndent)/2.);
    }

    g.clearRect(0,0,width,height); //erase the entire window first

    g.drawLine(startX, startY, xAxisX, xAxisY); //draw boundary of plot region
    g.drawLine(startX, startY, yAxisX, yAxisY);
    g.drawLine(xAxisX, xAxisY, xAxisX, yAxisY);
    g.drawLine(yAxisX, yAxisY, xAxisX, yAxisY);

    String ns=null; //temporarily hold the number converted to a string

    if (!zoomed && (origMinX.val>origMaxX.val ||  //if autoscaling is on then
                    origMinY.val>origMaxY.val)) { //let plots autoscale
      currMinX.val=Double.POSITIVE_INFINITY;
      currMaxX.val=Double.NEGATIVE_INFINITY;
      currMinY.val=Double.POSITIVE_INFINITY;
      currMaxY.val=Double.NEGATIVE_INFINITY;
      for (int i=0;i<plot[0].length;i++)
        ((Plot)plot[0][i]).autoscaleBounds(currMinX,currMaxX,currMinY,currMaxY);
      if (currMinX.val==Double.POSITIVE_INFINITY) currMinX.val=0;
      if (currMaxX.val==Double.NEGATIVE_INFINITY) currMaxX.val=screenRectRatio<1 ? 1/screenRectRatio : 1;
      if (currMinY.val==Double.POSITIVE_INFINITY) currMinY.val=0;
      if (currMaxY.val==Double.NEGATIVE_INFINITY) currMaxY.val=screenRectRatio>1 ?   screenRectRatio : 1;
      if (origMinX.val<origMaxX.val) { //if only autoscaling Y axis
        currMinX.val=origMinX.val;
        currMaxX.val=origMaxX.val;
      }
      if (origMinY.val<origMaxY.val) { //if only autoscaling X axis
        currMinY.val=origMinY.val;
        currMaxY.val=origMaxY.val;
      }
    } //end if autoscaling

    if (showNumbers.val) //only draw tick marks when showing the numbers on the axes
      for (double markNum=0; markNum<=1; markNum+=(double).125) //draw 8 tick marks
        g.drawLine ((int)(startX+(xAxisX-startX)*markNum),
                    startY,
                    (int)(startX+(xAxisX-startX)*markNum),
                    startY+lMarks);

    double markNum=0; //what fraction of the way across the number or tick mark is drawn
    int c=1+(int)((xAxisX-startX)/(1.1*maxNumWidth)); //how many numbers to draw on x axis

    if      (c<3) c=2;
    else if (c<5) c=3;
    else          c=5;

    if (showNumbers.val)
      for (int i=0;i<c;i++) {
        markNum=((double)i)/(c-1);
        number = currMinX.val + (currMaxX.val-currMinX.val)*markNum;
        ns=fix.Util.toString(number,decPlaces,maxZeros);
        nX = (int)(startX+(xAxisX-startX)*markNum)-(g.getFontMetrics().stringWidth(ns)/2);
        nY = (int)(startY+lMarks+maxNumHeight+3);
        g.drawString (ns, nX, nY);
      }

    c=1+(int)((startY-yAxisY)/(1.1*maxNumHeight)); //how many numbers to draw on y axis

    if      (c<3) c=2;
    else if (c<5) c=3;
    else          c=5;

    if (showNumbers.val)
      for (markNum=0; markNum<=1; markNum+=(double).125) //draw 8 tick marks
        g.drawLine (startX,
                    (int)(startY-(startY-yAxisY)*markNum),
                    startX-lMarks,
                    (int)(startY-(startY-yAxisY)*markNum));
    if (showNumbers.val)
      for (int i=0;i<c;i++) {
        markNum=((double)i)/(c-1);
        number = currMinY.val + (currMaxY.val-currMinY.val)*markNum;
        ns=fix.Util.toString(number,decPlaces,maxZeros);
        nX = startX-leftIndent+(maxNumWidth-g.getFontMetrics().stringWidth(ns))/2;
        nY = (int)((startY-(startY-yAxisY)*markNum)+maxNumHeight/2);
        g.drawString (ns, nX, nY);
      }

    if (!flicker.val) {
      plotBuffer.setColor(color);
      plotBuffer.clipRect(startX+1,yAxisY+1,xAxisX-startX-1,startY-yAxisY-1);
      plotBuffer.fillRect(startX+1,yAxisY+1,xAxisX-startX-1,startY-yAxisY-1);
      for (int i=0;i<plot[0].length;i++) //draw each Plot on top of each other
        ((Plot)plot[0][i]).drawAll(plotBuffer,  currMinX.val,  currMaxX.val,
                                                currMinY.val,  currMaxY.val,
                                                startX,        startY,
                                                xAxisX,        xAxisY,
                                                yAxisX,        yAxisY);
    } else {
      g.setColor(color);
      g.fillRect(startX+1,yAxisY+1,xAxisX-startX-1,startY-yAxisY-1);
      g.clipRect(startX+1,yAxisY+1,xAxisX-startX-1,startY-yAxisY-1);
      for (int i=0;i<plot[0].length;i++) //draw each Plot on top of each other
        ((Plot)plot[0][i]).drawAll(g,  currMinX.val,  currMaxX.val,
                                       currMinY.val,  currMaxY.val,
                                       startX,        startY,
                                       xAxisX,        xAxisY,
                                       yAxisX,        yAxisY);
    }

    if (mouseDownNow) { //redraw the user's black selection rectangle so it won't flicker
      g.setColor(Color.black);
      g.setXORMode (Color.white);
      g.fillRect (zoomCornerX, zoomCornerY, zoomWidth, zoomHeight);
      g.setPaintMode();
    }
  }//end drawAll

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      PInt f=new PInt(1);
      triggerVar=watchManager.registerWatch(trigger.val, triggerFreq, this);
      watchManager.registerWatch(wmName+"minX(now)",  f,this);
      watchManager.registerWatch(wmName+"maxX(now)",  f,this);
      watchManager.registerWatch(wmName+"minY(now)",  f,this);
      watchManager.registerWatch(wmName+"maxY(now)",  f,this);
      try {
        color=new Color((float)colorM.val.val(0),
                        (float)colorM.val.val(1),
                        (float)colorM.val.val(2));
      } catch(MatrixException e) {
        e.print();
      }
      currMinX.val = origMinX.val;  //set view region to be entire region chosen by user
      currMaxX.val = origMaxX.val;
      currMinY.val = origMinY.val;
      currMaxY.val = origMaxY.val;
      zoom(currMinX.val,currMaxX.val,currMinY.val,currMaxY.val);  //show entire plot, not a small region blown up
    }
    for (int i=0;i<plot[0].length;i++) {
      ((Plot)plot[0][i]).parentDisplay=this; //so they can call this.repaint() if necessary
      ((Plot)plot[0][i]).initialize(level);
    }
  }//end initialize
}  //end of class Graph2D
