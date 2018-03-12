import watch.*;
import parse.*;
import expression.*;
import Display;
import external.poskanzer.*;    //GIF encoding
import external.friedmanHill.*; //postscript encoding
import java.io.*;
import java.awt.*;
import java.applet.*;

/** This creates a window with one component, a Panel (this object),
  * and resizes and shows it.  The close box works, and keypresses
  * on the title bar are passed through to the Panel.  The paint()
  * and handleEvent() methods should be overridden.  The overridden
  * paint() method should start with a call to super.paint().  To resize the
  * window, set preferredSize, then call layout().
  *
  * This code contains numerous hacks to overcome Java bugs.
  * The goal was to ensure that on multiple platforms (or
  * at least for both JDK1.0-Win95 and Netscape NN30b5a), the Panel
  * would automatically expand to fill at least most of
  * the window when the user resized the window, the setSize()
  * method would resize the window such that the best-fitting
  * Panel would be EXACTLY the requested size, and every paint
  * would match the Panel size exactly.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1 13 Feb 97
  * @author Leemon Baird
  */
public class GWin extends Panel {
  //version 1.1 13 Feb 97 added watch button
  //version 1.0 1 July 96

  //The embedPanel is embedded in either the Applet, or a separate window.
  //The following shows what is embedded in the embedPanel, and the
  //type of each object:
  //
  //     embedPanel        (Panel)
  //       buttonPanel     (Panel)
  //         buttons       (Panel)
  //           embedButton (Button)
  //           watchButton (Button)
  //       this            (Gwin)
  //
  //Since GWin extends Panel, the user's code can add Components
  //to the GWin or draw to it directly.

  /** The size to make this window when layout() is called */
  protected Dimension preferredSize=new Dimension(250,50);

  /** The original size of this Panel at the start */
  protected Dimension startSize=new Dimension(250,50);

  /** the actual window holding this Panel object */
  public ClosableWin window=null;

  /** the panel containing this Panel and the embed button, # button, and menus */
  public EmbedPanel embedPanel=null;

  /** the button toggling embedding on the Web page vs. separate window */
  public Button embedButton=new Button("e");

  /** the button that brings up a window of everything watched here */
  public Button watchButton=(this instanceof Watchable)? new Button("#") : null;

  /** the applet holding every embedded GWin */
  public Applet applet=null;

  /** the panel holding the button that toggles embedding */
  public Panel buttonPanel=new Panel();

  /** a panel holding the button panel */
  public Panel buttons=new Panel();

  /** the starting column of the window */
  public int startX=0;

  /** the starting row of the window */
  public int startY=0;

  /** the starting width of the window */
  public int startWidth=0;

  /** the starting Height of the window */
  public int startHeight=0;

  /** the menus that should attach to a window containing this GWin */
  public MenuBar menubar=null;

  /** Paint the region of size (bounds().width,bounds().height),
    * and corner at (0,0).  This method should be overridden,
    * but the new method should still call window.layout() first,
    * and should assume the region to draw starts at (0,0) and
    * is of size (width(),height()).
    */
  public void paint(Graphics g) {
    if (window!=null) {
      g.setColor(Color.black);
      g.drawOval(0,0,bounds().width,bounds().height);
    }
  }

  /** close the window and destroy all threads, Graphics contexts, etc. */
  public void closeWindow() {
    if (window!=null)
      window.closeWindow();
    else {
      embedPanel.hide();
      destroy();
      applet.remove(embedPanel);
    }
  }

  /** Return the font height */
  public int fHeight() {
    try {
      return getFontMetrics(getFont()).getHeight();
    } catch(NullPointerException e) {
      return 15; //if no font metrics available, return 15
    }
  }

  /** Override this to give a GWin menus.  Menus are only visible
    * when it's a separate window, not when embedded in the
    * Web page.  The menus should be added to mb.
    */
  public void addMenus(MenuBar mb) {
  }

