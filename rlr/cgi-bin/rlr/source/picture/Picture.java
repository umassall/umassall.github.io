package picture;
import java.awt.*;
import parse.*;
import Project;
import expression.*;

/** Draw an image and allow user to zoom in on portions.
  * When the picture is
  * a gallery of many little pictures, it allows one to be zoomed in on
  * with a single mouse click.  Thereafter, a click and drag or number key zooms.
  * Many of the numerical constants have a suffix f because the compiler,
  * in direct violation of the spec, assumes they are double by default.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 11/23/95
  * @author Leemon Baird
  */
public class Picture extends Project {
  protected FadeIn fadeIn=new FadeIn();   //current picture shows either a single picture or many
  protected static final char CR='\n';//a carriage return
  protected static final char LF='\r';//a line feed
  protected int mdownx,mdowny;  //where did the mouse go down
  protected int mdragx,mdragy;  //where was the mouse last during this drag
  protected int gWidth,gHeight; //width/height of graphics region being drawn
  protected IntExp bHeight=new IntExp(3);  //height of the status bar at the top
  protected double minX;         //region of the picutre now showing on screen
  protected double maxX;
  protected double minY;
  protected double maxY;
  protected Gallery  currGallery =null; //the Gallery if there is one (null if there are none)
  protected Component component  =null; //where to draw
  protected PicPipePipeline pass1=null; //filter to the both pipeline during first pass that draws each pixel
  protected PicPipePipeline pass2=null; //filter to the both pipeline during 2nd pass that antialiases some pixels
  protected PicPipePipeline both =null; // the main picture that is antialiased/processed by the pass1/pass2 lists
  protected boolean bar; //is the par parameter given?

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{'"+
           "['bar' IntExp] "+
           "'pass1' picture.picPipePipeline "+
           "'pass2' picture.picPipePipeline "+
           "'bothPasses' picture.picPipePipelin "+
           "'}'"+
           "//Picture to fade in. "+
           "bothPasses is a picture, which is modified by pass1 or "+
           "by pass2.  The first pass is used to draw every pixel, "+
           "then for those pixels different from their neighbors, the "+
           "second pass is asked to find their color (e.g. through "+
           "antialiasing).  The number after 'bar' is the height of "+
           "the status bar in pixels (default 10).";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit('{');
    u.indent();
      u.emitLine();
      if (bar) {
      u.emit("bar ");
      u.emitUnparse(bHeight,lang);
      u.emitLine();
      }
      u.emitLine("pass1");
      u.indent();
        u.emitUnparse(pass1,lang);
      u.unindent();
      u.emitLine();
      u.emitLine("pass2");
      u.indent();
        u.emitUnparse(pass2,lang);
      u.unindent();
      u.emitLine();
      u.emitLine("bothPasses");
      u.indent();
        u.emitUnparse(both,lang);
      u.unindent();
    u.unindent();
    u.emit('}');
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    bar=p.parseID("bar",false);
    if (bar)
      bHeight=(IntExp)p.parseClass("IntExp",lang,true);
    p.parseID("pass1",true);
    pass1=(PicPipePipeline)p.parseClass("PicPipePipeline",lang,true);
    p.parseID("pass2",true);
    pass2=(PicPipePipeline)p.parseClass("PicPipePipeline",lang,true);
    p.parseID("bothPasses",true);
    both=(PicPipePipeline)p.parseClass("PicPipePipeline",lang,true);
    pass1.sourceLast.rest=both.source; //linked lists form a Y shape
    pass2.sourceLast.rest=both.source;
    p.parseChar('}',true);
    return this;
  } //end method parse

  /** Draw picture after done parsing.  This is in a separate thread. */
  public void run() {
    component=this;
    show(); //show this frame
    setGSize();
    currGallery=pass1.source.getGallery(); //get first Gallery, if any
    resetRegion();
    startDrawing();
    fadeIn.drawServer();
  } //end method init

