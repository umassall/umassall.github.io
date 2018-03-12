package picture;
import java.awt.*;
import java.awt.image.*;

/** Make a picture fade in by drawing big squares, then smaller ones.
  * A separate thread does the drawing, and a copy of the screen is kept in
  * memory so that the screen can be redrawn quickly.  Also, an
  * additional copy is kept as a byte array, so that the 24-bit
  * image can be examined for antialiasing, or written to a file easily.
  * Two PicPipeSources are given to this program.  The first is used to
  * color every pixel, then the second, if not null, is used to color
  * only those pixels that differ greatly from at least one of the 8 neighbors
  * in at least one of the 3 color components.  This allows adaptive
  * antialiasing if the first pass does just jitter, and the second
  * averages multiple rays per pixel.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 25 July 96
  * @author Leemon Baird
  */
public class FadeIn {
  protected Graphics onscrG=null; //draw to this to draw on the screen
  protected Image offscrImg=null; //copy used for paint quickly (double buffering)
  protected Graphics offscrG=null;//copy used for paint quickly (double buffering)
  protected byte[][][] offscrC;   //[whichcolor][x][y], 0=red,1=green,2=blue
  protected int x0;        //upper left corner of region to draw picture in (status bar is above that)
  protected int y0;
  protected int width=0;   //width of graphics area to draw in
  protected int height=0;  //height of graphics area to draw in, not including status bar
  protected int bHeight;   //height of the status bar at the top
  protected double minX;
  protected double maxX;
  protected double minY;
  protected double maxY;
  protected PicPipeList source1=null; //source for first pass
  protected PicPipeList source2=null; //source for second pass
  protected Colors color=new Colors(); //scratchpad holding color of a single pixel
  protected StopDrawingException stopDrawingException=new StopDrawingException(); //thrown to interrupt job

  //to request a new drawing job, clients set the following variables
  //and set newJob to true.  All this happens within a critical
  //region protected by the lock associated with this. If the drawing
  //loop finishes a drawing job, it will wait() which will suspend
  //it until a new job comes in and does a notify();
  protected boolean newJob=false;
  protected boolean stopJob=false; //if this is set, do wait()
  protected Graphics newG=null;
  protected Component newC=null;
  protected double newMinX,newMaxX,newMinY,newMaxY;
  protected int newX,newY,newGWidth,newGHeight,newBarHeight;
  protected PicPipeList newSource1,newSource2;

  /** Interrupt the current drawing job, and force a wait for start() */
  public synchronized void stop () {
    stopJob=true;
  }

  /** Start a new thread that causes the picture to fade in on the screen.
    * Each pixel is drawn first, then a second pass does antialiasing.
    * Stops the previously-running thread, if any.
    */
  public synchronized void start(Graphics g,Component c,
                                 double minX,double maxX,
                                 double minY,double maxY,
                                 int x,int y,int gWidth,
                                 int gHeight,int barHeight,
                                 PicPipeList sourcePass1,
                                 PicPipeList sourcePass2) {
    if (c==null || gHeight<1 || gWidth<1 || barHeight<0)
      return;
    if (newJob)       //requesting a new job before the last was started,
      newG.dispose(); //so get rid of the previous Graphics
    newJob   =true;
    stopJob  =false;
    newG        =g;
    newC        =c;
    newMinX     =minX;
    newMaxX     =maxX;
    newMinY     =minY;
    newMaxY     =maxY;
    newX        =x;
    newY        =y;
    newGWidth   =gWidth;
    newGHeight  =gHeight;
    newBarHeight=barHeight;
    newSource1  =sourcePass1;
    newSource2  =sourcePass2;
    notify(); //wake up server if sleeping
  } //end method start

  /** Instantly paint the screen from an offscreen buffer
    */
  public synchronized void paint(Graphics g,ImageObserver c) {
    if (offscrImg!=null && g!=null && c!=null)
      g.drawImage(offscrImg,x0,y0-bHeight,c);
  } //end method paint