  /** create a graphics window with upper-left corner at (x,y),
    * and the graphics area of size (width,height). If embed==true,
    * meaning the GWin should be embedded in the web page,
    * then it sets the preferred size of this Panel, but doesn't
    * actually create a new window.  To add menus to a GWin window,
    * override addMenus().
    */
  public void createWin(int x,int y,int width,int height,
                                     Applet app,boolean embed) {
    applet=app;
    preferredSize.width =startSize.width =width;
    preferredSize.height=startSize.height=height;
    startX     =x;
    startY     =y;
    startWidth =width;
    startHeight=height;
    if (embedPanel==null) { //if this GWin has never been initialized
      setBackground(Color.white);
      menubar=new MenuBar();
      Menu m=new Menu("Save");
      m.add("GIF");
      m.add("PS");
      m.add("PS bitmap");
      menubar.add(m);
      addMenus(menubar);

      embedButton.setBackground(Color.white);
      watchButton.setBackground(Color.white);
      buttons.setLayout(new BorderLayout(0,0));
      buttons.setBackground(Color.white);
      buttonPanel.setBackground(Color.white);
      buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
      buttonPanel.add(embedButton);
      if (watchButton!=null)
        buttonPanel.add(watchButton);
      buttonPanel.add(menuToChoice(menubar));

      buttons.add("West",buttonPanel);

      embedPanel=new EmbedPanel();
      embedPanel.setBackground(Color.white);
      embedPanel.gWin=this;
      embedPanel.setBackground(Color.white);
      embedPanel.setLayout(new BorderLayout(0,0));
      embedPanel.add("Center",this);
      embedPanel.add("North",buttons);
    }
    if (embed) { //new window should be embedded as first embedded window
      window=null;
      app.setBackground(Color.white);
      app.add(embedPanel,0);
    } else {//new window is a separate window
      preferredSize.height+=25; //this will be subtracted off again when window is embedded
      window=new ClosableWin();
      window.gWin=this;
      window.setBackground(Color.white);
      window.setLayout(new BorderLayout(0,0));
      window.add("Center",embedPanel);
      if (window.getPeer()==null) //make sure the peer exists
        window.addNotify();       //before trying to move the window
      //With the following, the size of a newly-created window may
      //be slightly different from the size of the embedded GWin.
      //But the sizes are the same with Win95 JDK 1.1 beta.
      window.reshape(window.bounds().x,window.bounds().y,
              preferredSize.width+8,
              preferredSize.height+27);/**/
      window.show();
      window.toFront();
      window.invalidate();
      window.validate();
    }
  }//end createWin

  /** convert a menubar into an equivalent Panel filled with
    * a Choice corresponding to each menu.
    */
  public Panel menuToChoice(MenuBar mb) {
    Panel  p=new Panel();
    p.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    Choice c=null;  //=new Choice();
    Menu m=null;
    for (int i=0;i<mb.countMenus();i++) { //for each menu
      Panel pc=new Panel();
      pc.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
      c=new Choice();
      m=mb.getMenu(i);
      c.addItem(m.getLabel());
      for (int j=0;j<m.countItems();j++) //for each item on this menu
        c.addItem(m.getItem(j).getLabel());
      pc.add(c);//Symantec bug: layout() can't handle 2 choices in the same panel
      p.add(""+i,pc);
    }
    return p;
  }


  /** This is called when the user closes the window. Override it
    * to react to that event (e.g. kill threads, dispose of Graphics, unregister watches)
    */
  public void destroy() {
  }

  /** return the size this window prefers */
  public Dimension preferredSize() {
    return preferredSize;
  }
  /** return the size this window prefers */
  public Dimension MinimumSize() {
    return preferredSize;
  }
  /** x coordinate of upper-left corner of whole window (including title) */
  public int x() {
    return window==null ? startX : bounds().x;
  }
  /** y coordinate of upper-left corner of whole window (including title) */
  public int y() {
    return window==null ? startY : bounds().x;
  }
  /** width of graphics area in the window (not including title bar) */
  public int width() {
    return window==null ? startWidth : bounds().width;
  }
  /** height of graphics area in the window (not including title bar) */
  public int height() {
    return window==null ? startHeight : bounds().height;
  }
}//end GWin