  /** ensure that events are handled sequentially */
  public boolean handleEvent(Event e) {
    return super.handleEvent(e);
  }

  /** when the user resizes or moves the canvas, stop and restart the thread */
  public void reshape(int x,int y,int w, int h) {
    super.reshape(x,y,w,h);
    resetRegion();
    startDrawing();
  }


  /** paint the screen from the buffer without stopping the drawing thread. */
  public void paint(Graphics g) {
    if (fadeIn!=null && component!=null && g!=null) {
//      requestFocus();
      fadeIn.setGraphics(getGraphics());//reaquire Graphics if it was lost
      fadeIn.paint(g,component);
      printText();
    }
  }

  /** Remember where mouse clicks so highlighting will be right. */
  public boolean mouseDown(Event event,int x, int y) {
    setGSize();
    if (x<0)           x=0;
    if (x>gWidth)      x=gWidth;
    if (y<bHeight.val) y=bHeight.val;
    if (y>gHeight)     y=gHeight;
    mdownx=mdragx=x;
    mdowny=mdragy=y;
    mouseDrag(event,x,y); //highlight the current area
    return true;
  }

  /** As mouse is dragged, highlight appropriate region. */
  public boolean mouseDrag(Event event,int x, int y) {
    int w;
    double gx,gy; //mdrag in image coordinates from (-1,-1) to (1,1)
    double nx,ny; //mdown in image coordinates from (-1,-1) to (1,1)
    Graphics g=getGraphics();
    try {
      setGSize();
      if (x<0)           x=0;
      if (x>gWidth)      x=gWidth;
      if (y<bHeight.val) y=bHeight.val;
      if (y>gHeight)     y=gHeight;
      mdragx=x;
      mdragy=y;
      if (currGallery==null) { //a single picture visible, not a collection
        w=Math.abs(mdragx-mdownx) < Math.abs(mdragy-mdowny)?
          Math.abs(mdragx-mdownx) : Math.abs(mdragy-mdowny);
        if (mdragx<mdownx)
          mdragx=mdownx-w;
        else
          mdragx=mdownx+w;
        if (mdragy<mdowny)
          mdragy=mdowny-w;
        else
          mdragy=mdowny+w;

        fadeIn.paint(g,component);
        g.setXORMode(Color.white);
        if (w>4)
          g.fillRect(mdownx<mdragx?mdownx:mdragx,
                     mdowny<mdragy?mdowny:mdragy,w,w);
        g.setPaintMode();
      } else { //not a single picture but a collection
        gx=(double)mdragx/gWidth                     *(maxX-minX)+minX; //mdrag in image coordinates from (-1,-1) to (1,1)
        gy=(double)(mdragy-bHeight.val)/(gHeight-bHeight.val)*(minY-maxY)+maxY;
        nx=(double)mdownx/gWidth                     *(maxX-minX)+minX; //mdown in image coordinates from (-1,-1) to (1,1)
        ny=(double)(mdowny-bHeight.val)/(gHeight-bHeight.val)*(minY-maxY)+maxY;
        if ((currGallery.picNum(nx,ny)!=currGallery.picNum(gx,gy)) || //if cursor leaves curr pic
            currGallery.whichPic(nx,ny)==null)                       //or cursor in white region
          fadeIn.paint(g,component);                             //then no highlight
        else { //else highlight current picture
          fadeIn.paint(g,component);
          g.setXORMode(Color.white);
          g.clipRect (0,0,gWidth,gHeight);
          g.fillRect(
            (int)((currGallery.minX(gx,gy)-minX)/(maxX-minX)*gWidth),
            (int)((currGallery.minY(gx,gy)+maxY)/(maxY-minY)*(gHeight-bHeight.val))+bHeight.val,
            (int)((currGallery.maxX(gx,gy)-currGallery.minX(gx,gy))/(maxX-minX)*gWidth),
            (int)((currGallery.maxY(gx,gy)-currGallery.minY(gx,gy))/(maxY-minY)*(gHeight-bHeight.val)));
          g.setPaintMode();
        } //end if mdown pic = mdrag pic
      } //end else
      return true;
    } finally {
      if (g!=null)
        g.dispose();
    }
  } //end method mouseDrag

