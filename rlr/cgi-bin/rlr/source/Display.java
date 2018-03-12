import parse.*;
import java.awt.*;
import watch.*;
import GWin;
import pointer.*;

/** A display object can display simulation variables in a separate window.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 24 July 96
  * @author Leemon Baird
  */
public abstract class Display extends GWin implements Parsable, Watcher, Watchable {
  /** Should all the displays be disabled (still record data, but don't display it)? */
  public static boolean disableDisplays=false;

  /** The WatchManager that this Watcher watches */
  public WatchManager watchManager;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  public String wmName=null;
  /** Should drawing to the screen not be double buffered (use offscreen buffer for no flicker)? */
  public PBoolean flicker=new PBoolean(false);
  /** offscreen buffer used for double buffering */
  public Graphics  buffer         =null;
  /** smaller region within offscreen buffer used for double buffering */
  public Graphics  plotBuffer     =null;
  /** image corresponding to buffer */
  public Image     image          =null;
  /** bounds of buffer */
  public Dimension buffBounds     =null;
  /** is a repaint currently queued (and so there's no need to call repaint() again)? */
  public PBoolean repaintInProgress=new PBoolean(false);


  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
  }

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Get the WatchManager being used */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "";
  }

  /** Output a description of this object that can be parsed with parse().*/
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  }

  /** One of the watched variables has been unregistered.
    * The watcher doesn't have to do anything, but might
    * remove this variable from some internal data structure.
    */
  public void unregister(String watchedVar) {};

  /** One of the watched variables has changed, so look at it and others.
    * It should call checkMoved() to
    * make sure the window is a legal size.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (!disableDisplays)
        repaint();
  }

  /** Redraw the display without flicker.  Normally update() first
    * erases the screen, then calls paint(), but it's overridden
    * here so it won't erase.  Don't override update() or paint().  Override
    * drawAll() instead.  When it is time to redraw the display, call repaint();
    */
  public void update(Graphics g) {
    paint(g);
  }

  /** Redraw the display. Do not override update(Graphics) or paint(Graphics).
    * Override drawAll() instead. When it is time to redraw the
    * display, call repaint().
    */
  public void paint(Graphics g) {
    if (disableDisplays) { //don't draw the display until all the variables are set up right
      synchronized (repaintInProgress) {
        repaintInProgress.val=false;
      }
      return;
    }
    if (flicker==null || flicker.val) {//if no double buffering
      drawAll(g); //draw directly to screen
    }else { //do double buffering
      Dimension bounds=size();
      if (buffBounds==null || //if no current buffer or buffer wrong size, recreate it
          bounds.width !=buffBounds.width ||
          bounds.height!=buffBounds.height) {
        if (buffer    !=null) buffer.dispose();
        if (plotBuffer!=null) plotBuffer.dispose();
        if (image     !=null) image.flush();
        image=createImage(bounds.width <1 ? 1 : bounds.width,
                          bounds.height<1 ? 1 : bounds.height);
        buffer=image.getGraphics();
        plotBuffer=image.getGraphics();
        buffBounds=bounds;
        repaint();//Java Bug: the first paint() after a resize has the new bounds but still has the
                  //old clipRect, so we need to enqueue another repaint() here, which
                  //will have the correct clipRect when it's finally called.
      }
      drawAll(buffer);            //draw to offscreen buffer
      g.drawImage(image,0,0,this); //copy buffer to screen
    }
    synchronized (repaintInProgress) {
      repaintInProgress.val=false;
    }
  }//end update

  /** Repaint the screen if a repaint isn't already queued.  Do not override repaint(). */
  public void repaint() {
    boolean doit=false;
    synchronized (repaintInProgress) {
      if (!repaintInProgress.val) {
        repaintInProgress.val=true;
        doit=true;
      }
    if (doit)
      super.repaint();
    }
  }

  /** Override this to draw directly to the display.
    * It should clear the entire window and redraw everything.
    * It is called by paint() which is called when a window is created, uncovered,
    * or saved to the disk as a GIF or Postscript file.
    */
  public void drawAll(Graphics g) {}

  /** This is called when the user closes the window. Override it
    * to react to that event (e.g. kill threads, dispose of Graphics)
    */
  public void destroy() {
    if (buffer     !=null) buffer.dispose();
    if (plotBuffer !=null) plotBuffer.dispose();
    if (image      !=null) image.flush();
    watchManager.unregisterWatcher  (this); //unregister all my watches
    watchManager.unregisterWatchable(this); //unregister all my variables
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} //end class Display