////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
////////////////////////////////////////////////////////

// A panel that holds an embed button, watch button, and a GWin Panel
class EmbedPanel extends Panel {
  /** the GWin that this panel contains */
  public GWin gWin=null;
  private FileOutputStream file=null; //the file to write to
  private Graphics g=null;            //offscreen graphics buffer holds image temporarily
  private Image    img=null;          //Image created from offscreen buffer g
  private File     fn=null;           //the file to save to
  private Object   choice=null;       //which menu item the user chose

  /** when embed button is clicked, toggle embedding in Web page vs. window*/
  public boolean handleEvent(Event e) {
    String prefix=((Watchable)gWin).getName(); //prefix of variable names to watch
    Rectangle b=null; //used to make sure size doesn't change when embedding/unembedding
    if (e.id==Event.ACTION_EVENT) //clicking a button is ACTION_EVENT
      if (e.target==gWin.watchButton) { //clicked watch button, so create a window
        DisplayList disp=null;
        int[]       x={0}, y={0}, width={300}, height={300};
        Boolean[]   falseVal={new Boolean(false)};
        Display[]   displays={new Title((prefix==null || prefix.equals("")
                                           ? "All watchable variables"
                                           : "Variables for "+prefix),
                                        new ShowEdit("time",new IntExp(-1), /**/
                                                       true,prefix,(String [])null))};
        disp=new DisplayList(x,y,width,height,falseVal,
                             displays,gWin.applet);
        disp.setWatchManager(((Watchable)gWin).getWatchManager(),
                             ((Watchable)gWin).getName()+"#/");
        disp.initialize(0); //initialize the new ShowEdit object just created
      } else if (e.target==gWin.embedButton) //if embed button clicked
        if (gWin.window==null) {  //there is no window, so make one
          disable();
          hide();
          gWin.createWin(gWin.startX,gWin.startY,gWin.preferredSize.width,
                         gWin.preferredSize.height,gWin.applet,false);
          gWin.applet.remove(this);//remove this panel from the gWin.applet
          show();
          enable();
          gWin.applet.layout();
          gWin.applet.invalidate();
          gWin.applet.validate();
          return true; //don't let event propagate
        } else { //there is a window, so close it and embed self
          gWin.preferredSize=size(); //make it the same size it is now
          gWin.preferredSize.height-=25; //this was added on when the window was created
          b=gWin.applet.bounds();
          if (gWin.preferredSize.width  > b.width)
              gWin.preferredSize.width  = b.width;
          if (gWin.preferredSize.height > b.height)
              gWin.preferredSize.height = b.height;
          disable();
          hide();
          gWin.window.dispose();
          gWin.window=null;
          gWin.applet.add(this,0);  //each new item is embedded first
          show();
          enable();
          gWin.applet.layout();
          gWin.applet.invalidate();
          gWin.applet.validate();
          return true; //don't let event propagate
        }
    return super.handleEvent(e);//let event propagate
  }//end handleEvent


  /** pass menu selections on to the GWin panel */
  public boolean action(Event e,Object w) {
    choice=w;
    boolean write=true; //should write file to disk?
    if (e.target instanceof Choice)
      ((Choice)e.target).select(0);
    if (   !((String)w).equals("GIF")
        && !((String)w).equals("PS")
        && !((String)w).equals("PS bitmap"))
      return gWin.action(e,w); //action is not save .GIF or .PS
    ((WebSim)gWin.applet).suspendAllThreads();
    FileDialog fd=new FileDialog(new Frame(),"Enter filename for saving image");
    fd.show();
    ImageDialog a;
    fn=null;
    if (fd.getDirectory()!=null && fd.getFile()!=null)
      fn=new File(fd.getDirectory(),fd.getFile());
    if (fn==null)            //user hit CANCEL when choosing filename
      ((WebSim)gWin.applet).resumeAllThreads();
    else if (fn.exists())    //user entered a filename for a file that already exists
      a=new ImageDialog(this,"WARNING: "+fd.getFile()+" exists.  Overwrite it?");
    else                     //user entered a filename for a file that doesn't already exist
        saveFile();
    return true;
  }//end action