  /** set the drawing to be going to g instead of its current destination */
  public void setGraphics(Graphics g) {
    if (1==1) {  //the code that should work doesn't
      g.dispose();
      return;
    }

    if (onscrG!=g) {
      if (onscrG!=null)
        onscrG.dispose();
      onscrG=g;
    }
  }

  /** Draw the whole picture with either dots or squares.
    * If dots==true, then it draws dots and a white status bar.
    * otherwise it draws squares and a black status bar.
    * @exception StopDrawingException if the drawing is interrupted
    */
  public synchronized void redraw (PicPipeList source,boolean dots) throws StopDrawingException {
    int w;                   //width of main square region
    int size;                //size of next square to draw
    int x,iy,a,b;            //used to calculate scattering
    long ii;                 //used to calculate scattering
    Color c;                 //scratchpad holding a color temporarily
    int minsize;             //min of width and height
    int pdone,k,y;           //percent done
    minsize=(width<height) ? width : height;
    for (w=1; w<<1 <= minsize; w<<=1);
    size=w;
    a=b=1;
    for (long i=0;i<w*w;i++) {//draw whole graphics region
      if (i%(w*w/width)==0) { //draw status bar at top
        pdone=(int)(width*i/(w*w));
        onscrG.setColor  (dots?Color.black:Color.white);
        offscrG.setColor (dots?Color.black:Color.white);
        onscrG.fillRect  (x0+pdone,y0-bHeight,width-pdone,bHeight);
        offscrG.fillRect (   pdone,         0,width-pdone,bHeight);
        onscrG.setColor  (dots?Color.white:Color.black);
        offscrG.setColor (dots?Color.white:Color.black);
        onscrG.fillRect  (x0,y0-bHeight,pdone,bHeight);
        offscrG.fillRect ( 0,         0,pdone,bHeight);
      }
      if (a-- <=0) { //adjust size of squares being drawn
          size>>=1;
          a=b*3-1;
          b<<=2;
      }
      ii=i;
      x=iy=0;
      for (k=w>>1;k>0;k>>=1) { //adjust offset of grid
        x =( x<<1)+(int)(ii&1);
        iy=(iy<<1)+(int)(ii&2);
        ii>>=2;
      }
      iy=(iy>>1)^x;

      synchronized (this) { //check for interruption of this drawing job
        if (newJob || stopJob)
          throw stopDrawingException;
      }

      try {
        wait(1);//be polite to nonpeemptive threads (e.g. some Unixs)
      } catch (InterruptedException e) {
      }

      for (;x<width;x+=w)  //draw a grid of squares
        for (y=iy;y<height;y+=w)
          if (!dots || shouldAntialias(x,y)) {
            color.reset(); //default color is black, no filter, no transparency, value=0
            source.first.get(source.rest,color,
                             (maxX-minX)*x/width+minX+1e-7f,
                             (maxY-minY)*(height-y)/height
                                          +minY+9e-8f,0,0,
                             (maxX-minX)/width,
                             (maxY-minY)/height,0,0);
            offscrC[0][x][y]=color.red;
            offscrC[1][x][y]=color.green;
            offscrC[2][x][y]=color.blue;
            //The following line is the only "new" in this entire program
            //that is called multiple times per second during drawing.
            //This probably causes awful garbage-collection slowdowns
            //of the entire program.  This whole problem could be
            //avoided if the java.awt.Color object had a setRGBint(int rgb)
            //method, or if java.awt.Graphics.setColor() could accept
            //RGB packed integers instead of only Color objects.
            //Who's idea was it anyway to force the use of an object which
            //can never be changed, and can only be thrown away and
            //recreated?
            c=new Color(color.red+128,color.green+128,color.blue+128);
            onscrG.setColor(c);
            offscrG.setColor(c);
            if (size>1 && !dots) { //Java bug: size 1 rectangles are invisible
              onscrG.fillRect (x+x0,y+y0,     size,size);
              offscrG.fillRect(x   ,y+bHeight,size,size);
            } else {
              onscrG.drawLine  (x+x0,y+y0,     x+x0,y+y0     );  //but lines where start=end are visible
              offscrG.drawLine (x   ,y+bHeight,x   ,y+bHeight);  //but lines where start=end are visible
            }
          }
    } //end for i
  } //end method redraw