  /** When mouse is released, zoom in on appropriate region. */
  public boolean mouseUp(Event event,int x, int y) {
    int w;
    double t;
    double gx,gy; //mdrag in image coordinates from (-1,-1) to (1,1)
    double nx,ny; //mdown in image coordinates from (-1,-1) to (1,1)
    PicPipe newPict; //new picture to draw after zooming
    Graphics graphics=getGraphics(); //used only to paint for click without drag
    try {
      setGSize();
      if (currGallery==null) { //if only a single picture is on the screen
        if (mdragx<mdownx) {  //swap to get mdownx <= mdragx
          w=mdragx;
          mdragx=mdownx;
          mdownx=w;
        }
        if (mdragy<mdowny) {  //swap to get mdowny <= mdragy
          w=mdragy;
          mdragy=mdowny;
          mdowny=w;
        }
        w=mdragx-mdownx<mdragy-mdowny ?  //w is width in pixels of square selected
          mdragx-mdownx:mdragy-mdowny;
        if (w>4) {
          t=maxX-minX;
          minX+=t*mdownx/gWidth;
          maxX=minX+t*w/gWidth;
          t=maxY-minY;
          minY+=t*(gHeight-mdragy)/(gHeight-bHeight.val);
          maxY=minY+t*w/(gHeight-bHeight.val);
          startDrawing();
        } else //a mere mouse click without drag doesn't do anything
          fadeIn.paint(graphics,component);
      } else { //not a single picture, but a collection of pictures
        gx= (double)mdragx/gWidth                     *(maxX-minX)+minX; //mdrag in image coordinates from (-1,-1) to (1,1)
        gy= (double)(mdragy-bHeight.val)/(gHeight-bHeight.val)*(minY-maxY)+maxY;
        nx= (double)mdownx/gWidth                     *(maxX-minX)+minX; //mdown in image coordinates from (-1,-1) to (1,1)
        ny= (double)(mdowny-bHeight.val)/(gHeight-bHeight.val)*(minY-maxY)+maxY;
        if ((currGallery.picNum(nx,ny)==currGallery.picNum(gx,gy)) &
            currGallery.whichPic(nx,ny)!=null) {//if cursor down and up in same picture ...
          currGallery.select(gx,gy); //zoom into picture containing this point
          currGallery=pass1.source.getGallery();//perhaps a gallery within a gallery?
          resetRegion(); //set screen to show initial region for this picture
          resetRegion();
          startDrawing();
        }
      } //end else
      return true;
    } finally {
      if (graphics!=null)
        graphics.dispose();
    }
  } //end method mouseUp

  /* change the region graphed.  If current region were (0,0)-(1,1),
   * then new region would be (minx,miny)-(maxx,maxy);
   */
  void zoom (double minx,double maxx,double miny,double maxy) {
    double w=maxX-minX;
    double h=maxY-minY;
    minX+=w*minx;
    maxX=minX+w*(maxx-minx);
    minY+=h*miny;
    maxY=minY+h*(maxy-miny);
    startDrawing();
  }

  /* Make the drawing thread start drawing the graphics to the screen. */
  void startDrawing() {
    if (fadeIn==null)
      return;
    Graphics graphics=getGraphics();
    //start() will displose of graphics so it shouldn't be done here
    setGSize();
    fadeIn.stop(); //kill the old drawing thread before starting a new one
    fadeIn.start(graphics,component,        //start a drawing thread
                   minX,   //picture should show this region of space, where [-1,1] is everything
                   maxX-(maxX-minX)/gWidth,
                   minY,
                   maxY-(maxY-minY)/gHeight,
                   0,bHeight.val,              //upper-left corner of picture region
                   gWidth,gHeight-bHeight.val, //width/height of picture region
                   bHeight.val,                //height of status bar above picutre region
                   pass1.source,pass2.source); //source for each pass
    printText();
  }//end startDrawing