  /** save the image as a file */
  public void saveFile() {
      try {
        Rectangle b=gWin.bounds();
        file=new FileOutputStream(fn);
        if (((String)choice).equals("GIF")) { //write out the GIF
          img=gWin.createImage(b.width,b.height);
          g=img.getGraphics();
          g.translate(-b.x,-b.y);    //corner of gWin is corner of g
          fix.Util.paintAll(g,gWin,false,true); //force gWin to draw self into this Image
          GifEncoder ge=new GifEncoder(img,file);
          ge.setInterlace(true);     //GIF89a interlaced format
          ge.encode();               //fill in file contents
        } else if (((String)choice).equals("PS bitmap")) { //write out as a big postscript bitmap
          img=gWin.createImage(b.width,b.height);
          g=img.getGraphics();
          PSGr postscript = new PSGr(file, g);
          g.translate(-b.x,-b.y);    //corner of gWin is corner of g
          fix.Util.paintAll(g,gWin,false,true); //force gWin to draw self into this Image
          postscript.drawImage(img,0,0,null);
          postscript.close();
        } else if (((String)choice).equals("PS")) { //write out as postscript list of drawing primitives
          PSGr postscript = new PSGr(file, gWin.getGraphics());
          fix.Util.paintAll(postscript,gWin,true,true);
          postscript.close();
        }
      } catch (Throwable ee) { //disk full, etc.
        System.out.println("Error while saving all to disk");
        System.out.println(ee);
        ee.printStackTrace();
      } finally {
        if (g!=null)
          g.dispose(); //dispose of graphics context properly
        try {
          file.close();
        } catch (Throwable eee) {
           System.out.println(""+eee);
           eee.printStackTrace();
        }
      }
    ((WebSim)gWin.applet).resumeAllThreads();
  }//end saveFile

}//end class embedPanel

////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
////////////////////////////////////////////////////////

// window with working close button,
class ClosableWin extends Frame {
  /** the GWin that this window holds */
  public GWin gWin=null;

  /** toggle embedding, close window, or pass event to first component */
  public boolean handleEvent(Event e) {
    if (e.id==Event.WINDOW_DESTROY) {
      closeWindow();
      return true; //don't propagate further
    }
    else
      return super.handleEvent(e); //just let the event propagate
  }

  /** close this window, and destroy all associated threads, Graphics contexts, etc. */
  public void closeWindow() {
    if (gWin!=null)
      gWin.hide();
    hide();
    if (gWin!=null)
      gWin.destroy();
    dispose();
  }
}//end ClosableWin



////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
////////////////////////////////////////////////////////


// Tell user file exists, and save file if user chooses "OK" rather than "CANCEL".
class ImageDialog extends Dialog {
  private Button okButton=new Button("OK");
  private Button cancelButton=new Button("CANCEL");
  private Panel buttons=new Panel();
  private Frame unusedFrame=new Frame(); //parent of file dialog
  private EmbedPanel creator=null; //object that created this dialog

  /** create an alert with the given title and message to the user */
  public ImageDialog(EmbedPanel c,String msg) {
    super(null,msg,true);
    creator=c;
    setLayout(new BorderLayout());
    buttons.setLayout(new FlowLayout());
    buttons.add(okButton);
    buttons.add(cancelButton);
    add("South",buttons);
    add("Center",new Label(msg,Label.CENTER));
    resize(300,200);
    show();
  }

  /** listen for OK or CANCEL being clicked */
  public boolean handleEvent(Event e) {
    if (e.id==Event.ACTION_EVENT) { //clicking a button is ACTION_EVENT
      if (e.target==okButton) {
        hide();
        dispose();
        creator.saveFile();
      } else if (e.target==cancelButton) {
        hide();
        dispose();
        ((WebSim)creator.gWin.applet).resumeAllThreads();
      }
      return true;
    }
    return false; //let action other than OK and CANCEL propagate
  }//end handleEvent
}//end class ImageDialog