  /** Should pixel (x,y) be antialiased because its color is currently
    * very different from at least one of its neighbors?
    */
  protected final boolean shouldAntialias(int x, int y) {
    return (x<=0  || x>=width-1 || y<=0 || y>=height-1 //antialias border
            || isFar(x,y,x-1,y-1) || isFar(x,y,x,y-1) || isFar(x,y,x+1,y-1)
            || isFar(x,y,x-1,y  ) ||                     isFar(x,y,x+1,y  )
            || isFar(x,y,x-1,y+1) || isFar(x,y,x,y-1) || isFar(x,y,x+1,y+1));
  } //end method shouldAntialias

  /** Is pixel (x1,y1) a very different color than pixel (x2,y2)? */
  protected final boolean isFar(int x1,int y1,int x2,int y2) {
    return (30<Math.abs((((int)offscrC[0][x1][y1]))-(((int)offscrC[0][x2][y2]))))
        || (30<Math.abs((((int)offscrC[1][x1][y1]))-(((int)offscrC[1][x2][y2]))))
        || (30<Math.abs((((int)offscrC[2][x1][y1]))-(((int)offscrC[2][x2][y2]))));
  } //end method isFar

  /** do an infinite loop that executes drawing jobs */
  public synchronized void drawServer() {
    for(;;) {//each time through this loop handles one job
      synchronized(this) { //get the parameters of that drawing job
        while (!newJob) //wait for a new drawing job
          try {
            wait();
          } catch(java.lang.InterruptedException e) {
          }
        newJob   =false;
        stopJob  =false;
        onscrG   =newG;        newG=null;
        minX     =newMinX;
        maxX     =newMaxX;
        minY     =newMinY;
        maxY     =newMaxY;
        x0       =newX;
        y0       =newY;
        bHeight  =newBarHeight;
        source1  =newSource1;  newSource1=null;
        source2  =newSource2;  newSource2=null;
        if (width!=newGWidth || height!=newGHeight) {//create new buffer
          width    =newGWidth;
          height   =newGHeight;
          offscrImg=(newC.createImage(width,bHeight+height));
          if (offscrG!=null)
            offscrG.dispose();
          offscrG  =offscrImg.getGraphics();
          offscrC  = new byte[3][][];
          for (int i=0;i<3;i++) {
            offscrC[i]=new byte[width][];
            for (int j=0;j<width;j++)
              offscrC[i][j]=new byte[height];
          }
        }
        onscrG.clipRect(x0,y0-bHeight,width,height+bHeight); //clip rect includes picture + bar
        offscrG.clipRect(0,0,width,height+bHeight);
      }
      try { //do 2 passes of drawing unless interrupted by a stop or new job
        if (source1!=null)
          redraw(source1,false);   //draw whole screen with shrinking squares

        //draw the completed black status bar since now done with first pass
        onscrG.setColor  (Color.black);
        offscrG.setColor (Color.black);
        onscrG.fillRect  (x0,y0-bHeight,width,bHeight);
        offscrG.fillRect ( 0,         0,width,bHeight);

        if (source2!=null) //only do second pass if there is a source for it
          redraw(source2,true);    //draw whole screen, using only dots

        //draw the completed white status bar since now done with second pass
        onscrG.setColor  (Color.white);
        offscrG.setColor (Color.white);
        onscrG.fillRect  (x0,y0-bHeight,width,bHeight);
        offscrG.fillRect ( 0,         0,width,bHeight);
      } catch (StopDrawingException e) {
      } finally {
        onscrG.dispose(); //when drawing is done, dispose of Graphics
      }//end try (do one drawing job)
    }//end for (infinite loop)
  }//end drawServer
} //end class FadeIn

//This is raised to interrupt a drawing job in the middle
class StopDrawingException extends Exception {};