  /* Print the text on the screen describing the current picture */
  void printText() {
    int row=gHeight+bHeight.val+14;
    Graphics g=getGraphics();
    try {
      String description=pass1.source.first.getDescription(pass1.source.rest);
      if (g==null)
        return;
      setGSize();
      g.setPaintMode();
      g.clearRect(4,row,10000,10000);
      g.setColor(Color.black);
      g.drawString("Direct Fractal Viewer 1.0 (c) 1996 Leemon Baird",4,row); row+=12;
      g.drawString("Freeware: see the source code for details",4,row);   row+=12;
      g.drawString("Region drawn: min x="+minX,4,row);   row+=12;
      g.drawString("Region drawn: max x="+maxX,4,row);   row+=12;
      g.drawString("Region drawn: min y="+minY,4,row);   row+=12;
      g.drawString("Region drawn: max y="+maxY,4,row);   row+=12;
      row+=12;
      if (description!=null) {
        g.drawString(description,4,row);
        row+=12;
      }
      g.clipRect(0,0,gWidth,gHeight+bHeight.val);
      g.dispose();
    } finally {
      if (g!=null)
        g.dispose();
    }
  }//end printText

  /* Reset the screen to view the region that the current picture initially shows.*/
  void resetRegion() {
    minX=pass1.source.first.regionMinX(pass1.source.rest);
    maxX=pass1.source.first.regionMaxX(pass1.source.rest);
    minY=pass1.source.first.regionMinY(pass1.source.rest);
    maxY=pass1.source.first.regionMaxY(pass1.source.rest);
  }

  /** If a key is pressed, respond appropriately. */
  public boolean keyDown(Event event, int key) {
    switch (key) {
      case '0': zoom(-0.50f, 1.50f,-0.50f, 1.50f); break; //zoom out
      case '1': zoom(-0.25f, 0.75f,-0.25f, 0.75f); break; //pan down/left
      case '2': zoom( 0.00f, 1.00f,-0.25f, 0.75f); break; //pan down
      case '3': zoom( 0.25f, 1.25f,-0.25f, 0.75f); break; //pan down/right
      case '4': zoom(-0.25f, 0.75f, 0.00f, 1.00f); break; //pan left
      case '5': zoom( 0.25f, 0.75f, 0.25f, 0.75f); break; //zoom in
      case '6': zoom( 0.25f, 1.25f, 0.00f, 1.00f); break; //pan right
      case '7': zoom(-0.25f, 0.75f, 0.25f, 1.25f); break; //pan up/left
      case '8': zoom( 0.00f, 1.00f, 0.25f, 1.25f); break; //pan up
      case '9': zoom( 0.25f, 1.25f, 0.25f, 1.25f); break; //pan up/right
      case ' ': resetRegion();  //return to current picture's original coordinates
                startDrawing();
                break;
      case LF :
      case CR : pass1.source.resetGalleries();
                resetRegion();   //return to original picture with original coordinates
                currGallery=pass1.source.getGallery();
                resetRegion();
                startDrawing();
                break;
      case 's': if (fadeIn!=null) //stop drawing.
                  fadeIn.stop();
    }
    return true;
  }//end keyDown

  /** set gWidth and gHeight according to current window size */
  void setGSize() {
    gWidth  =size().width;
    gHeight =size().height;
    if (gWidth < gHeight-bHeight.val) //draw within the largest square that fits
      gHeight=gWidth+bHeight.val;
    else
      gWidth = gHeight-bHeight.val;
  }
} // end class Picture
